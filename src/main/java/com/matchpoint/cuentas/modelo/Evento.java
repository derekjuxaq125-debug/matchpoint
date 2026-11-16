package com.matchpoint.cuentas.modelo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de un torneo sobre el que se registran pronósticos (HU-04).
 *
 * @param inicio         fecha y hora en que se juega el evento
 * @param cierre         hora límite para registrar o cambiar pronósticos
 * @param costoCreditos  créditos que cuesta cada pronóstico (lo descuenta HU-11)
 */
public record Evento(String id, String torneoId, String nombre, LocalDateTime inicio,
                     ModalidadPronostico modalidad, List<String> opciones,
                     LocalDateTime cierre, int costoCreditos) {

    public Evento {
        opciones = List.copyOf(opciones);
    }

    /** El estado depende solo de la hora de cierre: abierto hasta ese instante. */
    public EstadoEvento estado(LocalDateTime ahora) {
        return ahora.isBefore(cierre) ? EstadoEvento.ABIERTO : EstadoEvento.CERRADO;
    }

    /** ¿Admite pronósticos en este momento? (base de HU-10 y HU-12) */
    public boolean estaHabilitado(LocalDateTime ahora) {
        return estado(ahora) == EstadoEvento.ABIERTO;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
