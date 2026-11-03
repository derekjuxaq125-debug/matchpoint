package com.matchpoint.cuentas.repositorio;

/** Error al leer o guardar usuarios. */
public class RepositorioException extends RuntimeException {

    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
