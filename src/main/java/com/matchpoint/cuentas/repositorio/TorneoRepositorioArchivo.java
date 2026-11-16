package com.matchpoint.cuentas.repositorio;

import static com.matchpoint.cuentas.repositorio.ArchivoLineas.cod;
import static com.matchpoint.cuentas.repositorio.ArchivoLineas.dec;

import com.matchpoint.cuentas.modelo.Torneo;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Torneos en archivo de texto. Formato: id;nombre;deporte;inicio;fin */
public class TorneoRepositorioArchivo implements TorneoRepositorio {

    private final Path archivo;

    public TorneoRepositorioArchivo(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public synchronized List<Torneo> listar() {
        try {
            return ArchivoLineas.leer(archivo, "torneos").stream()
                    .map(p -> new Torneo(dec(p[0]), dec(p[1]), dec(p[2]),
                            LocalDate.parse(p[3]), LocalDate.parse(p[4])))
                    .toList();
        } catch (RuntimeException e) {
            if (e instanceof RepositorioException) {
                throw e;
            }
            throw new RepositorioException("El archivo de torneos está dañado.", e);
        }
    }

    @Override
    public Optional<Torneo> buscarPorId(String id) {
        return listar().stream().filter(t -> t.id().equals(id)).findFirst();
    }

    @Override
    public synchronized void guardar(Torneo t) {
        ArchivoLineas.agregar(archivo, "el torneo", cod(t.id()), cod(t.nombre()), cod(t.deporte()),
                t.fechaInicio().toString(), t.fechaFin().toString());
    }
}
