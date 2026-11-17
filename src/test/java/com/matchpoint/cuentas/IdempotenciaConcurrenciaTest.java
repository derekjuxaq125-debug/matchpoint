package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.ResultadoPronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioMemoria;
import com.matchpoint.cuentas.servicio.CobroConCreditos;
import com.matchpoint.cuentas.servicio.LibroMovimientos;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-14 (solicitudes repetidas) y HU-15 (operaciones simultáneas). */
class IdempotenciaConcurrenciaTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    static final Usuario ANA = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");

    @Test
    void hu14LaMismaClaveDevuelveElMovimientoOriginal() {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        Movimiento a = libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 100, "U", "INICIAL-1", null);
        Movimiento b = libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 100, "U", "INICIAL-1", null);
        assertSame(a, b);
        assertEquals(1, libro.todos().size());
        assertEquals(100, libro.saldoVigente("12345678"));
    }

    @Test
    void hu14ReintentoDeUnPronosticoNoCobraDosVeces() {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        EventoRepositorioMemoria eventos = new EventoRepositorioMemoria();
        eventos.guardar(CobroConCreditosTest.evento("E1", 10));
        PronosticoRepositorioMemoria pronosticos = new PronosticoRepositorioMemoria();
        PronosticoServicio servicio = new PronosticoServicio(eventos, pronosticos, new CobroConCreditos(libro), RELOJ);
        libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 50, "U", "INICIAL-12345678", null);

        ResultadoPronostico primero = servicio.registrar(ANA, "E1", "Local", "solicitud-1");
        ResultadoPronostico reintento = servicio.registrar(ANA, "E1", "Local", "solicitud-1");

        assertFalse(primero.repetida());
        assertTrue(reintento.repetida());
        assertEquals(primero.pronostico().id(), reintento.pronostico().id());
        assertEquals(1, pronosticos.listar().size());
        assertEquals(40, libro.saldoVigente("12345678"));
    }

    @Test
    void hu15DosDebitosSimultaneosNoDejanSaldoNegativo() throws Exception {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 10, "U", "INICIAL", null);
        CountDownLatch salida = new CountDownLatch(1);
        int[] exitos = new int[1];
        Runnable debito = () -> {
            try {
                salida.await();
                libro.registrar("12345678", TipoMovimiento.PARTICIPACION, -10, "P",
                        "COBRO-" + Thread.currentThread().getId(), null);
                synchronized (exitos) {
                    exitos[0]++;
                }
            } catch (Exception esperado) {
                // el segundo débito se rechaza por saldo insuficiente
            }
        };
        Thread t1 = new Thread(debito);
        Thread t2 = new Thread(debito);
        t1.start();
        t2.start();
        salida.countDown();
        t1.join();
        t2.join();
        assertEquals(1, exitos[0]);
        assertEquals(0, libro.saldoVigente("12345678"));
        assertEquals(0, libro.sumaMovimientos("12345678"));
    }
}
