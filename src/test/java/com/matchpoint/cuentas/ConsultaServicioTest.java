package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.EstadoEvento;
import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.modelo.TorneoConEventos;
import com.matchpoint.cuentas.repositorio.EventoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.TorneoRepositorioMemoria;
import com.matchpoint.cuentas.servicio.ConsultaServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-05 (listar torneos y eventos con estado) y HU-06 (opciones y cierre). Ahora: 2026-11-16 10:00. */
class ConsultaServicioTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    private ConsultaServicio servicio;

    @BeforeEach
    void preparar() {
        TorneoRepositorioMemoria torneos = new TorneoRepositorioMemoria();
        EventoRepositorioMemoria eventos = new EventoRepositorioMemoria();
        torneos.guardar(new Torneo("T2", "Liga B", "Tenis", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 9)));
        torneos.guardar(new Torneo("T1", "Copa A", "Fútbol", LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 30)));
        eventos.guardar(new Evento("E2", "T1", "Final", LocalDateTime.of(2026, 11, 25, 18, 0),
                ModalidadPronostico.GANADOR_ENFRENTAMIENTO, List.of("A", "B"), LocalDateTime.of(2026, 11, 25, 17, 0), 20));
        eventos.guardar(new Evento("E1", "T1", "Semifinal", LocalDateTime.of(2026, 11, 16, 11, 0),
                ModalidadPronostico.GANADOR_O_EMPATE, List.of("Local", "Empate", "Visitante"),
                LocalDateTime.of(2026, 11, 16, 9, 30), 10));
        servicio = new ConsultaServicio(torneos, eventos, RELOJ);
    }

    @Test
    void hu05ListaTorneosConSusEventosYEstado() {
        List<TorneoConEventos> lista = servicio.torneosConEventos();
        assertEquals("Copa A", lista.get(0).torneo().nombre(), "ordenados por fecha de inicio");
        assertEquals(2, lista.get(0).eventos().size());
        assertEquals("Semifinal", lista.get(0).eventos().get(0).evento().nombre(), "eventos por fecha");
        assertEquals(EstadoEvento.CERRADO, lista.get(0).eventos().get(0).estado());
        assertEquals(EstadoEvento.ABIERTO, lista.get(0).eventos().get(1).estado());
        assertTrue(lista.get(1).eventos().isEmpty(), "un torneo sin eventos también se lista");
    }

    @Test
    void hu06MuestraOpcionesYCierreDelEvento() {
        var detalle = servicio.detalleEvento("E2");
        assertEquals(List.of("A", "B"), detalle.evento().opciones());
        assertEquals(LocalDateTime.of(2026, 11, 25, 17, 0), detalle.evento().cierre());
        assertEquals(EstadoEvento.ABIERTO, detalle.estado());
    }

    @Test
    void hu06EventoInexistente() {
        assertThrows(ValidacionException.class, () -> servicio.detalleEvento("NO"));
    }
}
