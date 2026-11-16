package com.matchpoint.cuentas.modelo;

import java.time.LocalDateTime;

/**
 * Pronóstico de un participante sobre un evento (HU-10). {@code id} es único y
 * sirve de referencia para el movimiento de débito (HU-09 / HU-11).
 */
public record Pronostico(String id, String usuarioId, String eventoId, String opcion, LocalDateTime registrado) {
}
