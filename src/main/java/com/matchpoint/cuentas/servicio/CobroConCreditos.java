package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.modelo.Usuario;

/**
 * HU-11: cobro real de un pronóstico. Verifica saldo suficiente y descuenta
 * {@link Evento#costoCreditos()} con un movimiento de PARTICIPACION cuyo origen
 * es el pronóstico. Es la estrategia que reemplaza a {@link CobroPronostico#NINGUNO}.
 */
public class CobroConCreditos implements CobroPronostico {

    private final LibroMovimientos libro;

    public CobroConCreditos(LibroMovimientos libro) {
        this.libro = libro;
    }

    @Override
    public void cobrar(Usuario usuario, Evento evento, String referencia) {
        libro.registrar(usuario.identificacion(), TipoMovimiento.PARTICIPACION, -evento.costoCreditos(),
                origen(referencia), claveCobro(referencia), null);
    }

    @Override
    public void revertir(Usuario usuario, Evento evento, String referencia) {
        libro.registrar(usuario.identificacion(), TipoMovimiento.REVERSION, evento.costoCreditos(),
                origen(referencia), "REVERSION-" + claveCobro(referencia),
                "Falló el guardado del pronóstico; se devuelven los créditos descontados.");
    }

    public static String claveCobro(String referencia) {
        return "COBRO-" + referencia;
    }

    private static String origen(String referencia) {
        return "PRONOSTICO:" + referencia;
    }
}
