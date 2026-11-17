package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Movimiento;
import java.util.List;
import java.util.Optional;

/**
 * Libro de movimientos (HU-09). A propósito no tiene métodos para modificar ni
 * borrar: todo movimiento confirmado se conserva.
 */
public interface MovimientoRepositorio {

    /** Todos los movimientos en orden cronológico de registro. */
    List<Movimiento> listar();

    default List<Movimiento> listarPorUsuario(String usuarioId) {
        return listar().stream().filter(m -> m.usuarioId().equalsIgnoreCase(usuarioId)).toList();
    }

    default Optional<Movimiento> buscarPorClave(String claveIdempotencia) {
        return listar().stream().filter(m -> m.claveIdempotencia().equals(claveIdempotencia)).findFirst();
    }

    /** @throws RepositorioException si no se pudo guardar (no queda nada a medias) */
    void guardar(Movimiento movimiento);
}
