package com.matchpoint.cuentas.modelo;

import java.time.LocalDate;

/**
 * Torneo deportivo (HU-03). Las validaciones están en
 * {@link com.matchpoint.cuentas.servicio.TorneoServicio}.
 */
public record Torneo(String id, String nombre, String deporte, LocalDate fechaInicio, LocalDate fechaFin) {

    @Override
    public String toString() {
        return nombre;
    }
}
