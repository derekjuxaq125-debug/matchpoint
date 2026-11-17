package com.matchpoint.cuentas.servicio;

import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorio;
import java.util.Map;
import java.util.Optional;

/**
 * HU-07: el organizador asigna el saldo inicial de créditos virtuales a un
 * usuario, una sola vez. La asignación queda como primer movimiento del libro
 * (HU-09), así el saldo siempre se puede verificar.
 */
public class CreditoServicio {

    public static final String CAMPO_USUARIO = "usuario";
    public static final String CAMPO_CANTIDAD = "cantidad";
    /** Máximo acordado en DEFINICIONES.md (sección 1). */
    public static final int MAXIMO_INICIAL = 10_000;

    private final UsuarioRepositorio usuarios;
    private final LibroMovimientos libro;

    public CreditoServicio(UsuarioRepositorio usuarios, LibroMovimientos libro) {
        this.usuarios = usuarios;
        this.libro = libro;
    }

    /**
     * Valida los datos sin registrar nada (antes de pedir la confirmación del diálogo).
     *
     * @return la cantidad ya convertida a entero
     */
    public int validar(Usuario actor, String usuarioId, String cantidadTexto) {
        exigirOrganizador(actor);
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new ValidacionException(Map.of(CAMPO_USUARIO, "Seleccione el usuario al que se asignan los créditos."));
        }
        Usuario destino = usuarios.buscarPorIdentificacion(usuarioId.trim()).orElseThrow(() ->
                new ValidacionException(Map.of(CAMPO_USUARIO, "El usuario seleccionado no está registrado.")));
        int cantidad = leerCantidad(cantidadTexto);
        if (saldoInicialDe(destino.identificacion()).isPresent()) {
            throw new ValidacionException(Map.of(CAMPO_USUARIO,
                    "El usuario ya tiene saldo inicial asignado; no se puede asignar de nuevo y su saldo no cambia."));
        }
        return cantidad;
    }

    /**
     * Registra el saldo inicial (usuario, cantidad y fecha) como movimiento.
     *
     * @throws AccesoDenegadoException si quien asigna no es organizador
     * @throws ValidacionException si la cantidad no es válida o ya tenía saldo inicial
     */
    public Movimiento asignarInicial(Usuario actor, String usuarioId, String cantidadTexto) {
        int cantidad = validar(actor, usuarioId, cantidadTexto);
        String destino = usuarios.buscarPorIdentificacion(usuarioId.trim()).orElseThrow().identificacion();
        // La clave INICIAL-<usuario> es única: dos asignaciones simultáneas producen un solo movimiento (HU-14/15).
        return libro.registrar(destino, TipoMovimiento.ASIGNACION_INICIAL, cantidad,
                "ORGANIZADOR:" + actor.identificacion(), claveInicial(destino), null);
    }

    /** Saldo inicial asignado, si ya lo tiene. */
    public Optional<Integer> saldoInicialDe(String usuarioId) {
        return libro.movimientosDe(usuarioId).stream()
                .filter(m -> m.tipo() == TipoMovimiento.ASIGNACION_INICIAL)
                .map(Movimiento::cantidad).findFirst();
    }

    public static String claveInicial(String usuarioId) {
        return "INICIAL-" + usuarioId.toLowerCase();
    }

    private static int leerCantidad(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            throw new ValidacionException(Map.of(CAMPO_CANTIDAD, "Falta el campo obligatorio: Cantidad de créditos."));
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            throw new ValidacionException(Map.of(CAMPO_CANTIDAD, "La cantidad debe ser un número entero."));
        }
        if (cantidad <= 0) {
            throw new ValidacionException(Map.of(CAMPO_CANTIDAD, "La cantidad debe ser un entero mayor que cero."));
        }
        if (cantidad > MAXIMO_INICIAL) {
            throw new ValidacionException(Map.of(CAMPO_CANTIDAD,
                    "La cantidad máxima de créditos iniciales es " + MAXIMO_INICIAL + "."));
        }
        return cantidad;
    }

    private static void exigirOrganizador(Usuario actor) {
        if (actor == null || actor.rol() != Rol.ORGANIZADOR) {
            throw new AccesoDenegadoException("Solo un organizador puede asignar créditos iniciales.");
        }
    }
}
