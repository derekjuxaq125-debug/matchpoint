package com.matchpoint.cuentas.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Utilidad para proteger contraseñas: SHA-256 con sal aleatoria. */
final class ClaveHash {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private ClaveHash() {
    }

    static String generarSal() {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sal);
    }

    static String hash(String clave, String sal) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] resultado = md.digest((sal + clave).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resultado);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /** Comparación en tiempo constante. */
    static boolean coincide(String clave, String sal, String hashEsperado) {
        return MessageDigest.isEqual(
                hash(clave, sal).getBytes(StandardCharsets.UTF_8),
                hashEsperado.getBytes(StandardCharsets.UTF_8));
    }
}
