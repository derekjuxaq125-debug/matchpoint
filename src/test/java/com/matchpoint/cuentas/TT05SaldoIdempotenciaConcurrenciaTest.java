package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.ResultadoPronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioMemoria;
import com.matchpoint.cuentas.servicio.CobroConCreditos;
import com.matchpoint.cuentas.servicio.LibroMovimientos;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import com.matchpoint.cuentas.servicio.SaldoInsuficienteException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * TT-05 (Johan): pruebas de saldo, idempotencia y concurrencia.
 * Meta del proyecto: cero descuentos duplicados y cero saldos negativos.
 * Cada prueba lanza muchos hilos que arrancan al mismo tiempo (CountDownLatch).
 */
class TT05SaldoIdempotenciaConcurrenciaTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);

    /** Ejecuta {@code n} tareas a la vez y devuelve cuántas terminaron sin excepción. */
    static int enParalelo(int n, Callable<?> tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(n, 32));
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                try {
                    tarea.call();
                    exitos.incrementAndGet();
                } catch (SaldoInsuficienteException esperado) {
                    // rechazo correcto: no alcanza el saldo
                }
                return null;
            }));
        }
        salida.countDown();
        for (Future<?> f : futuros) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        return exitos.get();
    }

    static void verificarLibro(LibroMovimientos libro, String usuario) {
        assertEquals(libro.saldoVigente(usuario), libro.sumaMovimientos(usuario), "saldo = suma de movimientos");
        assertTrue(libro.movimientosDe(usuario).stream().allMatch(m -> m.saldoResultante() >= 0), "sin saldos negativos");
        long claves = libro.movimientosDe(usuario).stream().map(Movimiento::claveIdempotencia).distinct().count();
        assertEquals(libro.movimientosDe(usuario).size(), claves, "sin movimientos duplicados");
    }

    @Test
    void cienDebitosSimultaneosSobreCincuentaCreditos() throws Exception {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        libro.registrar("ANA00001", TipoMovimiento.ASIGNACION_INICIAL, 50, "U", "INICIAL", null);
        AtomicInteger n = new AtomicInteger();
        int exitos = enParalelo(100, () -> libro.registrar("ANA00001", TipoMovimiento.PARTICIPACION, -1,
                "P", "COBRO-" + n.incrementAndGet(), null));
        assertEquals(50, exitos, "solo alcanzan 50 débitos de 1");
        assertEquals(0, libro.saldoVigente("ANA00001"));
        verificarLibro(libro, "ANA00001");
    }

    @Test
    void laMismaSolicitudEnviadaCincuentaVecesALaVezGeneraUnSoloMovimiento() throws Exception {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        libro.registrar("ANA00001", TipoMovimiento.ASIGNACION_INICIAL, 100, "U", "INICIAL", null);
        enParalelo(50, () -> libro.registrar("ANA00001", TipoMovimiento.PARTICIPACION, -10, "P", "COBRO-UNICO", null));
        assertEquals(2, libro.movimientosDe("ANA00001").size());
        assertEquals(90, libro.saldoVigente("ANA00001"));
        verificarLibro(libro, "ANA00001");
    }

    @Test
    void reintentosSimultaneosDeUnPronosticoNoCobranDosVeces() throws Exception {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        EventoRepositorioMemoria eventos = new EventoRepositorioMemoria();
        eventos.guardar(CobroConCreditosTest.evento("E1", 10));
        PronosticoRepositorioMemoria pronosticos = new PronosticoRepositorioMemoria();
        PronosticoServicio servicio = new PronosticoServicio(eventos, pronosticos, new CobroConCreditos(libro), RELOJ);
        Usuario ana = new Usuario("ANA00001", "Ana", Rol.PARTICIPANTE, "s", "h");
        libro.registrar("ANA00001", TipoMovimiento.ASIGNACION_INICIAL, 100, "U", "INICIAL", null);

        AtomicInteger repetidas = new AtomicInteger();
        enParalelo(20, () -> {
            ResultadoPronostico r = servicio.registrar(ana, "E1", "Local", "doble-clic-1");
            if (r.repetida()) {
                repetidas.incrementAndGet();
            }
            return r;
        });
        assertEquals(1, pronosticos.listar().size(), "un solo pronóstico");
        assertEquals(19, repetidas.get(), "las otras 19 solicitudes se reconocen como repetidas");
        assertEquals(90, libro.saldoVigente("ANA00001"), "un solo descuento");
        verificarLibro(libro, "ANA00001");
    }

    @Test
    void pronosticosSimultaneosEnVariosEventosNoSuperanElSaldo() throws Exception {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        EventoRepositorioMemoria eventos = new EventoRepositorioMemoria();
        for (int i = 0; i < 20; i++) {
            eventos.guardar(CobroConCreditosTest.evento("E" + i, 10));
        }
        PronosticoRepositorioMemoria pronosticos = new PronosticoRepositorioMemoria();
        PronosticoServicio servicio = new PronosticoServicio(eventos, pronosticos, new CobroConCreditos(libro), RELOJ);
        Usuario ana = new Usuario("ANA00001", "Ana", Rol.PARTICIPANTE, "s", "h");
        libro.registrar("ANA00001", TipoMovimiento.ASIGNACION_INICIAL, 100, "U", "INICIAL", null);

        AtomicInteger evento = new AtomicInteger();
        int exitos = enParalelo(20, () -> servicio.registrar(ana, "E" + evento.getAndIncrement(), "Local", null));
        assertEquals(10, exitos, "100 créditos alcanzan para 10 pronósticos de 10");
        assertEquals(10, pronosticos.listar().size());
        assertEquals(0, libro.saldoVigente("ANA00001"));
        verificarLibro(libro, "ANA00001");
    }

    @Test
    void variosUsuariosALaVezMantienenSusSaldosIndependientes() throws Exception {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        String[] usuarios = {"USR00001", "USR00002", "USR00003", "USR00004", "USR00005"};
        for (String u : usuarios) {
            libro.registrar(u, TipoMovimiento.ASIGNACION_INICIAL, 30, "U", "INICIAL-" + u, null);
        }
        AtomicInteger n = new AtomicInteger();
        int exitos = enParalelo(200, () -> {
            int i = n.incrementAndGet();
            return libro.registrar(usuarios[i % 5], TipoMovimiento.PARTICIPACION, -3, "P", "C-" + i, null);
        });
        assertEquals(50, exitos, "cada usuario alcanza 10 débitos de 3");
        for (String u : usuarios) {
            assertEquals(0, libro.saldoVigente(u));
            verificarLibro(libro, u);
        }
    }

    @Test
    void concurrenciaConPersistenciaEnArchivo() throws Exception {
        var archivo = Files.createTempDirectory("tt05").resolve("movimientos.txt");
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioArchivo(archivo), RELOJ);
        libro.registrar("ANA00001", TipoMovimiento.ASIGNACION_INICIAL, 20, "U", "INICIAL", null);
        AtomicInteger n = new AtomicInteger();
        int exitos = enParalelo(40, () -> libro.registrar("ANA00001", TipoMovimiento.PARTICIPACION, -1,
                "P", "C-" + n.incrementAndGet(), null));
        assertEquals(20, exitos);
        LibroMovimientos releido = new LibroMovimientos(new MovimientoRepositorioArchivo(archivo), RELOJ);
        assertEquals(0, releido.saldoVigente("ANA00001"), "el archivo quedó consistente");
        verificarLibro(releido, "ANA00001");
    }
}
