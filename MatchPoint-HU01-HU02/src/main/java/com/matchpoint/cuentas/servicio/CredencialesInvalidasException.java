package com.matchpoint.cuentas.servicio;

/** Identificación o contraseña incorrectas: se niega el acceso (HU-02). */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
