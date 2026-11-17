package com.matchpoint.cuentas.modelo;

import java.util.List;

/** HU-05: un torneo con sus eventos y el estado de cada uno en el momento de la consulta. */
public record TorneoConEventos(Torneo torneo, List<EventoConEstado> eventos) {

    public TorneoConEventos {
        eventos = List.copyOf(eventos);
    }

    /** HU-05 / HU-06: evento con su estado (abierto o cerrado). */
    public record EventoConEstado(Evento evento, EstadoEvento estado) {
    }
}
