package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Evento;
import java.util.List;
import java.util.Optional;

/** Acceso a los eventos registrados (HU-04). */
public interface EventoRepositorio {

    /** Todos los eventos, en orden de creación. */
    List<Evento> listar();

    List<Evento> listarPorTorneo(String torneoId);

    Optional<Evento> buscarPorId(String id);

    /** @throws RepositorioException si no se pudo guardar (no debe quedar nada a medias) */
    void guardar(Evento evento);
}
