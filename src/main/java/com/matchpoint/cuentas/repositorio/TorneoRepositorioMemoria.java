package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Torneo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria (útil para pruebas). */
public class TorneoRepositorioMemoria implements TorneoRepositorio {

    private final List<Torneo> torneos = new ArrayList<>();

    @Override
    public synchronized List<Torneo> listar() {
        return List.copyOf(torneos);
    }

    @Override
    public synchronized Optional<Torneo> buscarPorId(String id) {
        return torneos.stream().filter(t -> t.id().equals(id)).findFirst();
    }

    @Override
    public synchronized void guardar(Torneo torneo) {
        torneos.add(torneo);
    }
}
