package com.matchpoint.cuentas.servicio;

/** El rol del usuario no puede realizar la operación (HU-02: cada rol solo accede a lo suyo). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
