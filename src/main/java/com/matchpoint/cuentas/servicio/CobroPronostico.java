package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.Usuario;

/**
 * Punto de enlace entre HU-10 (registrar pronóstico, Santiago) y HU-11 (verificar
 * saldo y descontar créditos, Julian).
 *
 * <p>{@link PronosticoServicio} llama a {@link #cobrar} después de validar el
 * pronóstico y ANTES de guardarlo. Si la implementación lanza una excepción (por
 * ejemplo, saldo insuficiente), el pronóstico no se guarda. Julian reemplaza
 * {@link #NINGUNO} por el cobro real y usa {@code referencia} (el id del
 * pronóstico) como origen del movimiento de HU-09 y como llave de idempotencia
 * de HU-14.
 *
 * <p>Patrón Strategy (TT-04): {@link #NINGUNO} y {@link CobroConCreditos} son
 * estrategias intercambiables sin cambiar {@link PronosticoServicio}.
 */
@FunctionalInterface
public interface CobroPronostico {

    /** Mientras HU-11 no esté lista: no cobra nada. */
    CobroPronostico NINGUNO = (usuario, evento, referencia) -> { };

    /**
     * @param referencia id del pronóstico que se está confirmando
     * @throws RuntimeException si no se puede cobrar; el pronóstico no se registra
     */
    void cobrar(Usuario usuario, Evento evento, String referencia);

    /**
     * Devuelve lo cobrado cuando el pronóstico no se pudo guardar después del cobro
     * (operación fallida). No borra el débito: deja un movimiento de reversión.
     */
    default void revertir(Usuario usuario, Evento evento, String referencia) {
    }
}
