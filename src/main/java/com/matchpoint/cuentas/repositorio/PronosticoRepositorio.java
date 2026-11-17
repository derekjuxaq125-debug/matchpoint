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

    default Optional<Pronostico> buscarPorId(String id) {
        return listar().stream().filter(p -> p.id().equals(id)).findFirst();
    }

    default List<Pronostico> listarPorUsuario(String usuarioId) {
        return listar().stream().filter(p -> p.usuarioId().equalsIgnoreCase(usuarioId)).toList();
    }

    /**
     * Reemplaza la opción de un pronóstico existente (HU-12, solo antes del cierre).
     *
     * @throws RepositorioException si no se pudo guardar (el pronóstico queda como estaba)
     */
    void actualizar(Pronostico pronostico);
}
