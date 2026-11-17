package com.matchpoint.cuentas.modelo;

import java.time.LocalDateTime;

/**
 * Movimiento confirmado del libro de créditos (HU-09). Es inmutable: una vez
 * guardado no se modifica ni se borra; las correcciones se hacen con nuevos
 * movimientos de ajuste o reversión (HU-23).
 *
 * @param operacion          identificador único de la operación
 * @param cantidad           positiva si suma créditos, negativa si los descuenta
 * @param saldoResultante    saldo del usuario justo después del movimiento
 * @param origen             operación que lo originó (p. ej. "PRONOSTICO:&lt;id&gt;")
 * @param claveIdempotencia  llave única; la misma clave nunca genera dos movimientos (HU-14)
 */
public record Movimiento(String operacion, String usuarioId, TipoMovimiento tipo, int cantidad,
                         int saldoResultante, String origen, String claveIdempotencia,
                         LocalDateTime registrado, String justificacion) {
}
