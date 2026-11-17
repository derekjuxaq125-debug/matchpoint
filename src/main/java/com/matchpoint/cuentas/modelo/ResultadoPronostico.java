package com.matchpoint.cuentas.modelo;

/**
 * Respuesta de registrar un pronóstico con clave de solicitud (HU-14).
 *
 * @param repetida {@code true} si la solicitud ya se había procesado: se devuelve
 *                 el pronóstico original y no se cobró de nuevo
 */
public record ResultadoPronostico(Pronostico pronostico, boolean repetida) {
}
