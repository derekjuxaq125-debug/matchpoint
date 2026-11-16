package com.matchpoint.cuentas.modelo;

import java.util.List;

/**
 * Modalidad de pronóstico de un evento (HU-04). Cada modalidad sabe validar sus
 * propias opciones; para agregar una modalidad nueva basta con añadir un valor
 * al enum (no hay que tocar el servicio).
 */
public enum ModalidadPronostico {
    GANADOR_ENFRENTAMIENTO("Ganador (2 opciones, sin empate)", 2, 2),
    GANADOR_O_EMPATE("Local / Empate / Visitante (3 opciones)", 3, 3),
    OPCION_MULTIPLE("Opción múltiple (2 a 10 opciones)", 2, 10);

    private final String etiqueta;
    private final int minOpciones;
    private final int maxOpciones;

    ModalidadPronostico(String etiqueta, int minOpciones, int maxOpciones) {
        this.etiqueta = etiqueta;
        this.minOpciones = minOpciones;
        this.maxOpciones = maxOpciones;
    }

    /** Mensaje de error si la cantidad de opciones no es válida; {@code null} si lo es. */
    public String validarCantidad(List<String> opciones) {
        int n = opciones.size();
        if (n >= minOpciones && n <= maxOpciones) {
            return null;
        }
        return minOpciones == maxOpciones
                ? "La modalidad elegida requiere exactamente " + minOpciones + " opciones."
                : "La modalidad elegida requiere entre " + minOpciones + " y " + maxOpciones + " opciones.";
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
