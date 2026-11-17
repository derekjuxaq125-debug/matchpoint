package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.LibroMovimientos;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-09: registrar movimientos trazables que no se pueden eliminar. */
class LibroMovimientosTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);

    private MovimientoRepositorioMemoria repo;
    private LibroMovimientos libro;

    @BeforeEach
    void preparar() {
        repo = new MovimientoRepositorioMemoria();
        libro = new LibroMovimientos(repo, RELOJ);
    }

    @Test
    void guardaTodosLosDatosDeTrazabilidad() {
        Movimiento m = libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 100,
                "USUARIO:12345678", "INICIAL-12345678", null);
        assertEquals("12345678", m.usuarioId());
        assertEquals(TipoMovimiento.ASIGNACION_INICIAL, m.tipo());
        assertEquals(100, m.cantidad());
        assertEquals(100, m.saldoResultante());
        assertEquals("USUARIO:12345678", m.origen());
        assertEquals(LocalDateTime.of(2026, 11, 16, 10, 0), m.registrado());
        assertTrue(!m.operacion().isBlank());
    }

    @Test
    void elSaldoEsLaSumaDeLosMovimientos() {
        libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 100, "USUARIO:1", "c1", null);
        libro.registrar("12345678", TipoMovimiento.PARTICIPACION, -10, "PRONOSTICO:p1", "c2", null);
        libro.registrar("87654321", TipoMovimiento.ASIGNACION_INICIAL, 50, "USUARIO:2", "c3", null);
        assertEquals(90, libro.saldoVigente("12345678"));
        assertEquals(90, libro.sumaMovimientos("12345678"));
        assertEquals(50, libro.saldoVigente("87654321"));
    }

    @Test
    void rechazaCantidadCeroYDatosFaltantes() {
        assertThrows(ValidacionException.class,
                () -> libro.registrar("12345678", TipoMovimiento.AJUSTE, 0, "X", "c", "j"));
        assertThrows(ValidacionException.class,
                () -> libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 5, "", "c", null));
        assertTrue(repo.listar().isEmpty());
    }

    @Test
    void ajustesYReversionesExigenJustificacion() {
        assertThrows(ValidacionException.class,
                () -> libro.registrar("12345678", TipoMovimiento.AJUSTE, 5, "AUDITORIA:1", "c", " "));
    }

    @Test
    void elRepositorioNoOfreceModificarNiBorrar() {
        boolean puedeBorrar = Arrays.stream(com.matchpoint.cuentas.repositorio.MovimientoRepositorio.class.getMethods())
                .map(Method::getName)
                .anyMatch(n -> n.startsWith("eliminar") || n.startsWith("borrar") || n.startsWith("actualizar"));
        assertTrue(!puedeBorrar, "El libro no debe permitir modificar ni borrar movimientos");
    }

    @Test
    void siFallaElGuardadoElSaldoNoCambia() {
        LibroMovimientos conFallo = new LibroMovimientos(new MovimientoRepositorioMemoria() {
            @Override
            public synchronized void guardar(Movimiento m) {
                throw new RepositorioException("No se pudo guardar el movimiento.", null);
            }
        }, RELOJ);
        assertThrows(RepositorioException.class,
                () -> conFallo.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 100, "U", "c", null));
        assertEquals(0, conFallo.saldoVigente("12345678"));
    }

    @Test
    void persisteEnArchivoYSeReconstruyeElSaldo() throws Exception {
        Path carpeta = Files.createTempDirectory("mov");
        Path archivo = carpeta.resolve("movimientos.txt");
        LibroMovimientos primero = new LibroMovimientos(new MovimientoRepositorioArchivo(archivo), RELOJ);
        primero.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 100, "USUARIO:1", "c1", null);
        primero.registrar("12345678", TipoMovimiento.PARTICIPACION, -30, "PRONOSTICO:p;1", "c2", null);

        LibroMovimientos segundo = new LibroMovimientos(new MovimientoRepositorioArchivo(archivo), RELOJ);
        assertEquals(70, segundo.saldoVigente("12345678"));
        assertEquals("PRONOSTICO:p;1", segundo.movimientosDe("12345678").get(1).origen());
    }
}
