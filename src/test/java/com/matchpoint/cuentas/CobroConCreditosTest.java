package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.Pronostico;
import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.TipoMovimiento;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.EventoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioMemoria;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.CobroConCreditos;
import com.matchpoint.cuentas.servicio.LibroMovimientos;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import com.matchpoint.cuentas.servicio.SaldoInsuficienteException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-11: verificar saldo y descontar créditos al confirmar un pronóstico. */
class CobroConCreditosTest {

    static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    static final Usuario ANA = new Usuario("12345678", "Ana", Rol.PARTICIPANTE, "s", "h");

    private LibroMovimientos libro;
    private EventoRepositorioMemoria eventos;
    private PronosticoRepositorioMemoria pronosticos;
    private PronosticoServicio servicio;

    @BeforeEach
    void preparar() {
        libro = new LibroMovimientos(new MovimientoRepositorioMemoria(), RELOJ);
        eventos = new EventoRepositorioMemoria();
        pronosticos = new PronosticoRepositorioMemoria();
        servicio = new PronosticoServicio(eventos, pronosticos, new CobroConCreditos(libro), RELOJ);
        eventos.guardar(evento("E1", 10));
        eventos.guardar(evento("E2", 30));
        libro.registrar("12345678", TipoMovimiento.ASIGNACION_INICIAL, 40, "USUARIO", "INICIAL-12345678", null);
    }

    static Evento evento(String id, int costo) {
        return new Evento(id, "T1", "Partido " + id, LocalDateTime.of(2026, 11, 20, 15, 0),
                ModalidadPronostico.GANADOR_O_EMPATE, List.of("Local", "Empate", "Visitante"),
                LocalDateTime.of(2026, 11, 20, 14, 0), costo);
    }

    @Test
    void descuentaElCostoAlConfirmarYDejaMovimientoConOrigen() {
        Pronostico p = servicio.registrar(ANA, "E1", "Local");
        assertEquals(30, libro.saldoVigente("12345678"));
        Movimiento debito = libro.movimientosDe("12345678").get(1);
        assertEquals(TipoMovimiento.PARTICIPACION, debito.tipo());
        assertEquals(-10, debito.cantidad());
        assertEquals("PRONOSTICO:" + p.id(), debito.origen());
    }

    @Test
    void permiteDejarElSaldoEnCeroExacto() {
        servicio.registrar(ANA, "E1", "Local");
        servicio.registrar(ANA, "E2", "Empate");
        assertEquals(0, libro.saldoVigente("12345678"));
    }

    @Test
    void conSaldoInsuficienteRechazaSinRegistrarNiDescontar() {
        servicio.registrar(ANA, "E2", "Local"); // queda en 10
        eventos.guardar(evento("E3", 11));
        SaldoInsuficienteException ex = assertThrows(SaldoInsuficienteException.class,
                () -> servicio.registrar(ANA, "E3", "Local"));
        assertEquals(10, ex.getSaldoDisponible());
        assertEquals(10, libro.saldoVigente("12345678"));
        assertEquals(1, pronosticos.listar().size());
        assertTrue(libro.movimientosDe("12345678").stream().allMatch(m -> m.saldoResultante() >= 0));
    }

    @Test
    void sinSaldoInicialNoSePuedePronosticar() {
        Usuario luis = new Usuario("87654321", "Luis", Rol.PARTICIPANTE, "s", "h");
        assertThrows(SaldoInsuficienteException.class, () -> servicio.registrar(luis, "E1", "Local"));
        assertTrue(pronosticos.listar().isEmpty());
    }

    @Test
    void siFallaElGuardadoDevuelveLosCreditosConUnaReversion() {
        PronosticoServicio conFallo = new PronosticoServicio(eventos, new PronosticoRepositorioMemoria() {
            @Override
            public synchronized void guardar(Pronostico p) {
                throw new RepositorioException("No se pudo guardar el pronóstico.", null);
            }
        }, new CobroConCreditos(libro), RELOJ);
        assertThrows(RepositorioException.class, () -> conFallo.registrar(ANA, "E1", "Local"));
        assertEquals(40, libro.saldoVigente("12345678"));
        List<Movimiento> movs = libro.movimientosDe("12345678");
        assertEquals(3, movs.size(), "asignación + débito + reversión: nada se borra");
        assertEquals(TipoMovimiento.REVERSION, movs.get(2).tipo());
    }
}
