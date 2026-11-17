package com.matchpoint.torneos.repositorio;

import com.matchpoint.torneos.modelo.Torneo;
import java.util.ArrayList;
import java.util.List;

/** Implementación en memoria (útil para pruebas). */
public class TorneoRepositorioMemoria implements TorneoRepositorio {

    private final List<Torneo> torneos = new ArrayList<>();

    @Override
    public List<Torneo> listar() {
        return List.copyOf(torneos);
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return torneos.stream().anyMatch(t -> t.nombre().equalsIgnoreCase(nombre));
    }

    @Override
    public void guardar(Torneo torneo) {
        torneos.add(torneo);
    }
}
