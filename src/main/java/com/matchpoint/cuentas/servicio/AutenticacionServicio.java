package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorio;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Lógica de HU-02: iniciar sesión y reconocer el rol del usuario. */
public class AutenticacionServicio {

    public static final String CAMPO_IDENTIFICACION = "identificacion";
    public static final String CAMPO_CLAVE = "clave";
    public static final String MENSAJE_ACCESO_NEGADO = "Identificación o contraseña incorrectas.";

    private final UsuarioRepositorio repositorio;

    public AutenticacionServicio(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Valida las credenciales y devuelve el usuario con su rol.
     *
     * @throws ValidacionException si falta la identificación o la contraseña
     * @throws CredencialesInvalidasException si las credenciales no son válidas (acceso negado)
     */
    public Usuario iniciarSesion(String identificacion, String clave) {
        Map<String, String> errores = new LinkedHashMap<>();
        String idLimpia = identificacion == null ? "" : identificacion.trim();
        String claveSegura = clave == null ? "" : clave;

        if (idLimpia.isEmpty()) {
            errores.put(CAMPO_IDENTIFICACION, "Falta el campo obligatorio: Identificación.");
        }
        if (claveSegura.isEmpty()) {
            errores.put(CAMPO_CLAVE, "Falta el campo obligatorio: Contraseña.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // Mismo mensaje si no existe el usuario o si la clave es incorrecta (no revela cuál falló)
        Optional<Usuario> encontrado = repositorio.buscarPorIdentificacion(idLimpia);
        if (encontrado.isEmpty()
                || !ClaveHash.coincide(claveSegura, encontrado.get().sal(), encontrado.get().hashClave())) {
            throw new CredencialesInvalidasException(MENSAJE_ACCESO_NEGADO);
        }
        return encontrado.get();
    }
}
