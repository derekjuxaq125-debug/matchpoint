package com.matchpoint.torneos.servicio;

import java.util.Collections;
import java.util.Map;

/** Los datos del torneo no cumplen los criterios de aceptación. */
public class ValidacionException extends RuntimeException {

    private final Map<String, String> errores;

    public ValidacionException(Map<String, String> errores) {
        super(String.join(" ", errores.values()));
        this.errores = Collections.unmodifiableMap(errores);
    }

    /** Mensaje de error por campo (claves: ver constantes de {@link TorneoServicio}). */
    public Map<String, String> getErrores() {
        return errores;
    }
}
