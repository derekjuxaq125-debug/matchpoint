package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorio;
import com.matchpoint.cuentas.repositorio.TorneoRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Lógica de HU-04: registrar evento con fecha, modalidad, opciones válidas y hora de cierre. */
public class EventoServicio {

    public static final String CAMPO_TORNEO = "torneo";
    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_INICIO = "inicio";
    public static final String CAMPO_MODALIDAD = "modalidad";
    public static final String CAMPO_OPCIONES = "opciones";
    public static final String CAMPO_CIERRE = "cierre";
    public static final String CAMPO_COSTO = "costo";

    /** Propuestas pendientes de cierre con el equipo (ver docs/DEFINICIONES.md). */
    static final int NOMBRE_MAX = 60;
    static final int OPCION_MAX = 40;

    private final EventoRepositorio eventos;
    private final TorneoRepositorio torneos;
    private final Clock reloj;

    public EventoServicio(EventoRepositorio eventos, TorneoRepositorio torneos) {
        this(eventos, torneos, Clock.systemDefaultZone());
    }

    public EventoServicio(EventoRepositorio eventos, TorneoRepositorio torneos, Clock reloj) {
        this.eventos = eventos;
        this.torneos = torneos;
        this.reloj = reloj;
    }

    /**
     * Registra un evento nuevo. Solo lo puede hacer un ORGANIZADOR.
     *
     * @param torneoId  id del torneo al que pertenece
     * @param inicio    fecha y hora del evento, formato {@link FechasEvento#PATRON}
     * @param opciones  opciones de pronóstico (se ignoran las vacías)
     * @param cierre    hora límite para pronosticar, mismo formato
     * @param costo     créditos que cuesta cada pronóstico (entero mayor que cero)
     * @throws AccesoDenegadoException si el usuario no es organizador
     * @throws ValidacionException si algún dato incumple los criterios (no se guarda nada)
     * @throws com.matchpoint.cuentas.repositorio.RepositorioException si falla el guardado
     */
    public synchronized Evento registrar(Usuario actor, String torneoId, String nombre, String inicio,
                                         ModalidadPronostico modalidad, List<String> opciones,
                                         String cierre, String costo) {
        if (actor == null || actor.rol() != Rol.ORGANIZADOR) {
            throw new AccesoDenegadoException("Solo un organizador puede registrar eventos.");
        }

        Map<String, String> errores = new LinkedHashMap<>();
        String torneoLimpio = vacioSiNulo(torneoId).trim();
        String nombreLimpio = vacioSiNulo(nombre).trim();
        String inicioLimpio = vacioSiNulo(inicio).trim();
        String cierreLimpio = vacioSiNulo(cierre).trim();
        String costoLimpio = vacioSiNulo(costo).trim();
        List<String> opcionesLimpias = limpiar(opciones);

        // 1. Campos obligatorios: se indica cuál falta
        if (torneoLimpio.isEmpty()) {
            errores.put(CAMPO_TORNEO, "Falta el campo obligatorio: Torneo.");
        }
        if (nombreLimpio.isEmpty()) {
            errores.put(CAMPO_NOMBRE, "Falta el campo obligatorio: Nombre del evento.");
        }
        if (inicioLimpio.isEmpty()) {
            errores.put(CAMPO_INICIO, "Falta el campo obligatorio: Fecha y hora del evento.");
        }
        if (modalidad == null) {
            errores.put(CAMPO_MODALIDAD, "Falta el campo obligatorio: Modalidad de pronóstico.");
        }
        if (opcionesLimpias.isEmpty()) {
            errores.put(CAMPO_OPCIONES, "Falta el campo obligatorio: Opciones de pronóstico.");
        }
        if (cierreLimpio.isEmpty()) {
            errores.put(CAMPO_CIERRE, "Falta el campo obligatorio: Hora de cierre.");
        }
        if (costoLimpio.isEmpty()) {
            errores.put(CAMPO_COSTO, "Falta el campo obligatorio: Costo en créditos.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // 2. Formato y reglas de cada campo
        Optional<Torneo> torneo = torneos.buscarPorId(torneoLimpio);
        if (torneo.isEmpty()) {
            errores.put(CAMPO_TORNEO, "El torneo seleccionado no existe.");
        }
        if (nombreLimpio.length() > NOMBRE_MAX) {
            errores.put(CAMPO_NOMBRE, "El nombre del evento no puede superar los " + NOMBRE_MAX + " caracteres.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime inicioFecha = FechasEvento.leer(inicioLimpio);
        LocalDateTime cierreFecha = FechasEvento.leer(cierreLimpio);
        if (inicioFecha == null) {
            errores.put(CAMPO_INICIO, "La fecha y hora del evento deben tener el formato " + FechasEvento.PATRON + ".");
        } else if (!inicioFecha.isAfter(ahora)) {
            errores.put(CAMPO_INICIO, "La fecha del evento debe ser futura.");
        } else if (torneo.isPresent()
                && (inicioFecha.toLocalDate().isBefore(torneo.get().fechaInicio())
                    || inicioFecha.toLocalDate().isAfter(torneo.get().fechaFin()))) {
            errores.put(CAMPO_INICIO, "La fecha del evento debe estar dentro de las fechas del torneo ("
                    + torneo.get().fechaInicio() + " a " + torneo.get().fechaFin() + ").");
        }
        if (cierreFecha == null) {
            errores.put(CAMPO_CIERRE, "La hora de cierre debe tener el formato " + FechasEvento.PATRON + ".");
        } else if (!cierreFecha.isAfter(ahora)) {
            errores.put(CAMPO_CIERRE, "La hora de cierre debe ser futura.");
        } else if (inicioFecha != null && cierreFecha.isAfter(inicioFecha)) {
            errores.put(CAMPO_CIERRE, "La hora de cierre no puede ser posterior al inicio del evento.");
        }
        validarOpciones(modalidad, opcionesLimpias, errores);
        int costoCreditos = leerCosto(costoLimpio, errores);
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // 3. Sin duplicados (mismo torneo, nombre y fecha de inicio)
        boolean repetido = eventos.listarPorTorneo(torneoLimpio).stream()
                .anyMatch(e -> e.nombre().equalsIgnoreCase(nombreLimpio) && e.inicio().equals(inicioFecha));
        if (repetido) {
            errores.put(CAMPO_NOMBRE, "Ya existe un evento con ese nombre y fecha en el torneo.");
            throw new ValidacionException(errores);
        }

        Evento evento = new Evento(UUID.randomUUID().toString(), torneoLimpio, nombreLimpio, inicioFecha,
                modalidad, opcionesLimpias, cierreFecha, costoCreditos);
        eventos.guardar(evento); // si falla lanza RepositorioException y no se crea nada
        return evento;
    }

    private static void validarOpciones(ModalidadPronostico modalidad, List<String> opciones,
                                        Map<String, String> errores) {
        if (modalidad == null) {
            return;
        }
        String error = modalidad.validarCantidad(opciones);
        if (error == null) {
            for (int i = 0; i < opciones.size() && error == null; i++) {
                String opcion = opciones.get(i);
                if (opcion.length() > OPCION_MAX) {
                    error = "Cada opción puede tener máximo " + OPCION_MAX + " caracteres.";
                }
                for (int j = 0; j < i && error == null; j++) {
                    if (opciones.get(j).equalsIgnoreCase(opcion)) {
                        error = "Las opciones no pueden repetirse: \"" + opcion + "\".";
                    }
                }
            }
        }
        if (error != null) {
            errores.put(CAMPO_OPCIONES, error);
        }
    }

    private static int leerCosto(String texto, Map<String, String> errores) {
        try {
            int valor = Integer.parseInt(texto);
            if (valor > 0) {
                return valor;
            }
        } catch (NumberFormatException ignorada) {
            // se informa abajo
        }
        errores.put(CAMPO_COSTO, "El costo debe ser un número entero mayor que cero.");
        return 0;
    }

    private static List<String> limpiar(List<String> opciones) {
        List<String> resultado = new ArrayList<>();
        if (opciones != null) {
            for (String o : opciones) {
                if (o != null && !o.isBlank()) {
                    resultado.add(o.trim());
                }
            }
        }
        return resultado;
    }

    private static String vacioSiNulo(String s) {
        return s == null ? "" : s;
    }
}
