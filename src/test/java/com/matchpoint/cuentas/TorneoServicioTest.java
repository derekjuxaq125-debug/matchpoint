package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.repositorio.TorneoRepositorioMemoria;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.TorneoServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-03 (casos P-03-01 a P-03-07). "Hoy" es 2026-11-16. */
class TorneoServicioTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    static final Usuario ORG = new Usuario("ORG0001", "Org", Rol.ORGANIZADOR, "s", "h");
    static final Usuario ANA = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");

    private TorneoRepositorioMemoria repo;
    private TorneoServicio servicio;

    @BeforeEach
    void preparar() {
        repo = new TorneoRepositorioMemoria();
        servicio = new TorneoServicio(repo, RELOJ);
    }

    @Test
    void p0301CreaConDatosValidosYApareceEnLaLista() {
        Torneo t = servicio.crear(ORG, "  Copa UMB 2026 ", "fútbol", "2026-11-20", "2026-12-15");
        assertEquals("Copa UMB 2026", t.nombre());
        assertEquals("Fútbol", t.deporte());
        assertEquals(LocalDate.of(2026, 12, 15), t.fechaFin());
        assertEquals(1, servicio.listar().size());
    }

    @Test
    void p0302RechazaFinAnteriorAlInicio() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.crear(ORG, "Copa", "Tenis", "2026-12-10", "2026-12-01"));
        assertEquals(TorneoServicio.MENSAJE_FIN_ANTERIOR, ex.getErrores().get(TorneoServicio.CAMPO_FIN));
        assertTrue(repo.listar().isEmpty());
    }

    @Test
    void p0303PermiteInicioIgualAFin() {
        servicio.crear(ORG, "Relámpago", "Atletismo", "2026-11-16", "2026-11-16");
    }

    @Test
    void p0304RechazaInicioAnteriorAHoy() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.crear(ORG, "Copa", "Tenis", "2026-11-15", "2026-11-30"));
        assertTrue(ex.getErrores().containsKey(TorneoServicio.CAMPO_INICIO));
    }

    @Test
    void p0305RechazaNombreRepetidoSinDistinguirMayusculas() {
        servicio.crear(ORG, "Liga Andina", "Fútbol", "2026-11-20", "2026-11-30");
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.crear(ORG, " liga andina ", "Tenis", "2026-12-01", "2026-12-02"));
        assertTrue(ex.getMessage().contains("ya está en uso"));
        assertEquals(1, repo.listar().size());
    }

    @Test
    void p0306IndicaCadaCampoFaltante() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.crear(ORG, "", null, " ", ""));
        assertEquals(4, ex.getErrores().size());
    }

    @Test
    void rechazaFormatoDeFechaYDeporteFueraDeLista() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.crear(ORG, "Copa", "Ajedrez", "20/11/2026", "2026-02-30"));
        assertTrue(ex.getErrores().containsKey(TorneoServicio.CAMPO_DEPORTE));
        assertTrue(ex.getErrores().containsKey(TorneoServicio.CAMPO_INICIO));
        assertTrue(ex.getErrores().containsKey(TorneoServicio.CAMPO_FIN));
    }

    @Test
    void soloElOrganizadorCreaTorneos() {
        assertThrows(AccesoDenegadoException.class,
                () -> servicio.crear(ANA, "Copa", "Tenis", "2026-11-20", "2026-11-30"));
    }

    @Test
    void siFallaElGuardadoNoSeCreaNada() {
        TorneoServicio conFallo = new TorneoServicio(new TorneoRepositorioMemoria() {
            @Override
            public void guardar(Torneo t) {
                throw new RepositorioException("No se pudo guardar el torneo.", null);
            }
        }, RELOJ);
        assertThrows(RepositorioException.class,
                () -> conFallo.crear(ORG, "Copa", "Tenis", "2026-11-20", "2026-11-30"));
    }
}
