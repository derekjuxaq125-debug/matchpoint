package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Torneo;
import java.util.List;
import java.util.Optional;

/** Acceso a los torneos. HU-03 (Johan) completa la creación; HU-04 solo los consulta. */
public interface TorneoRepositorio {

    List<Torneo> listar();

    Optional<Torneo> buscarPorId(String id);

    /** @throws RepositorioException si no se pudo guardar */
    void guardar(Torneo torneo);
}
