package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.ConsultaSaldo;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;

/** HU-08: consultar el saldo de créditos; debe coincidir con la suma de los movimientos. */
public class SaldoServicio {

    private final LibroMovimientos libro;

    public SaldoServicio(LibroMovimientos libro) {
        this.libro = libro;
    }

    /** Saldo del propio participante. */
    public ConsultaSaldo consultar(Usuario actor) {
        if (actor == null || actor.rol() != Rol.PARTICIPANTE) {
            throw new AccesoDenegadoException("Solo un participante consulta su saldo de créditos.");
        }
        return armar(actor.identificacion());
    }

    /** HU-26: saldo y movimientos de un usuario; solo el propio participante o un auditor. */
    public ConsultaSaldo consultarDe(Usuario actor, String usuarioId) {
        AccesoDatos.exigirLectura(actor, usuarioId);
        return armar(usuarioId.trim());
    }

    private ConsultaSaldo armar(String id) {
        return new ConsultaSaldo(id, libro.saldoVigente(id), libro.sumaMovimientos(id), libro.movimientosDe(id));
    }
}
