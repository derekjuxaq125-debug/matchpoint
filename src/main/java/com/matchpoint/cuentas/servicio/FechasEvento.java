package com.matchpoint.cuentas.servicio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Formato único de fecha y hora que se escribe en los formularios. */
public final class FechasEvento {

    public static final String PATRON = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private FechasEvento() {
    }

    /** @return la fecha y hora, o {@code null} si el texto no cumple el formato */
    public static LocalDateTime leer(String texto) {
        try {
            return LocalDateTime.parse(texto.trim(), FORMATO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String escribir(LocalDateTime fecha) {
        return fecha.format(FORMATO);
    }
}
