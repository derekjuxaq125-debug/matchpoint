package com.matchpoint.cuentas.modelo;

/** Estado de un evento según su hora de cierre. */
public enum EstadoEvento {
    ABIERTO("Abierto"),
    CERRADO("Cerrado");

    private final String etiqueta;

    EstadoEvento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
