package com.matchpoint.cuentas.modelo;

import java.util.List;

/**
 * Roles del sistema (HU-02). Cada rol define las únicas opciones que se le
 * muestran después de iniciar sesión.
 */
public enum Rol {
    PARTICIPANTE("Participante", List.of(
            Opcion.LISTAR_TORNEOS_Y_EVENTOS,
            Opcion.VER_OPCIONES_PRONOSTICO,
            Opcion.REGISTRAR_PRONOSTICO,
            Opcion.CAMBIAR_PRONOSTICO,
            Opcion.CONSULTAR_SALDO)),
    ORGANIZADOR("Organizador", List.of(
            Opcion.CREAR_TORNEO,
            Opcion.REGISTRAR_EVENTO,
            Opcion.ASIGNAR_CREDITOS)),
    AUDITOR("Auditor", List.of(
            Opcion.REPORTE_CONSOLIDADO,
            Opcion.DETECTAR_INCONSISTENCIAS));

    private final String etiqueta;
    private final List<Opcion> opciones;

    Rol(String etiqueta, List<Opcion> opciones) {
        this.etiqueta = etiqueta;
        this.opciones = opciones;
    }

    /** Opciones de menú permitidas para este rol. */
    public List<Opcion> getOpciones() {
        return opciones;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
