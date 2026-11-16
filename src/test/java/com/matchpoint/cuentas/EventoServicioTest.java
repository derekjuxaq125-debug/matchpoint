package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.EstadoEvento;
import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorio;
import com.matchpoint.cuentas.repositorio.EventoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.repositorio.TorneoRepositorioMemoria;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.EventoServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-04: Registrar evento. "Ahora" es siempre 2026-11-16 10:00. */
class EventoServicioTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    static final Usuario ORGANIZADOR = new Usuario("ORG0001", "Org", Rol.ORGANIZADOR, "s", "h");
    static final Usuario PARTICIPANTE = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");

    private EventoRepositorioMemoria repositorio;
    private EventoServicio servicio;
    private Torneo torneo;

    @BeforeEach
    void preparar() {
        TorneoRepositorioMemoria torneos = new TorneoRepositorioMemoria();
        torneo = new Torneo("T1", "Copa", "Fútbol", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31));
        torneos.guardar(torneo);
        repositorio = new EventoRepositorioMemoria();
        servicio = new EventoServicio(repositorio, torneos, RELOJ);
    }

    private Evento registrarValido() {
        return servicio.registrar(ORGANIZADOR, "T1", "Real vs Barça", "2026-11-20 15:00",
                ModalidadPronostico.GANADOR_O_EMPATE, List.of("Real", "Empate", "Barça"),
                "2026-11-20 14:00", "10");
    }

    @Test
    void registraEventoConTodosSusDatos() {
        Evento e = registrarValido();
        assertEquals("Real vs Barça", e.nombre());
        assertEquals(LocalDateTime.of(2026, 11, 20, 15, 0), e.inicio());
        assertEquals(LocalDateTime.of(2026, 11, 20, 14, 0), e.cierre());
        assertEquals(List.of("Real", "Empate", "Barça"), e.opciones());
        assertEquals(10, e.costoCreditos());
        assertEquals(1, repositorio.listar().size());
    }

    @Test
    void soloUnOrganizadorPuedeRegistrar() {
        assertThrows(AccesoDenegadoException.class, () -> servicio.registrar(PARTICIPANTE, "T1", "X",
                "2026-11-20 15:00", ModalidadPronostico.OPCION_MULTIPLE, List.of("a", "b"), "2026-11-20 14:00", "10"));
        assertThrows(AccesoDenegadoException.class, () -> servicio.registrar(null, "T1", "X",
                "2026-11-20 15:00", ModalidadPronostico.OPCION_MULTIPLE, List.of("a", "b"), "2026-11-20 14:00", "10"));
        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void indicaCualesCamposFaltan() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.registrar(ORGANIZADOR, "T1", "  ", "", null, List.of(" ", ""), "", ""));
        assertTrue(ex.getErrores().keySet().containsAll(List.of(
                EventoServicio.CAMPO_NOMBRE, EventoServicio.CAMPO_INICIO, EventoServicio.CAMPO_MODALIDAD,
                EventoServicio.CAMPO_OPCIONES, EventoServicio.CAMPO_CIERRE, EventoServicio.CAMPO_COSTO)));
        assertEquals(6, ex.getErrores().size());
        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void rechazaTorneoInexistente() {
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR,
                "NO-EXISTE", "X", "2026-11-20 15:00", ModalidadPronostico.OPCION_MULTIPLE,
                List.of("a", "b"), "2026-11-20 14:00", "10"));
        assertTrue(ex.getErrores().containsKey(EventoServicio.CAMPO_TORNEO));
    }

    @Test
    void rechazaFechasConFormatoInvalido() {
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR,
                "T1", "X", "20/11/2026 3pm", ModalidadPronostico.OPCION_MULTIPLE,
                List.of("a", "b"), "2026-13-45 99:99", "10"));
        assertTrue(ex.getErrores().containsKey(EventoServicio.CAMPO_INICIO));
        assertTrue(ex.getErrores().containsKey(EventoServicio.CAMPO_CIERRE));
    }

    @Test
    void rechazaEventoEnElPasadoOCierreEnElPasado() {
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR,
                "T1", "X", "2026-11-16 09:00", ModalidadPronostico.OPCION_MULTIPLE,
                List.of("a", "b"), "2026-11-16 08:00", "10"));
        assertEquals("La fecha del evento debe ser futura.", ex.getErrores().get(EventoServicio.CAMPO_INICIO));
        assertEquals("La hora de cierre debe ser futura.", ex.getErrores().get(EventoServicio.CAMPO_CIERRE));
    }

    @Test
    void elCierreNoPuedeSerPosteriorAlInicio() {
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR,
                "T1", "X", "2026-11-20 15:00", ModalidadPronostico.OPCION_MULTIPLE,
                List.of("a", "b"), "2026-11-20 15:01", "10"));
        assertEquals("La hora de cierre no puede ser posterior al inicio del evento.",
                ex.getErrores().get(EventoServicio.CAMPO_CIERRE));
    }

    @Test
    void elCierrePuedeSerExactamenteALaHoraDelInicio() {
        Evento e = servicio.registrar(ORGANIZADOR, "T1", "X", "2026-11-20 15:00",
                ModalidadPronostico.OPCION_MULTIPLE, List.of("a", "b"), "2026-11-20 15:00", "10");
        assertEquals(e.inicio(), e.cierre());
    }

    @Test
    void elEventoDebeEstarDentroDeLasFechasDelTorneo() {
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR,
                "T1", "X", "2027-01-05 15:00", ModalidadPronostico.OPCION_MULTIPLE,
                List.of("a", "b"), "2027-01-05 14:00", "10"));
        assertTrue(ex.getErrores().get(EventoServicio.CAMPO_INICIO).contains("dentro de las fechas del torneo"));
    }

    @Test
    void validaLasOpcionesSegunLaModalidad() {
        // GANADOR_O_EMPATE exige exactamente 3
        assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR, "T1", "X",
                "2026-11-20 15:00", ModalidadPronostico.GANADOR_O_EMPATE, List.of("a", "b"),
                "2026-11-20 14:00", "10"));
        // GANADOR_ENFRENTAMIENTO exige exactamente 2
        assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR, "T1", "X",
                "2026-11-20 15:00", ModalidadPronostico.GANADOR_ENFRENTAMIENTO, List.of("a", "b", "c"),
                "2026-11-20 14:00", "10"));
        // OPCION_MULTIPLE: de 2 a 10
        assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR, "T1", "X",
                "2026-11-20 15:00", ModalidadPronostico.OPCION_MULTIPLE, List.of("solo una"),
                "2026-11-20 14:00", "10"));
        assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR, "T1", "X",
                "2026-11-20 15:00", ModalidadPronostico.OPCION_MULTIPLE,
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"), "2026-11-20 14:00", "10"));
        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void rechazaOpcionesRepetidasSinDistinguirMayusculas() {
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR,
                "T1", "X", "2026-11-20 15:00", ModalidadPronostico.GANADOR_ENFRENTAMIENTO,
                List.of("Real", "REAL"), "2026-11-20 14:00", "10"));
        assertTrue(ex.getErrores().get(EventoServicio.CAMPO_OPCIONES).contains("repetirse"));
    }

    @Test
    void ignoraLasLineasVaciasDeLasOpciones() {
        Evento e = servicio.registrar(ORGANIZADOR, "T1", "X", "2026-11-20 15:00",
                ModalidadPronostico.GANADOR_ENFRENTAMIENTO, List.of(" Real ", "", "  ", "Barça"),
                "2026-11-20 14:00", "10");
        assertEquals(List.of("Real", "Barça"), e.opciones());
    }

    @Test
    void elCostoDebeSerEnteroMayorQueCero() {
        for (String costo : List.of("0", "-5", "abc", "2.5")) {
            ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(
                    ORGANIZADOR, "T1", "X", "2026-11-20 15:00", ModalidadPronostico.OPCION_MULTIPLE,
                    List.of("a", "b"), "2026-11-20 14:00", costo));
            assertTrue(ex.getErrores().containsKey(EventoServicio.CAMPO_COSTO), costo);
        }
    }

    @Test
    void rechazaEventoDuplicadoEnElMismoTorneo() {
        registrarValido();
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.registrar(ORGANIZADOR,
                "T1", "REAL VS BARÇA", "2026-11-20 15:00", ModalidadPronostico.OPCION_MULTIPLE,
                List.of("a", "b"), "2026-11-20 14:00", "10"));
        assertTrue(ex.getErrores().containsKey(EventoServicio.CAMPO_NOMBRE));
        assertEquals(1, repositorio.listar().size());
    }

    @Test
    void elEstadoDependeDeLaHoraDeCierre() {
        Evento e = registrarValido();
        assertEquals(EstadoEvento.ABIERTO, e.estado(LocalDateTime.of(2026, 11, 20, 13, 59)));
        assertEquals(EstadoEvento.CERRADO, e.estado(LocalDateTime.of(2026, 11, 20, 14, 0)));
        assertTrue(e.estaHabilitado(LocalDateTime.of(2026, 11, 16, 10, 0)));
    }

    @Test
    void siFallaElGuardadoNoSeCreaElEvento() {
        EventoRepositorio roto = new EventoRepositorioMemoria() {
            @Override
            public void guardar(Evento evento) {
                throw new RepositorioException("No se pudo guardar el evento.", new RuntimeException());
            }
        };
        TorneoRepositorioMemoria torneos = new TorneoRepositorioMemoria();
        torneos.guardar(torneo);
        EventoServicio s = new EventoServicio(roto, torneos, RELOJ);
        assertThrows(RepositorioException.class, () -> s.registrar(ORGANIZADOR, "T1", "X", "2026-11-20 15:00",
                ModalidadPronostico.OPCION_MULTIPLE, List.of("a", "b"), "2026-11-20 14:00", "10"));
        assertTrue(roto.listar().isEmpty());
    }
}
