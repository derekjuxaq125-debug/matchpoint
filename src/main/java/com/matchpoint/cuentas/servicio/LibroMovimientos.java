package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Libro de movimientos de créditos (HU-09). Es el único punto del sistema que
 * cambia saldos: cada cambio queda como un movimiento confirmado con operación,
 * usuario, tipo, cantidad, fecha y origen, y nunca se borra.
 *
 * <p>Mantiene además el saldo vigente de cada usuario; HU-08 lo compara con la
 * suma de los movimientos para verificar que coinciden.
 */
public class LibroMovimientos {

    public static final String MENSAJE_CLAVE_REPETIDA = "Ya existe un movimiento con esa clave.";

    private final MovimientoRepositorio repositorio;
    private final Clock reloj;
    private final Map<String, Integer> saldosVigentes = new ConcurrentHashMap<>();

    public LibroMovimientos(MovimientoRepositorio repositorio) {
        this(repositorio, Clock.systemDefaultZone());
    }

    public LibroMovimientos(MovimientoRepositorio repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /**
     * Registra un movimiento confirmado.
     *
     * @param cantidad positiva (crédito) o negativa (débito), nunca cero
     * @param origen   operación que lo origina, p. ej. "PRONOSTICO:&lt;id&gt;"
     * @param clave    llave única del movimiento
     * @throws ValidacionException si faltan datos o la cantidad es cero
     * @throws SaldoInsuficienteException si el movimiento dejaría el saldo negativo (HU-11)
     * @throws com.matchpoint.cuentas.repositorio.RepositorioException si falla el guardado (el saldo no cambia)
     */
    public synchronized Movimiento registrar(String usuarioId, TipoMovimiento tipo, int cantidad,
                                             String origen, String clave, String justificacion) {
        validar(usuarioId, tipo, cantidad, origen, clave, justificacion);
        if (repositorio.buscarPorClave(clave).isPresent()) {
            throw new ValidacionException(Map.of("clave", MENSAJE_CLAVE_REPETIDA));
        }
        int saldoActual = saldoVigente(usuarioId);
        int nuevoSaldo = saldoActual + cantidad;
        if (nuevoSaldo < 0) {
            throw new SaldoInsuficienteException(saldoActual, -cantidad); // HU-11: nunca saldo negativo
        }
        Movimiento movimiento = new Movimiento(UUID.randomUUID().toString(), usuarioId, tipo, cantidad,
                nuevoSaldo, origen, clave, LocalDateTime.now(reloj), justificacion);
        repositorio.guardar(movimiento);              // si falla, no se toca el saldo
        saldosVigentes.put(clave(usuarioId), nuevoSaldo);
        return movimiento;
    }

    /** Saldo vigente del usuario (el que se le muestra). */
    public int saldoVigente(String usuarioId) {
        return saldosVigentes.computeIfAbsent(clave(usuarioId), k -> sumaMovimientos(usuarioId));
    }

    /** Suma de todos los movimientos del usuario; debe ser igual al saldo vigente. */
    public int sumaMovimientos(String usuarioId) {
        return repositorio.listarPorUsuario(usuarioId).stream().mapToInt(Movimiento::cantidad).sum();
    }

    /** Historial del usuario en orden cronológico. */
    public List<Movimiento> movimientosDe(String usuarioId) {
        return repositorio.listarPorUsuario(usuarioId);
    }

    public List<Movimiento> todos() {
        return repositorio.listar();
    }

    private static String clave(String usuarioId) {
        return usuarioId.toLowerCase();
    }

    private static void validar(String usuarioId, TipoMovimiento tipo, int cantidad, String origen,
                                String clave, String justificacion) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new ValidacionException(Map.of("usuario", "El movimiento debe indicar el usuario."));
        }
        if (tipo == null) {
            throw new ValidacionException(Map.of("tipo", "El movimiento debe indicar su tipo."));
        }
        if (cantidad == 0) {
            throw new ValidacionException(Map.of("cantidad", "La cantidad del movimiento no puede ser cero."));
        }
        if (origen == null || origen.isBlank() || clave == null || clave.isBlank()) {
            throw new ValidacionException(Map.of("origen", "El movimiento debe indicar su origen y su clave."));
        }
        if (tipo.exigeJustificacion() && (justificacion == null || justificacion.isBlank())) {
            throw new ValidacionException(Map.of("justificacion", "Los ajustes y reversiones exigen una justificación."));
        }
    }
}
