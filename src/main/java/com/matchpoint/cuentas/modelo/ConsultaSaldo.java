package com.matchpoint.cuentas.modelo;

import java.util.List;

/**
 * HU-08: saldo que se le muestra al participante y la prueba de que coincide
 * con la suma de sus movimientos.
 */
public record ConsultaSaldo(String usuarioId, int saldo, int sumaMovimientos, List<Movimiento> movimientos) {

    public ConsultaSaldo {
        movimientos = List.copyOf(movimientos);
    }

    public boolean coincide() {
        return saldo == sumaMovimientos;
    }
}
