package com.matchpoint.cuentas.repositorio;

import com.matchpoint.cuentas.modelo.Movimiento;
import java.util.ArrayList;
import java.util.List;

/** Libro de movimientos en memoria (pruebas). */
public class MovimientoRepositorioMemoria implements MovimientoRepositorio {

    private final List<Movimiento> movimientos = new ArrayList<>();

    @Override
    public synchronized List<Movimiento> listar() {
        return List.copyOf(movimientos);
    }

    @Override
    public synchronized void guardar(Movimiento movimiento) {
        movimientos.add(movimiento);
    }
}
