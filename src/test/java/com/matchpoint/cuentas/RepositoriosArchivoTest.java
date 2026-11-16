package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import com.matchpoint.cuentas.modelo.Pronostico;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.repositorio.EventoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.TorneoRepositorioArchivo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Los datos de HU-04 y HU-10 persisten en archivo y se leen igual que se guardaron. */
class RepositoriosArchivoTest {

    @Test
    void torneoEventoYPronosticoSobrevivenAGuardarYLeer() throws Exception {
        Path carpeta = Files.createTempDirectory("matchpoint");
        Torneo torneo = new Torneo("T1", "Copa; \"Ñandú\"", "Fútbol", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31));
        Evento evento = new Evento("E1", "T1", "Ñ vs B; ida", LocalDateTime.of(2026, 11, 20, 15, 0),
                ModalidadPronostico.OPCION_MULTIPLE, List.of("Uno, dos", "Tres;4", "Ñu"),
                LocalDateTime.of(2026, 11, 20, 14, 0), 25);
        Pronostico pronostico = new Pronostico("P1", "12345678", "E1", "Ñu", LocalDateTime.of(2026, 11, 16, 10, 0));

        new TorneoRepositorioArchivo(carpeta.resolve("t.txt")).guardar(torneo);
        new EventoRepositorioArchivo(carpeta.resolve("e.txt")).guardar(evento);
        new PronosticoRepositorioArchivo(carpeta.resolve("p.txt")).guardar(pronostico);

        // repositorios nuevos: leen lo que quedó en disco
        assertEquals(List.of(torneo), new TorneoRepositorioArchivo(carpeta.resolve("t.txt")).listar());
        EventoRepositorioArchivo eventos = new EventoRepositorioArchivo(carpeta.resolve("e.txt"));
        assertEquals(List.of(evento), eventos.listar());
        assertEquals(evento, eventos.buscarPorId("E1").orElseThrow());
        assertEquals(1, eventos.listarPorTorneo("T1").size());
        assertTrue(eventos.listarPorTorneo("otro").isEmpty());
        PronosticoRepositorioArchivo pronosticos = new PronosticoRepositorioArchivo(carpeta.resolve("p.txt"));
        assertEquals(pronostico, pronosticos.buscarPorUsuarioYEvento("12345678", "E1").orElseThrow());
        assertTrue(pronosticos.buscarPorUsuarioYEvento("otro", "E1").isEmpty());
    }

    @Test
    void sinArchivoDevuelveListaVacia() throws Exception {
        Path carpeta = Files.createTempDirectory("matchpoint");
        assertTrue(new EventoRepositorioArchivo(carpeta.resolve("no-existe.txt")).listar().isEmpty());
    }
}
