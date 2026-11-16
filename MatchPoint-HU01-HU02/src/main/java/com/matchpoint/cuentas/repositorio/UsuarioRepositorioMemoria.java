package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Usuario;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria (útil para pruebas). */
public class UsuarioRepositorioMemoria implements UsuarioRepositorio {

    private final List<Usuario> usuarios = new ArrayList<>();

    @Override
    public List<Usuario> listar() {
        return List.copyOf(usuarios);
    }

    @Override
    public Optional<Usuario> buscarPorIdentificacion(String identificacion) {
        return usuarios.stream()
                .filter(u -> u.identificacion().equalsIgnoreCase(identificacion))
                .findFirst();
    }

    @Override
    public void guardar(Usuario usuario) {
        usuarios.add(usuario);
    }
}
