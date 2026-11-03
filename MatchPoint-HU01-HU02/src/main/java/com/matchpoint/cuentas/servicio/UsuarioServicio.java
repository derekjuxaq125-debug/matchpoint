package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorio;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lógica de HU-01: registrar usuario con identificación única. */
public class UsuarioServicio {

    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_IDENTIFICACION = "identificacion";
    public static final String CAMPO_CLAVE = "clave";
    public static final String CAMPO_CONFIRMACION = "confirmacion";

    static final int CLAVE_MIN = 6;
    static final String PATRON_IDENTIFICACION = "[A-Za-z0-9]{5,15}";

    private final UsuarioRepositorio repositorio;

    public UsuarioServicio(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Registra un usuario nuevo con rol PARTICIPANTE.
     *
     * @throws ValidacionException si algún dato incumple los criterios (no se guarda nada)
     * @throws com.matchpoint.cuentas.repositorio.RepositorioException si falla el guardado
     */
    public Usuario registrar(String nombre, String identificacion, String clave, String confirmacion) {
        Map<String, String> errores = new LinkedHashMap<>();
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String idLimpia = identificacion == null ? "" : identificacion.trim();
        String claveSegura = clave == null ? "" : clave;
        String confirmacionSegura = confirmacion == null ? "" : confirmacion;

        // 1. Campos obligatorios: se indica cuál falta
        if (nombreLimpio.isEmpty()) {
            errores.put(CAMPO_NOMBRE, "Falta el campo obligatorio: Nombre.");
        }
        if (idLimpia.isEmpty()) {
            errores.put(CAMPO_IDENTIFICACION, "Falta el campo obligatorio: Identificación.");
        }
        if (claveSegura.isEmpty()) {
            errores.put(CAMPO_CLAVE, "Falta el campo obligatorio: Contraseña.");
        }
        if (confirmacionSegura.isEmpty()) {
            errores.put(CAMPO_CONFIRMACION, "Falta el campo obligatorio: Confirmar contraseña.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // 2. Formato y coherencia
        if (!idLimpia.matches(PATRON_IDENTIFICACION)) {
            errores.put(CAMPO_IDENTIFICACION,
                    "La identificación debe tener entre 5 y 15 caracteres, solo letras y números.");
        }
        if (claveSegura.length() < CLAVE_MIN) {
            errores.put(CAMPO_CLAVE, "La contraseña debe tener al menos " + CLAVE_MIN + " caracteres.");
        }
        if (!claveSegura.equals(confirmacionSegura)) {
            errores.put(CAMPO_CONFIRMACION, "Las contraseñas no coinciden.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // 3. Identificación única
        if (repositorio.buscarPorIdentificacion(idLimpia).isPresent()) {
            errores.put(CAMPO_IDENTIFICACION,
                    "La identificación ya está registrada. Use otra o inicie sesión.");
            throw new ValidacionException(errores);
        }

        return crear(nombreLimpio, idLimpia, claveSegura, Rol.PARTICIPANTE);
    }

    /**
     * Crea las cuentas de organizador y auditor la primera vez que se ejecuta el
     * programa (el registro público solo crea participantes).
     */
    public void sembrarCuentasIniciales() {
        if (!repositorio.listar().isEmpty()) {
            return;
        }
        crear("Organizador MatchPoint", "ORG0001", "organizador123", Rol.ORGANIZADOR);
        crear("Auditor MatchPoint", "AUD0001", "auditor123", Rol.AUDITOR);
    }

    private Usuario crear(String nombre, String identificacion, String clave, Rol rol) {
        String sal = ClaveHash.generarSal();
        Usuario usuario = new Usuario(identificacion, nombre, rol, sal, ClaveHash.hash(clave, sal));
        repositorio.guardar(usuario); // si falla lanza RepositorioException y no se crea nada
        return usuario;
    }
}
