package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.modelo.TorneoConEventos;
import com.matchpoint.cuentas.modelo.TorneoConEventos.EventoConEstado;
import com.matchpoint.cuentas.repositorio.EventoRepositorio;
import com.matchpoint.cuentas.repositorio.TorneoRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * HU-05 (listar torneos y eventos con su estado) y HU-06 (ver las opciones de
 * pronóstico y la fecha y hora de cierre de cada evento). Solo consulta.
 */
public class ConsultaServicio {

    private final TorneoRepositorio torneos;
    private final EventoRepositorio eventos;
    private final Clock reloj;

    public ConsultaServicio(TorneoRepositorio torneos, EventoRepositorio eventos) {
        this(torneos, eventos, Clock.systemDefaultZone());
    }

    public ConsultaServicio(TorneoRepositorio torneos, EventoRepositorio eventos, Clock reloj) {
        this.torneos = torneos;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** HU-05: torneos (por fecha de inicio) con sus eventos (por fecha) y el estado de cada uno. */
    public List<TorneoConEventos> torneosConEventos() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return torneos.listar().stream()
                .sorted(Comparator.comparing(Torneo::fechaInicio).thenComparing(Torneo::nombre))
                .map(t -> new TorneoConEventos(t, eventos.listarPorTorneo(t.id()).stream()
                        .sorted(Comparator.comparing(Evento::inicio))
                        .map(e -> new EventoConEstado(e, e.estado(ahora)))
                        .toList()))
                .toList();
    }

    /**
     * HU-06: opciones de pronóstico, cierre y estado de un evento.
     *
     * @throws ValidacionException si el evento no existe
     */
    public EventoConEstado detalleEvento(String eventoId) {
        Evento evento = eventos.buscarPorId(eventoId == null ? "" : eventoId.trim()).orElseThrow(() ->
                new ValidacionException(Map.of(PronosticoServicio.CAMPO_EVENTO, "El evento seleccionado no existe.")));
        return new EventoConEstado(evento, evento.estado(LocalDateTime.now(reloj)));
    }
}
