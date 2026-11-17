package com.matchpoint.torneos.repositorio;

/** Error al leer o guardar torneos. */
public class RepositorioException extends RuntimeException {

    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
