package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;

/**
 * HU-26: regla única de acceso a participaciones y movimientos. Un participante
 * solo ve y gestiona lo suyo; el auditor puede consultar (solo lectura) a
 * cualquiera para atender reclamaciones; el organizador no ve datos de créditos
 * ni pronósticos individuales.
 */
public final class AccesoDatos {

    public static final String MENSAJE_AJENO = "No tiene permiso para ver ni gestionar datos de otro usuario.";

    private AccesoDatos() {
    }

    /** @throws AccesoDenegadoException si {@code actor} no puede ver los datos de {@code duenoId} */
    public static void exigirLectura(Usuario actor, String duenoId) {
        if (actor == null) {
            throw new AccesoDenegadoException("Debe iniciar sesión.");
        }
        boolean propio = duenoId != null && actor.identificacion().equalsIgnoreCase(duenoId.trim());
        if (actor.rol() == Rol.AUDITOR || (actor.rol() == Rol.PARTICIPANTE && propio)) {
            return;
        }
        throw new AccesoDenegadoException(MENSAJE_AJENO);
    }

    /** Gestionar (cambiar) solo lo propio; ni el auditor modifica datos ajenos. */
    public static void exigirGestion(Usuario actor, String duenoId) {
        if (actor == null || actor.rol() != Rol.PARTICIPANTE || duenoId == null
                || !actor.identificacion().equalsIgnoreCase(duenoId.trim())) {
            throw new AccesoDenegadoException(MENSAJE_AJENO);
        }
    }
}
