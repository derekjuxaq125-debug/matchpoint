package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Torneo;
import java.util.List;
import java.util.Optional;

/** Acceso a los torneos (HU-03 crea, HU-04 y HU-05 consultan). */
public interface TorneoRepositorio {

    List<Torneo> listar();

    Optional<Torneo> buscarPorId(String id);

    /** Busca por nombre sin distinguir mayúsculas ni espacios al inicio o al final (nombre único, HU-03). */
    default Optional<Torneo> buscarPorNombre(String nombre) {
        String buscado = nombre.trim();
        return listar().stream().filter(t -> t.nombre().trim().equalsIgnoreCase(buscado)).findFirst();
    }

    /** @throws RepositorioException si no se pudo guardar */
    void guardar(Torneo torneo);
}
