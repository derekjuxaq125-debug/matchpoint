package com.matchpoint.cuentas.servicio;

import java.util.Collections;
import java.util.Map;

/** Los datos ingresados no cumplen los criterios de aceptación. */
public class ValidacionException extends RuntimeException {

    private final Map<String, String> errores;

    public ValidacionException(Map<String, String> errores) {
        super(String.join(" ", errores.values()));
        this.errores = Collections.unmodifiableMap(errores);
    }

    /** Mensaje de error por campo (claves: ver constantes de {@link UsuarioServicio}). */
    public Map<String, String> getErrores() {
        return errores;
    }
}
