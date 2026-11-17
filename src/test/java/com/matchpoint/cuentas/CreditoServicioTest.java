package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorioMemoria;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.CreditoServicio;
import com.matchpoint.cuentas.servicio.LibroMovimientos;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-07 (casos P-07-01 a P-07-06 del Resumen de pruebas). */
class CreditoServicioTest {

    static final Usuario ORG = new Usuario("ORG0001", "Org", Rol.ORGANIZADOR, "s", "h");
    static final Usuario ANA = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");

    private MovimientoRepositorioMemoria movimientos;
    private LibroMovimientos libro;
    private CreditoServicio servicio;

    @BeforeEach
    void preparar() {
        UsuarioRepositorioMemoria usuarios = new UsuarioRepositorioMemoria();
        usuarios.guardar(ANA);
        movimientos = new MovimientoRepositorioMemoria();
        libro = new LibroMovimientos(movimientos);
        servicio = new CreditoServicio(usuarios, libro);
    }

    @Test
    void p0701AsignaSaldoInicialYQuedaComoMovimiento() {
        Movimiento m = servicio.asignarInicial(ORG, "12345678", "500");
        assertEquals(TipoMovimiento.ASIGNACION_INICIAL, m.tipo());
        assertEquals(500, libro.saldoVigente("12345678"));
        assertEquals(Optional.of(500), servicio.saldoInicialDe("12345678"));
    }

    @Test
    void p0702RechazaCantidadCeroNegativaVaciaONoNumerica() {
        for (String mala : new String[]{"0", "-5", "", "  ", "abc", "10.5"}) {
            ValidacionException ex = assertThrows(ValidacionException.class,
                    () -> servicio.asignarInicial(ORG, "12345678", mala));
            assertTrue(ex.getErrores().containsKey(CreditoServicio.CAMPO_CANTIDAD), mala);
        }
        assertTrue(movimientos.listar().isEmpty());
    }

    @Test
    void p0703NoPermiteAsignarDosVeces() {
        servicio.asignarInicial(ORG, "12345678", "100");
        assertThrows(ValidacionException.class, () -> servicio.asignarInicial(ORG, "12345678", "300"));
        assertEquals(100, libro.saldoVigente("12345678"));
        assertEquals(1, movimientos.listar().size());
    }

    @Test
    void p0705SiFallaElGuardadoElSaldoNoCambia() {
        UsuarioRepositorioMemoria usuarios = new UsuarioRepositorioMemoria();
        usuarios.guardar(ANA);
        LibroMovimientos conFallo = new LibroMovimientos(new MovimientoRepositorioMemoria() {
            @Override
            public synchronized void guardar(Movimiento m) {
                throw new RepositorioException("No se pudo guardar el movimiento.", null);
            }
        });
        CreditoServicio s = new CreditoServicio(usuarios, conFallo);
        assertThrows(RepositorioException.class, () -> s.asignarInicial(ORG, "12345678", "100"));
        assertEquals(0, conFallo.saldoVigente("12345678"));
    }

    @Test
    void p0706RechazaCantidadMayorAlMaximo() {
        assertThrows(ValidacionException.class, () -> servicio.asignarInicial(ORG, "12345678", "10001"));
        servicio.asignarInicial(ORG, "12345678", "10000");
    }

    @Test
    void soloUnOrganizadorAsignaYElUsuarioDebeExistir() {
        assertThrows(AccesoDenegadoException.class, () -> servicio.asignarInicial(ANA, "12345678", "100"));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.asignarInicial(ORG, "99999999", "100"));
        assertTrue(ex.getErrores().containsKey(CreditoServicio.CAMPO_USUARIO));
        assertThrows(ValidacionException.class, () -> servicio.asignarInicial(ORG, null, "100"));
    }
}
