package com.matchpoint.cuentas;

import com.matchpoint.cuentas.repositorio.EventoRepositorio;
import com.matchpoint.cuentas.repositorio.EventoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.MovimientoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.PronosticoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.TorneoRepositorio;
import com.matchpoint.cuentas.repositorio.TorneoRepositorioArchivo;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorio;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorioArchivo;
import com.matchpoint.cuentas.servicio.AutenticacionServicio;
import com.matchpoint.cuentas.servicio.CobroConCreditos;
import com.matchpoint.cuentas.servicio.ConsultaServicio;
import com.matchpoint.cuentas.servicio.CreditoServicio;
import com.matchpoint.cuentas.servicio.EventoServicio;
import com.matchpoint.cuentas.servicio.LibroMovimientos;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import com.matchpoint.cuentas.servicio.SaldoServicio;
import com.matchpoint.cuentas.servicio.TorneoServicio;
import com.matchpoint.cuentas.servicio.UsuarioServicio;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Arma los servicios y repositorios del sistema en un solo lugar. Las pantallas
 * piden aquí lo que necesitan; cuando el equipo cambie los archivos por la base
 * de datos (TT-01) solo se modifica esta clase.
 */
public final class Aplicacion {

    public final UsuarioServicio usuarios;
    public final AutenticacionServicio autenticacion;
    public final EventoServicio eventos;
    public final PronosticoServicio pronosticos;
    public final TorneoRepositorio torneos;
    public final TorneoServicio torneoServicio;
    public final EventoRepositorio repositorioEventos;
    public final UsuarioRepositorio repositorioUsuarios;
    public final LibroMovimientos libro;
    public final CreditoServicio creditos;
    public final ConsultaServicio consultas;
    public final SaldoServicio saldos;

    private Aplicacion(Path carpeta) {
        UsuarioRepositorioArchivo repoUsuarios = new UsuarioRepositorioArchivo(carpeta.resolve("usuarios.txt"));
        this.torneos = new TorneoRepositorioArchivo(carpeta.resolve("torneos.txt"));
        this.repositorioEventos = new EventoRepositorioArchivo(carpeta.resolve("eventos.txt"));
        PronosticoRepositorioArchivo repoPronosticos =
                new PronosticoRepositorioArchivo(carpeta.resolve("pronosticos.txt"));

        this.repositorioUsuarios = repoUsuarios;
        this.libro = new LibroMovimientos(new MovimientoRepositorioArchivo(carpeta.resolve("movimientos.txt")));
        this.creditos = new CreditoServicio(repoUsuarios, libro);
        this.saldos = new SaldoServicio(libro);

        this.usuarios = new UsuarioServicio(repoUsuarios);
        this.autenticacion = new AutenticacionServicio(repoUsuarios);
        this.torneoServicio = new TorneoServicio(torneos);
        this.consultas = new ConsultaServicio(torneos, repositorioEventos);
        this.eventos = new EventoServicio(repositorioEventos, torneos);
        this.pronosticos = new PronosticoServicio(repositorioEventos, repoPronosticos,
                new CobroConCreditos(libro), Clock.systemDefaultZone()); // HU-11
    }

    /** Crea la aplicación guardando sus datos en archivos de texto dentro de {@code carpeta}. */
    public static Aplicacion conArchivos(Path carpeta) {
        Aplicacion app = new Aplicacion(carpeta);
        app.usuarios.sembrarCuentasIniciales();
        return app;
    }
}
