package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.ConsultaSaldo;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioMemoria;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.LibroMovimientos;
import com.matchpoint.cuentas.servicio.SaldoServicio;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-08: el saldo consultado coincide con la suma de los movimientos. */
class SaldoServicioTest {

    static final Usuario ANA = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");

    @Test
    void sinMovimientosElSaldoEsCero() {
        ConsultaSaldo c = new SaldoServicio(new LibroMovimientos(new MovimientoRepositorioMemoria())).consultar(ANA);
        assertEquals(0, c.saldo());
        assertTrue(c.coincide());
    }

    @Test
    void elSaldoCoincideConLaSumaDeMovimientos() {
        LibroMovimientos libro = new LibroMovimientos(new MovimientoRepositorioMemoria());
        libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 100, "U", "c1", null);
        libro.registrar("12345678", TipoMovimiento.PARTICIPACION, -10, "P1", "c2", null);
        libro.registrar("12345678", TipoMovimiento.PARTICIPACION, -25, "P2", "c3", null);
        libro.registrar("87654321", TipoMovimiento.ASIGNACION_INICIAL, 500, "U", "c4", null);
        ConsultaSaldo c = new SaldoServicio(libro).consultar(ANA);
        assertEquals(65, c.saldo());
        assertEquals(65, c.sumaMovimientos());
        assertEquals(3, c.movimientos().size(), "solo los movimientos propios");
        assertTrue(c.coincide());
    }

    @Test
    void soloElParticipanteConsultaSuSaldo() {
        Usuario org = new Usuario("ORG0001", "Org", Rol.ORGANIZADOR, "s", "h");
        assertThrows(AccesoDenegadoException.class,
                () -> new SaldoServicio(new LibroMovimientos(new MovimientoRepositorioMemoria())).consultar(org));
    }
}
