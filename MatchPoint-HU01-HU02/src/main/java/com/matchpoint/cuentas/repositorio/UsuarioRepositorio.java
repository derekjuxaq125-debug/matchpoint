package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Usuario;
import java.util.List;
import java.util.Optional;

/** Acceso a los usuarios almacenados. */
public interface UsuarioRepositorio {

    /** Devuelve todos los usuarios, en orden de creación. */
    List<Usuario> listar();

    /** Busca un usuario por su identificación (sin distinguir mayúsculas). */
    Optional<Usuario> buscarPorIdentificacion(String identificacion);

    /**
     * Guarda el usuario. Si falla, no debe quedar ninguna cuenta a medias.
     *
     * @throws RepositorioException si no se pudo guardar
     */
    void guardar(Usuario usuario);
}
