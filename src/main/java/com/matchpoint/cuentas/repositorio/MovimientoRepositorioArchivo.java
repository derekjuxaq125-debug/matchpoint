package com.matchpoint.cuentas.repositorio;

import static com.matchpoint.cuentas.repositorio.ArchivoLineas.cod;
import static com.matchpoint.cuentas.repositorio.ArchivoLineas.dec;

import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Libro de movimientos en archivo de texto, solo de agregado (nunca se reescribe).
 * Formato: operacion;usuario;tipo;cantidad;saldoResultante;origen;clave;fecha;justificacion
 */
public class MovimientoRepositorioArchivo implements MovimientoRepositorio {

    private final Path archivo;

    public MovimientoRepositorioArchivo(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public synchronized List<Movimiento> listar() {
        try {
            return ArchivoLineas.leer(archivo, "movimientos").stream()
                    .map(p -> new Movimiento(dec(p[0]), dec(p[1]), TipoMovimiento.valueOf(p[2]),
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]), dec(p[5]), dec(p[6]),
                            LocalDateTime.parse(p[7]), p[8].isEmpty() ? null : dec(p[8])))
                    .toList();
        } catch (RuntimeException e) {
            if (e instanceof RepositorioException) {
                throw e;
            }
            throw new RepositorioException("El archivo de movimientos está dañado.", e);
        }
    }

    @Override
    public synchronized void guardar(Movimiento m) {
        ArchivoLineas.agregar(archivo, "el movimiento", cod(m.operacion()), cod(m.usuarioId()), m.tipo().name(),
                Integer.toString(m.cantidad()), Integer.toString(m.saldoResultante()), cod(m.origen()),
                cod(m.claveIdempotencia()), m.registrado().toString(),
                m.justificacion() == null ? "" : cod(m.justificacion()));
    }
}
