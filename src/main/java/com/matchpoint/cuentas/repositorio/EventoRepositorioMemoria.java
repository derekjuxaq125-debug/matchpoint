package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Evento;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria (útil para pruebas). */
public class EventoRepositorioMemoria implements EventoRepositorio {

    private final List<Evento> eventos = new ArrayList<>();

    @Override
    public synchronized List<Evento> listar() {
        return List.copyOf(eventos);
    }

    @Override
    public synchronized List<Evento> listarPorTorneo(String torneoId) {
        return eventos.stream().filter(e -> e.torneoId().equals(torneoId)).toList();
    }

    @Override
    public synchronized Optional<Evento> buscarPorId(String id) {
        return eventos.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    @Override
    public synchronized void guardar(Evento evento) {
        eventos.add(evento);
    }
}
