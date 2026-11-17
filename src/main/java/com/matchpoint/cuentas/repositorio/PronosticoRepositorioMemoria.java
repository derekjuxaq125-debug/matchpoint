package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Pronostico;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria (útil para pruebas). */
public class PronosticoRepositorioMemoria implements PronosticoRepositorio {

    private final List<Pronostico> pronosticos = new ArrayList<>();

    @Override
    public synchronized List<Pronostico> listar() {
        return List.copyOf(pronosticos);
    }

    @Override
    public synchronized Optional<Pronostico> buscarPorUsuarioYEvento(String usuarioId, String eventoId) {
        return pronosticos.stream()
                .filter(p -> p.usuarioId().equalsIgnoreCase(usuarioId) && p.eventoId().equals(eventoId))
                .findFirst();
    }

    @Override
    public synchronized void guardar(Pronostico pronostico) {
        pronosticos.add(pronostico);
    }

    @Override
    public synchronized void actualizar(Pronostico pronostico) {
        pronosticos.replaceAll(p -> p.id().equals(pronostico.id()) ? pronostico : p);
    }
}
