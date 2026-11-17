package com.matchpoint.torneos.servicio;

import com.matchpoint.torneos.modelo.Deporte;
import com.matchpoint.torneos.modelo.Torneo;
import com.matchpoint.torneos.repositorio.TorneoRepositorio;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lógica de HU-03: crear torneo con sus validaciones. */
public class TorneoServicio {

    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_DEPORTE = "deporte";
    public static final String CAMPO_INICIO = "fechaInicio";
    public static final String CAMPO_FIN = "fechaFin";

    private final TorneoRepositorio repositorio;
    private final Clock reloj;

    public TorneoServicio(TorneoRepositorio repositorio) {
        this(repositorio, Clock.systemDefaultZone());
    }

    public TorneoServicio(TorneoRepositorio repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /**
     * Crea y guarda un torneo.
     *
     * @throws ValidacionException si algún dato incumple los criterios (no se guarda nada)
     * @throws com.matchpoint.torneos.repositorio.RepositorioException si falla el guardado
     */
    public Torneo crear(String nombre, Deporte deporte, LocalDate inicio, LocalDate fin) {
        Map<String, String> errores = new LinkedHashMap<>();
        String nombreLimpio = nombre == null ? "" : nombre.trim();

        // 1. Campos obligatorios: se indica cuál falta
        if (nombreLimpio.isEmpty()) {
            errores.put(CAMPO_NOMBRE, "Falta el campo obligatorio: Nombre.");
        }
        if (deporte == null) {
            errores.put(CAMPO_DEPORTE, "Falta el campo obligatorio: Deporte.");
        }
        if (inicio == null) {
            errores.put(CAMPO_INICIO, "Falta el campo obligatorio: Fecha de inicio.");
        }
        if (fin == null) {
            errores.put(CAMPO_FIN, "Falta el campo obligatorio: Fecha de fin.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // 2. Reglas de fechas y nombre único
        if (fin.isBefore(inicio)) {
            errores.put(CAMPO_FIN, "La fecha de fin no puede ser anterior a la de inicio");
        }
        if (inicio.isBefore(LocalDate.now(reloj))) {
            errores.put(CAMPO_INICIO, "La fecha de inicio no puede ser anterior a la fecha actual.");
        }
        if (repositorio.existePorNombre(nombreLimpio)) {
            errores.put(CAMPO_NOMBRE, "El nombre del torneo ya está en uso.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        Torneo torneo = new Torneo(nombreLimpio, deporte, inicio, fin);
        repositorio.guardar(torneo); // si falla lanza RepositorioException y no se crea nada
        return torneo;
    }

    /** Torneos para mostrar en la lista. */
    public List<Torneo> listar() {
        return repositorio.listar();
    }
}
