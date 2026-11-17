package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.Pronostico;
import com.matchpoint.cuentas.modelo.ResultadoPronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorio;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lógica de HU-10: registrar un pronóstico solo en un evento habilitado y con
 * una opción válida. HU-12: después del cierre se rechazan pronósticos nuevos y
 * cambios; antes del cierre el participante puede cambiar su opción (sin un
 * nuevo cobro). Ambas reglas salen de {@link Evento#estaHabilitado}.
 */
public class PronosticoServicio {

    public static final String CAMPO_EVENTO = "evento";
    public static final String CAMPO_OPCION = "opcion";
    public static final String MENSAJE_EVENTO_CERRADO = "El evento ya está cerrado. No se aceptan pronósticos.";
    public static final String MENSAJE_CAMBIO_CERRADO =
            "El evento ya está cerrado. No se aceptan cambios en el pronóstico.";

    private final EventoRepositorio eventos;
    private final PronosticoRepositorio pronosticos;
    private final CobroPronostico cobro;
    private final Clock reloj;
    /** HU-14: clave de solicitud → id del pronóstico que generó. */
    private final Map<String, String> solicitudesProcesadas = new ConcurrentHashMap<>();

    public PronosticoServicio(EventoRepositorio eventos, PronosticoRepositorio pronosticos) {
        this(eventos, pronosticos, CobroPronostico.NINGUNO, Clock.systemDefaultZone());
    }

    public PronosticoServicio(EventoRepositorio eventos, PronosticoRepositorio pronosticos,
                              CobroPronostico cobro, Clock reloj) {
        this.eventos = eventos;
        this.pronosticos = pronosticos;
        this.cobro = cobro;
        this.reloj = reloj;
    }

    /**
     * Registra el pronóstico de un participante.
     *
     * @throws AccesoDenegadoException si el usuario no es participante
     * @throws ValidacionException si falta un dato, el evento no existe o está cerrado,
     *         la opción no es válida o ya había pronosticado en ese evento (no se guarda nada)
     * @throws com.matchpoint.cuentas.repositorio.RepositorioException si falla el guardado
     */
    public synchronized Pronostico registrar(Usuario actor, String eventoId, String opcion) {
        return registrar(actor, eventoId, opcion, null).pronostico();
    }

    /**
     * HU-14: igual que {@link #registrar(Usuario, String, String)}, pero con una
     * clave de solicitud que el cliente genera una vez y reutiliza en los
     * reintentos. Si la clave ya se procesó, devuelve el pronóstico original sin
     * volver a cobrar.
     */
    public synchronized ResultadoPronostico registrar(Usuario actor, String eventoId, String opcion,
                                                      String claveSolicitud) {
        String clave = claveSolicitud == null || claveSolicitud.isBlank() ? null : claveSolicitud.trim();
        if (clave != null && solicitudesProcesadas.containsKey(clave)) {
            String id = solicitudesProcesadas.get(clave);
            Optional<Pronostico> original = pronosticos.listar().stream().filter(p -> p.id().equals(id)).findFirst();
            if (original.isPresent() && actor != null
                    && original.get().usuarioId().equalsIgnoreCase(actor.identificacion())) {
                return new ResultadoPronostico(original.get(), true);
            }
        }
        Pronostico nuevo = registrarNuevo(actor, eventoId, opcion);
        if (clave != null) {
            solicitudesProcesadas.put(clave, nuevo.id());
        }
        return new ResultadoPronostico(nuevo, false);
    }

    private Pronostico registrarNuevo(Usuario actor, String eventoId, String opcion) {
        if (actor == null || actor.rol() != Rol.PARTICIPANTE) {
            throw new AccesoDenegadoException("Solo un participante puede registrar pronósticos.");
        }

        Map<String, String> errores = new LinkedHashMap<>();
        String eventoLimpio = eventoId == null ? "" : eventoId.trim();
        String opcionLimpia = opcion == null ? "" : opcion.trim();
        if (eventoLimpio.isEmpty()) {
            errores.put(CAMPO_EVENTO, "Falta el campo obligatorio: Evento.");
        }
        if (opcionLimpia.isEmpty()) {
            errores.put(CAMPO_OPCION, "Falta el campo obligatorio: Opción de pronóstico.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        Optional<Evento> encontrado = eventos.buscarPorId(eventoLimpio);
        if (encontrado.isEmpty()) {
            errores.put(CAMPO_EVENTO, "El evento seleccionado no existe.");
            throw new ValidacionException(errores);
        }
        Evento evento = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!evento.estaHabilitado(ahora)) {
            errores.put(CAMPO_EVENTO, MENSAJE_EVENTO_CERRADO);
            throw new ValidacionException(errores);
        }
        Optional<String> opcionValida = evento.opciones().stream()
                .filter(o -> o.equalsIgnoreCase(opcionLimpia)).findFirst();
        if (opcionValida.isEmpty()) {
            errores.put(CAMPO_OPCION, "La opción elegida no es válida para este evento.");
            throw new ValidacionException(errores);
        }
        if (pronosticos.buscarPorUsuarioYEvento(actor.identificacion(), evento.id()).isPresent()) {
            errores.put(CAMPO_EVENTO, "Ya registró un pronóstico en este evento.");
            throw new ValidacionException(errores);
        }

        Pronostico pronostico = new Pronostico(UUID.randomUUID().toString(), actor.identificacion(),
                evento.id(), opcionValida.get(), ahora);
        cobro.cobrar(actor, evento, pronostico.id()); // HU-11: si falla (saldo insuficiente), no se guarda
        try {
            pronosticos.guardar(pronostico);
        } catch (RuntimeException e) {
            cobro.revertir(actor, evento, pronostico.id()); // operación fallida: se devuelven los créditos
            throw e;
        }
        return pronostico;
    }

    /**
     * HU-12: cambia la opción de un pronóstico propio mientras el evento siga abierto.
     * El cambio no vuelve a cobrar: la participación ya está pagada.
     *
     * @throws AccesoDenegadoException si no es participante o el pronóstico es de otro usuario
     * @throws ValidacionException si el evento ya cerró o la opción no es válida
     */
    public synchronized Pronostico cambiar(Usuario actor, String pronosticoId, String nuevaOpcion) {
        if (actor == null || actor.rol() != Rol.PARTICIPANTE) {
            throw new AccesoDenegadoException("Solo un participante puede cambiar sus pronósticos.");
        }
        Pronostico actual = pronosticos.buscarPorId(pronosticoId == null ? "" : pronosticoId)
                .orElseThrow(() -> new ValidacionException(Map.of(CAMPO_EVENTO, "El pronóstico no existe.")));
        AccesoDatos.exigirGestion(actor, actual.usuarioId()); // HU-26
        Evento evento = eventos.buscarPorId(actual.eventoId()).orElseThrow(() ->
                new ValidacionException(Map.of(CAMPO_EVENTO, "El evento seleccionado no existe.")));
        if (!evento.estaHabilitado(LocalDateTime.now(reloj))) {
            throw new ValidacionException(Map.of(CAMPO_EVENTO, MENSAJE_CAMBIO_CERRADO));
        }
        String limpia = nuevaOpcion == null ? "" : nuevaOpcion.trim();
        if (limpia.isEmpty()) {
            throw new ValidacionException(Map.of(CAMPO_OPCION, "Falta el campo obligatorio: Opción de pronóstico."));
        }
        String valida = evento.opciones().stream().filter(o -> o.equalsIgnoreCase(limpia)).findFirst()
                .orElseThrow(() -> new ValidacionException(Map.of(CAMPO_OPCION,
                        "La opción elegida no es válida para este evento.")));
        Pronostico cambiado = new Pronostico(actual.id(), actual.usuarioId(), actual.eventoId(), valida,
                actual.registrado());
        pronosticos.actualizar(cambiado);
        return cambiado;
    }

    /** Pronósticos del propio participante (HU-12 y, en el Sprint 3, HU-13). */
    public List<Pronostico> misPronosticos(Usuario actor) {
        if (actor == null) {
            throw new AccesoDenegadoException("Debe iniciar sesión.");
        }
        return pronosticos.listarPorUsuario(actor.identificacion());
    }

    /** HU-26: pronósticos de un usuario; solo el propio participante o un auditor. */
    public List<Pronostico> pronosticosDe(Usuario actor, String usuarioId) {
        AccesoDatos.exigirLectura(actor, usuarioId);
        return pronosticos.listarPorUsuario(usuarioId.trim());
    }
}
