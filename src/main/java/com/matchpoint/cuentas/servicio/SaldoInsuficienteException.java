package com.matchpoint.cuentas.servicio;

import java.util.Map;

/** HU-11: la operación dejaría el saldo negativo, por eso se rechaza completa. */
public class SaldoInsuficienteException extends ValidacionException {

    private final int saldoDisponible;
    private final int requerido;

    public SaldoInsuficienteException(int saldoDisponible, int requerido) {
        super(Map.of(PronosticoServicio.CAMPO_EVENTO, "Saldo insuficiente: tiene " + saldoDisponible
                + " créditos y la operación requiere " + requerido + ". No se registró nada."));
        this.saldoDisponible = saldoDisponible;
        this.requerido = requerido;
    }

    public int getSaldoDisponible() {
        return saldoDisponible;
    }

    public int getRequerido() {
        return requerido;
    }
}
