package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.TorneoRepositorio;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HU-03: crear torneo con nombre único, deporte y fechas válidas.
 * Solo un organizador crea torneos.
 */
public class TorneoServicio {

    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_DEPORTE = "deporte";
    public static final String CAMPO_INICIO = "inicio";
    public static final String CAMPO_FIN = "fin";
    public static final int NOMBRE_MAX = 60;
    public static final String PATRON_FECHA = "yyyy-MM-dd";
    public static final String MENSAJE_FIN_ANTERIOR = "La fecha de fin no puede ser anterior a la de inicio.";
    public static final List<String> DEPORTES = List.of(
            "Fútbol", "Baloncesto", "Tenis", "Voleibol", "Atletismo", "Ciclismo", "Automovilismo", "Otro");

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

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
     * @throws AccesoDenegadoException si el usuario no es organizador
     * @throws ValidacionException si falta un campo, las fechas no son válidas o el nombre ya existe
     * @throws com.matchpoint.cuentas.repositorio.RepositorioException si falla el guardado (no se crea nada)
     */
    public synchronized Torneo crear(Usuario actor, String nombre, String deporte, String inicio, String fin) {
        if (actor == null || actor.rol() != Rol.ORGANIZADOR) {
            throw new AccesoDenegadoException("Solo un organizador puede crear torneos.");
        }
        String n = limpiar(nombre);
        String d = limpiar(deporte);
        String i = limpiar(inicio);
        String f = limpiar(fin);

        Map<String, String> errores = new LinkedHashMap<>();
        if (n.isEmpty()) {
            errores.put(CAMPO_NOMBRE, "Falta el campo obligatorio: Nombre.");
        }
        if (d.isEmpty()) {
            errores.put(CAMPO_DEPORTE, "Falta el campo obligatorio: Deporte.");
        }
        if (i.isEmpty()) {
            errores.put(CAMPO_INICIO, "Falta el campo obligatorio: Fecha de inicio.");
        }
        if (f.isEmpty()) {
            errores.put(CAMPO_FIN, "Falta el campo obligatorio: Fecha de fin.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        if (n.length() > NOMBRE_MAX) {
            errores.put(CAMPO_NOMBRE, "El nombre admite máximo " + NOMBRE_MAX + " caracteres.");
        }
        if (DEPORTES.stream().noneMatch(x -> x.equalsIgnoreCase(d))) {
            errores.put(CAMPO_DEPORTE, "Seleccione un deporte de la lista.");
        }
        LocalDate fechaInicio = leer(i);
        LocalDate fechaFin = leer(f);
        if (fechaInicio == null) {
            errores.put(CAMPO_INICIO, "La fecha de inicio debe tener el formato " + PATRON_FECHA + ".");
        }
        if (fechaFin == null) {
            errores.put(CAMPO_FIN, "La fecha de fin debe tener el formato " + PATRON_FECHA + ".");
        }
        if (fechaInicio != null && fechaInicio.isBefore(LocalDate.now(reloj))) {
            errores.put(CAMPO_INICIO, "La fecha de inicio no puede ser anterior a la fecha actual.");
        }
        if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            errores.put(CAMPO_FIN, MENSAJE_FIN_ANTERIOR);
        }
        if (errores.isEmpty() && repositorio.buscarPorNombre(n).isPresent()) {
            errores.put(CAMPO_NOMBRE, "El nombre \"" + n + "\" ya está en uso por otro torneo.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        String deporteOficial = DEPORTES.stream().filter(x -> x.equalsIgnoreCase(d)).findFirst().orElse(d);
        Torneo torneo = new Torneo(UUID.randomUUID().toString(), n, deporteOficial, fechaInicio, fechaFin);
        repositorio.guardar(torneo);
        return torneo;
    }

    /** Lista de torneos ordenada por fecha de inicio. */
    public List<Torneo> listar() {
        return repositorio.listar().stream()
                .sorted(Comparator.comparing(Torneo::fechaInicio).thenComparing(Torneo::nombre))
                .toList();
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private static LocalDate leer(String texto) {
        try {
            return LocalDate.parse(texto, FORMATO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
