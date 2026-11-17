package com.matchpoint.torneos.modelo;

import java.time.LocalDate;

/** Torneo creado por un organizador. */
public record Torneo(String nombre, Deporte deporte, LocalDate fechaInicio, LocalDate fechaFin) {
}
