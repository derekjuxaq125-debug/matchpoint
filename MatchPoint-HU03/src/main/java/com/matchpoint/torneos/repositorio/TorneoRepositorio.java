package com.matchpoint.torneos.repositorio;

import com.matchpoint.torneos.modelo.Torneo;
import java.util.List;

/** Acceso a los torneos almacenados. */
public interface TorneoRepositorio {

    /** Devuelve todos los torneos, en orden de creación. */
    List<Torneo> listar();

    /** Indica si ya existe un torneo con ese nombre (sin distinguir mayúsculas). */
    boolean existePorNombre(String nombre);

    /**
     * Guarda el torneo. Si falla, no debe quedar ningún torneo a medias.
     *
     * @throws RepositorioException si no se pudo guardar
     */
    void guardar(Torneo torneo);
}
