package com.matchpoint.cuentas.modelo;

import java.util.List;

/**
 * Roles del sistema (HU-02). Cada rol define las únicas opciones que se le
 * muestran después de iniciar sesión.
 */
public enum Rol {
    PARTICIPANTE("Participante", List.of(
            "Listar torneos y eventos",
            "Ver opciones de pronóstico",
            "Consultar mi saldo")),
    ORGANIZADOR("Organizador", List.of(
            "Crear torneo",
            "Registrar evento y opciones",
            "Asignar créditos iniciales")),
    AUDITOR("Auditor", List.of(
            "Reporte consolidado",
            "Detectar inconsistencias"));

    private final String etiqueta;
    private final List<String> opciones;

    Rol(String etiqueta, List<String> opciones) {
        this.etiqueta = etiqueta;
        this.opciones = opciones;
    }

    /** Opciones de menú permitidas para este rol. */
    public List<String> getOpciones() {
        return opciones;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
