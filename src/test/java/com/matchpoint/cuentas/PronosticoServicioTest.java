package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import com.matchpoint.cuentas.modelo.Pronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorio;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.CobroPronostico;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-10 (registrar pronóstico) y de la regla de cierre de HU-12. "Ahora" es 2026-11-16 10:00. */
class PronosticoServicioTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    static final Usuario ANA = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");
    static final Usuario LUIS = new Usuario("87654321", "Luis", Rol.PARTICIPANTE, "s", "h");
    static final Usuario ORGANIZADOR = new Usuario("ORG0001", "Org", Rol.ORGANIZADOR, "s", "h");

    private EventoRepositorioMemoria eventos;
    private PronosticoRepositorioMemoria pronosticos;
    private PronosticoServicio servicio;
    private Evento abierto;
    private Evento cerrado;

    @BeforeEach
    void preparar() {
        eventos = new EventoRepositorioMemoria();
        pronosticos = new PronosticoRepositorioMemoria();
        servicio = new PronosticoServicio(eventos, pronosticos, CobroPronostico.NINGUNO, RELOJ);
        abierto = new Evento("E1", "T1", "Real vs Barça", LocalDateTime.of(2026, 11, 20, 15, 0),
                ModalidadPronostico.GANADOR_O_EMPATE, List.of("Real", "Empate", "Barça"),
                LocalDateTime.of(2026, 11, 20, 14, 0), 10);
        cerrado = new Evento("E2", "T1", "Final", LocalDateTime.of(2026, 11, 16, 12, 0),
                ModalidadPronostico.GANADOR_ENFRENTAMIENTO, List.of("A", "B"),
                LocalDateTime.of(2026, 11, 16, 10, 0), 10); // cierra justo ahora
        eventos.guardar(abierto);
        eventos.guardar(cerrado);
    }

    @Test
    void registraPronosticoEnEventoHabilitadoConOpcionValida() {
        Pronostico p = servicio.registrar(ANA, "E1", "Empate");
        assertEquals("12345678", p.usuarioId());
        assertEquals("E1", p.eventoId());
        assertEquals("Empate", p.opcion());
        assertEquals(LocalDateTime.of(2026, 11, 16, 10, 0), p.registrado());
        assertEquals(1, pronosticos.listar().size());
    }

    @Test
    void guardaLaOpcionTalComoLaDefinioElEvento() {
        Pronostico p = servicio.registrar(ANA, "E1", "  barça ");
        assertEquals("Barça", p.opcion());
    }

    @Test
    void rechazaOpcionQueNoPerteneceAlEvento() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.registrar(ANA, "E1", "Atlético"));
        assertTrue(ex.getErrores().containsKey(PronosticoServicio.CAMPO_OPCION));
        assertTrue(pronosticos.listar().isEmpty());
    }

    @Test
    void rechazaPronosticoEnEventoCerrado() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.registrar(ANA, "E2", "A"));
        assertEquals(PronosticoServicio.MENSAJE_EVENTO_CERRADO, ex.getErrores().get(PronosticoServicio.CAMPO_EVENTO));
        assertTrue(pronosticos.listar().isEmpty());
    }

    @Test
    void rechazaEventoInexistente() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.registrar(ANA, "NO-EXISTE", "A"));
        assertTrue(ex.getErrores().containsKey(PronosticoServicio.CAMPO_EVENTO));
    }

    @Test
    void indicaCualCampoFalta() {
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ANA, "", " "));
        assertEquals(2, ex.getErrores().size());
        ValidacionException ex2 = assertThrows(ValidacionException.class, () -> servicio.registrar(ANA, "E1", null));
        assertEquals(1, ex2.getErrores().size());
        assertTrue(ex2.getErrores().containsKey(PronosticoServicio.CAMPO_OPCION));
    }

    @Test
    void soloUnParticipantePuedePronosticar() {
        assertThrows(AccesoDenegadoException.class, () -> servicio.registrar(ORGANIZADOR, "E1", "Real"));
        assertThrows(AccesoDenegadoException.class, () -> servicio.registrar(null, "E1", "Real"));
        assertTrue(pronosticos.listar().isEmpty());
    }

    @Test
    void unParticipanteSoloPuedePronosticarUnaVezPorEvento() {
        servicio.registrar(ANA, "E1", "Real");
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ANA, "E1", "Barça"));
        assertEquals("Ya registró un pronóstico en este evento.", ex.getErrores().get(PronosticoServicio.CAMPO_EVENTO));
        assertEquals(1, pronosticos.listar().size());
        servicio.registrar(LUIS, "E1", "Real"); // otro participante sí puede
        assertEquals(2, pronosticos.listar().size());
    }

    @Test
    void llamaAlCobroConElIdDelPronosticoAntesDeGuardar() {
        List<String> referencias = new ArrayList<>();
        PronosticoServicio conCobro = new PronosticoServicio(eventos, pronosticos, (u, e, ref) -> {
            assertTrue(pronosticos.listar().isEmpty()); // todavía no se guardó
            assertEquals(10, e.costoCreditos());
            referencias.add(ref);
        }, RELOJ);
        Pronostico p = conCobro.registrar(ANA, "E1", "Real");
        assertEquals(List.of(p.id()), referencias);
    }

    @Test
    void siElCobroFallaNoSeGuardaElPronostico() {
        PronosticoServicio sinSaldo = new PronosticoServicio(eventos, pronosticos, (u, e, ref) -> {
            throw new IllegalStateException("Saldo insuficiente.");
        }, RELOJ);
        assertThrows(IllegalStateException.class, () -> sinSaldo.registrar(ANA, "E1", "Real"));
        assertTrue(pronosticos.listar().isEmpty());
    }

    @Test
    void siFallaElGuardadoNoSeCreaElPronostico() {
        PronosticoRepositorio roto = new PronosticoRepositorioMemoria() {
            @Override
            public void guardar(Pronostico pronostico) {
                throw new RepositorioException("No se pudo guardar el pronóstico.", new RuntimeException());
            }
        };
        PronosticoServicio s = new PronosticoServicio(eventos, roto, CobroPronostico.NINGUNO, RELOJ);
        assertThrows(RepositorioException.class, () -> s.registrar(ANA, "E1", "Real"));
        assertTrue(roto.listar().isEmpty());
    }
}
