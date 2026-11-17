package com.matchpoint.cuentas.repositorio;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura y escritura de archivos de texto con una línea por registro y campos
 * separados por ';' (cada campo va codificado, así que no puede contener ';').
 */
final class ArchivoLineas {

    private ArchivoLineas() {
    }

    static String cod(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    static String dec(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }

    static List<String[]> leer(Path archivo, String queSeLee) {
        if (!Files.exists(archivo)) {
            return List.of();
        }
        try {
            List<String[]> filas = new ArrayList<>();
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                if (!linea.isBlank()) {
                    filas.add(linea.split(";", -1));
                }
            }
            return filas;
        } catch (IOException e) {
            throw new RepositorioException("No se pudo leer el archivo de " + queSeLee + ".", e);
        }
    }

    static void agregar(Path archivo, String queSeGuarda, String... camposCodificados) {
        try {
            Path padre = archivo.toAbsolutePath().getParent();
            if (padre != null) {
                Files.createDirectories(padre);
            }
            Files.writeString(archivo, String.join(";", camposCodificados) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RepositorioException("No se pudo guardar " + queSeGuarda + ".", e);
        }
    }

    /**
     * Reescribe el archivo completo de forma segura: escribe en un temporal y lo
     * reemplaza de una vez, así un fallo no deja el archivo a medias.
     */
    static void reescribir(Path archivo, String queSeGuarda, List<String[]> filas) {
        try {
            Path padre = archivo.toAbsolutePath().getParent();
            if (padre != null) {
                Files.createDirectories(padre);
            }
            Path temporal = Files.createTempFile(padre, "mp", ".tmp");
            StringBuilder texto = new StringBuilder();
            for (String[] fila : filas) {
                texto.append(String.join(";", fila)).append(System.lineSeparator());
            }
            Files.writeString(temporal, texto, StandardCharsets.UTF_8);
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RepositorioException("No se pudo guardar " + queSeGuarda + ".", e);
        }
    }
}
