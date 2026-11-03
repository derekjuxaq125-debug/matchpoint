package com.matchpoint.torneos.repositorio;

import com.matchpoint.torneos.modelo.Deporte;
import com.matchpoint.torneos.modelo.Torneo;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda los torneos en un archivo de texto (una línea por torneo) para que
 * persistan entre ejecuciones. Formato: nombre(codificado);DEPORTE;inicio;fin
 */
public class TorneoRepositorioArchivo implements TorneoRepositorio {

    private final Path archivo;

    public TorneoRepositorioArchivo(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public List<Torneo> listar() {
        if (!Files.exists(archivo)) {
            return List.of();
        }
        try {
            List<Torneo> resultado = new ArrayList<>();
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] p = linea.split(";");
                resultado.add(new Torneo(
                        URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                        Deporte.valueOf(p[1]),
                        LocalDate.parse(p[2]),
                        LocalDate.parse(p[3])));
            }
            return resultado;
        } catch (IOException | RuntimeException e) {
            throw new RepositorioException("No se pudo leer el archivo de torneos.", e);
        }
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return listar().stream().anyMatch(t -> t.nombre().equalsIgnoreCase(nombre));
    }

    @Override
    public void guardar(Torneo torneo) {
        String linea = URLEncoder.encode(torneo.nombre(), StandardCharsets.UTF_8) + ";"
                + torneo.deporte().name() + ";"
                + torneo.fechaInicio() + ";"
                + torneo.fechaFin() + System.lineSeparator();
        try {
            Path padre = archivo.toAbsolutePath().getParent();
            if (padre != null) {
                Files.createDirectories(padre);
            }
            Files.writeString(archivo, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RepositorioException("No se pudo guardar el torneo.", e);
        }
    }
}
