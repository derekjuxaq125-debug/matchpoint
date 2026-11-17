package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.Pronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorio;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lógica de HU-10: registrar un pronóstico solo en un evento habilitado y con
 * una opción válida. El rechazo por evento cerrado (HU-12) sale de la misma
 * regla: {@link Evento#estaHabilitado}.
 */
public class PronosticoServicio {

    public static final String CAMPO_EVENTO = "evento";
    public static final String CAMPO_OPCION = "opcion";
    public static final String MENSAJE_EVENTO_CERRADO = "El evento ya está cerrado. No se aceptan pronósticos.";

    private final EventoRepositorio eventos;
    private final PronosticoRepositorio pronosticos;
    private final CobroPronostico cobro;
    private final Clock reloj;

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
}
