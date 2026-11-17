package com.matchpoint.cuentas.repositorio;

import static com.matchpoint.cuentas.repositorio.ArchivoLineas.cod;
import static com.matchpoint.cuentas.repositorio.ArchivoLineas.dec;

import com.matchpoint.cuentas.modelo.Pronostico;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Pronósticos en archivo de texto. Formato: id;usuarioId;eventoId;opcion;registrado */
public class PronosticoRepositorioArchivo implements PronosticoRepositorio {

    private final Path archivo;

    public PronosticoRepositorioArchivo(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public synchronized List<Pronostico> listar() {
        try {
            return ArchivoLineas.leer(archivo, "pronósticos").stream()
                    .map(p -> new Pronostico(dec(p[0]), dec(p[1]), dec(p[2]), dec(p[3]),
                            LocalDateTime.parse(p[4])))
                    .toList();
        } catch (RuntimeException e) {
            if (e instanceof RepositorioException) {
                throw e;
            }
            throw new RepositorioException("El archivo de pronósticos está dañado.", e);
        }
    }

    @Override
    public Optional<Pronostico> buscarPorUsuarioYEvento(String usuarioId, String eventoId) {
        return listar().stream()
                .filter(p -> p.usuarioId().equalsIgnoreCase(usuarioId) && p.eventoId().equals(eventoId))
                .findFirst();
    }

    @Override
    public synchronized void guardar(Pronostico p) {
        ArchivoLineas.agregar(archivo, "el pronóstico", campos(p));
    }

    @Override
    public synchronized void actualizar(Pronostico cambiado) {
        List<String[]> filas = listar().stream()
                .map(p -> campos(p.id().equals(cambiado.id()) ? cambiado : p))
                .toList();
        ArchivoLineas.reescribir(archivo, "el pronóstico", filas);
    }

    private static String[] campos(Pronostico p) {
        return new String[]{cod(p.id()), cod(p.usuarioId()), cod(p.eventoId()), cod(p.opcion()),
                p.registrado().toString()};
    }
}
