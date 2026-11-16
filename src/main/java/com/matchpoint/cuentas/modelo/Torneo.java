package com.matchpoint.cuentas.modelo;

import java.time.LocalDate;

/**
 * Torneo. Modelo mínimo que HU-04 necesita para asociar eventos; la historia
 * HU-03 (Johan) lo completa con sus validaciones y pantalla de creación.
 */
public record Torneo(String id, String nombre, String deporte, LocalDate fechaInicio, LocalDate fechaFin) {

    @Override
    public String toString() {
        return nombre;
    }
}
