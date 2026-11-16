package com.matchpoint.cuentas.modelo;

/**
 * Cuenta de usuario. La contraseña nunca se guarda en claro: solo su hash
 * (SHA-256) con una sal aleatoria propia de cada cuenta.
 */
public record Usuario(String identificacion, String nombre, Rol rol, String sal, String hashClave) {
}
