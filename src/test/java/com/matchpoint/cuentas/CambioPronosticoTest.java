package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import com.matchpoint.cuentas.modelo.Pronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioMemoria;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.CobroPronostico;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-12: rechazar pronósticos y cambios cuando el evento ya está cerrado. */
class CambioPronosticoTest {

    static final Usuario ANA = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");
    static final Usuario LUIS = new Usuario("87654321", "Luis", Rol.PARTICIPANTE, "s", "h");

    private EventoRepositorioMemoria eventos;
    private PronosticoRepositorioMemoria pronosticos;

    @BeforeEach
    void preparar() {
        eventos = new EventoRepositorioMemoria();
        pronosticos = new PronosticoRepositorioMemoria();
        eventos.guardar(new Evento("E1", "T1", "Clásico", LocalDateTime.of(2026, 11, 20, 15, 0),
                ModalidadPronostico.GANADOR_O_EMPATE, List.of("Local", "Empate", "Visitante"),
                LocalDateTime.of(2026, 11, 20, 14, 0), 10));
    }

    private PronosticoServicio servicioEn(String instante) {
        return new PronosticoServicio(eventos, pronosticos, CobroPronostico.NINGUNO,
                Clock.fixed(Instant.parse(instante), ZoneOffset.UTC));
    }

    @Test
    void antesDelCierreSePuedeCambiarLaOpcion() {
        Pronostico p = servicioEn("2026-11-16T10:00:00Z").registrar(ANA, "E1", "Local");
        Pronostico cambiado = servicioEn("2026-11-20T13:59:00Z").cambiar(ANA, p.id(), "empate");
        assertEquals("Empate", cambiado.opcion());
        assertEquals(p.registrado(), cambiado.registrado(), "se conserva la fecha original");
        assertEquals("Empate", pronosticos.listar().get(0).opcion());
    }

    @Test
    void despuesDelCierreSeRechazaElCambioConMensajeClaro() {
        Pronostico p = servicioEn("2026-11-16T10:00:00Z").registrar(ANA, "E1", "Local");
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicioEn("2026-11-20T14:00:00Z").cambiar(ANA, p.id(), "Visitante"));
        assertEquals(PronosticoServicio.MENSAJE_CAMBIO_CERRADO, ex.getErrores().get(PronosticoServicio.CAMPO_EVENTO));
        assertEquals("Local", pronosticos.listar().get(0).opcion(), "la participación original no cambia");
    }

    @Test
    void despuesDelCierreSeRechazaUnPronosticoNuevo() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicioEn("2026-11-20T14:00:00Z").registrar(LUIS, "E1", "Local"));
        assertEquals(PronosticoServicio.MENSAJE_EVENTO_CERRADO, ex.getErrores().get(PronosticoServicio.CAMPO_EVENTO));
    }

    @Test
    void nadieCambiaElPronosticoDeOtro() {
        Pronostico p = servicioEn("2026-11-16T10:00:00Z").registrar(ANA, "E1", "Local");
        assertThrows(AccesoDenegadoException.class,
                () -> servicioEn("2026-11-16T10:00:00Z").cambiar(LUIS, p.id(), "Empate"));
    }

    @Test
    void elCambioSePersisteEnArchivo() throws Exception {
        Path archivo = Files.createTempDirectory("pron").resolve("pronosticos.txt");
        PronosticoRepositorioArchivo repo = new PronosticoRepositorioArchivo(archivo);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
        PronosticoServicio s = new PronosticoServicio(eventos, repo, CobroPronostico.NINGUNO, reloj);
        Pronostico p = s.registrar(ANA, "E1", "Local");
        s.cambiar(ANA, p.id(), "Visitante");
        assertEquals("Visitante", new PronosticoRepositorioArchivo(archivo).listar().get(0).opcion());
    }
}
