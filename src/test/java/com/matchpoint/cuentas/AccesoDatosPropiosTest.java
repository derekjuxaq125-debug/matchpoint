package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.matchpoint.cuentas.modelo.Pronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioMemoria;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.CobroPronostico;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-26: cada participante ve y gestiona solo sus participaciones y movimientos. */
class AccesoDatosPropiosTest {

    static final Usuario ANA = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");
    static final Usuario LUIS = new Usuario("87654321", "Luis", Rol.PARTICIPANTE, "s", "h");
    static final Usuario ORG = new Usuario("ORG0001", "Org", Rol.ORGANIZADOR, "s", "h");
    static final Usuario AUD = new Usuario("AUD0001", "Aud", Rol.AUDITOR, "s", "h");

    private PronosticoServicio pronosticos;
    private Pronostico deAna;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
        EventoRepositorioMemoria eventos = new EventoRepositorioMemoria();
        eventos.guardar(CobroConCreditosTest.evento("E1", 10));
        pronosticos = new PronosticoServicio(eventos, new PronosticoRepositorioMemoria(), CobroPronostico.NINGUNO, reloj);
        deAna = pronosticos.registrar(ANA, "E1", "Local");
    }

    @Test
    void elParticipanteVeLoSuyo() {
        assertEquals(1, pronosticos.pronosticosDe(ANA, "12345678").size());
    }

    @Test
    void otroParticipanteNoVeNiGestionaDatosAjenos() {
        assertThrows(AccesoDenegadoException.class, () -> pronosticos.pronosticosDe(LUIS, "12345678"));
        assertThrows(AccesoDenegadoException.class, () -> pronosticos.cambiar(LUIS, deAna.id(), "Empate"));
    }

    @Test
    void elOrganizadorNoVeDatosIndividuales() {
        assertThrows(AccesoDenegadoException.class, () -> pronosticos.pronosticosDe(ORG, "12345678"));
    }

    @Test
    void elAuditorConsultaPeroNoModifica() {
        assertEquals(1, pronosticos.pronosticosDe(AUD, "12345678").size());
        assertThrows(AccesoDenegadoException.class, () -> pronosticos.cambiar(AUD, deAna.id(), "Empate"));
    }
}
