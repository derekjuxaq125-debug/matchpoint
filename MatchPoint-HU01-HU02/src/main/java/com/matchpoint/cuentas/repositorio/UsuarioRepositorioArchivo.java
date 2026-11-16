package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Guarda los usuarios en un archivo de texto (una línea por usuario) para que
 * persistan entre ejecuciones.
 * Formato: identificacion(cod);nombre(cod);ROL;sal;hash
 */
public class UsuarioRepositorioArchivo implements UsuarioRepositorio {

    private final Path archivo;

    public UsuarioRepositorioArchivo(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public List<Usuario> listar() {
        if (!Files.exists(archivo)) {
            return List.of();
        }
        try {
            List<Usuario> resultado = new ArrayList<>();
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] p = linea.split(";");
                resultado.add(new Usuario(
                        URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(p[1], StandardCharsets.UTF_8),
                        Rol.valueOf(p[2]),
                        p[3],
                        p[4]));
            }
            return resultado;
        } catch (IOException | RuntimeException e) {
            throw new RepositorioException("No se pudo leer el archivo de usuarios.", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorIdentificacion(String identificacion) {
        return listar().stream()
                .filter(u -> u.identificacion().equalsIgnoreCase(identificacion))
                .findFirst();
    }

    @Override
    public void guardar(Usuario usuario) {
        String linea = URLEncoder.encode(usuario.identificacion(), StandardCharsets.UTF_8) + ";"
                + URLEncoder.encode(usuario.nombre(), StandardCharsets.UTF_8) + ";"
                + usuario.rol().name() + ";"
                + usuario.sal() + ";"
                + usuario.hashClave() + System.lineSeparator();
        try {
            Path padre = archivo.toAbsolutePath().getParent();
            if (padre != null) {
                Files.createDirectories(padre);
            }
            Files.writeString(archivo, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RepositorioException("No se pudo guardar el usuario.", e);
        }
    }
}
