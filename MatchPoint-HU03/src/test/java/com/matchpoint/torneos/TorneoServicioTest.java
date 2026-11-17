package com.matchpoint.torneos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.torneos.modelo.Deporte;
import com.matchpoint.torneos.modelo.Torneo;
import com.matchpoint.torneos.repositorio.RepositorioException;
import com.matchpoint.torneos.repositorio.TorneoRepositorio;
import com.matchpoint.torneos.repositorio.TorneoRepositorioArchivo;
import com.matchpoint.torneos.repositorio.TorneoRepositorioMemoria;
import com.matchpoint.torneos.servicio.TorneoServicio;
import com.matchpoint.torneos.servicio.ValidacionException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pruebas de los criterios de aceptación de HU-03 (Crear torneo). */
class TorneoServicioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 3);
    private static final Clock RELOJ = Clock.fixed(
            HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private TorneoRepositorioMemoria repo;
    private TorneoServicio servicio;

    @BeforeEach
    void preparar() {
        repo = new TorneoRepositorioMemoria();
        servicio = new TorneoServicio(repo, RELOJ);
    }

    private ValidacionException rechazo(String nombre, Deporte d, LocalDate ini, LocalDate fin) {
        return assertThrows(ValidacionException.class, () -> servicio.crear(nombre, d, ini, fin));
    }

    @Test
    void datosValidosGuardanYApareceEnLista() {
        Torneo t = servicio.crear("Copa MatchPoint", Deporte.FUTBOL, HOY.plusDays(5), HOY.plusDays(20));
        assertEquals("Copa MatchPoint", t.nombre());
        assertEquals(List.of(t), servicio.listar());
    }

    @Test
    void fechaDeFinAnteriorSeRechazaConMensajeExacto() {
        ValidacionException e = rechazo("Copa", Deporte.TENIS, HOY.plusDays(10), HOY.plusDays(5));
        assertEquals("La fecha de fin no puede ser anterior a la de inicio",
                e.getErrores().get(TorneoServicio.CAMPO_FIN));
        assertTrue(repo.listar().isEmpty());
    }

    @Test
    void fechasIgualesSePermiten() {
        servicio.crear("Torneo relámpago", Deporte.BALONCESTO, HOY.plusDays(2), HOY.plusDays(2));
        assertEquals(1, repo.listar().size());
    }

    @Test
    void inicioHoyEsValidoPeroAyerNo() {
        servicio.crear("Hoy", Deporte.VOLEIBOL, HOY, HOY);
        ValidacionException e = rechazo("Ayer", Deporte.VOLEIBOL, HOY.minusDays(1), HOY);
        assertTrue(e.getErrores().containsKey(TorneoServicio.CAMPO_INICIO));
        assertEquals(1, repo.listar().size());
    }

    @Test
    void nombreRepetidoSeRechazaSinDistinguirMayusculas() {
        servicio.crear("Liga Pro", Deporte.FUTBOL, HOY.plusDays(1), HOY.plusDays(9));
        ValidacionException e = rechazo("  liga pro ", Deporte.TENIS, HOY.plusDays(1), HOY.plusDays(9));
        assertTrue(e.getErrores().get(TorneoServicio.CAMPO_NOMBRE).contains("en uso"));
        assertEquals(1, repo.listar().size());
    }

    @Test
    void indicaCadaCampoObligatorioQueFalta() {
        ValidacionException e = rechazo("   ", null, null, null);
        assertEquals(4, e.getErrores().size());
        assertTrue(e.getErrores().get(TorneoServicio.CAMPO_NOMBRE).contains("Nombre"));
        assertTrue(e.getErrores().get(TorneoServicio.CAMPO_DEPORTE).contains("Deporte"));
        assertTrue(e.getErrores().get(TorneoServicio.CAMPO_INICIO).contains("inicio"));
        assertTrue(e.getErrores().get(TorneoServicio.CAMPO_FIN).contains("fin"));
        assertTrue(repo.listar().isEmpty());
    }

    @Test
    void faltaSoloUnCampo() {
        ValidacionException e = rechazo("Copa", Deporte.ESPORTS, HOY.plusDays(1), null);
        assertEquals(List.of(TorneoServicio.CAMPO_FIN), List.copyOf(e.getErrores().keySet()));
    }

    @Test
    void errorAlGuardarNoDejaNingunTorneo() {
        TorneoRepositorio roto = new TorneoRepositorioMemoria() {
            @Override
            public void guardar(Torneo torneo) {
                throw new RepositorioException("falla simulada", null);
            }
        };
        TorneoServicio s = new TorneoServicio(roto, RELOJ);
        assertThrows(RepositorioException.class,
                () -> s.crear("Copa", Deporte.FUTBOL, HOY.plusDays(1), HOY.plusDays(2)));
        assertTrue(s.listar().isEmpty());
    }

    @Test
    void repositorioArchivoPersisteEntreInstancias(@TempDir Path dir) {
        Path archivo = dir.resolve("datos").resolve("torneos.txt");
        new TorneoServicio(new TorneoRepositorioArchivo(archivo), RELOJ)
                .crear("Copa; Ñandú & Co", Deporte.BEISBOL, HOY.plusDays(1), HOY.plusDays(3));

        TorneoRepositorioArchivo otro = new TorneoRepositorioArchivo(archivo);
        assertEquals(1, otro.listar().size());
        assertEquals("Copa; Ñandú & Co", otro.listar().get(0).nombre());
        assertTrue(otro.existePorNombre("copa; ñandú & co"));
        assertFalse(otro.existePorNombre("otro"));
    }
}
