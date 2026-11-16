package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Pronostico;
import java.util.List;
import java.util.Optional;

/** Acceso a los pronósticos registrados (HU-10). */
public interface PronosticoRepositorio {

    List<Pronostico> listar();

    Optional<Pronostico> buscarPorUsuarioYEvento(String usuarioId, String eventoId);

    /** @throws RepositorioException si no se pudo guardar (no debe quedar nada a medias) */
    void guardar(Pronostico pronostico);
}
