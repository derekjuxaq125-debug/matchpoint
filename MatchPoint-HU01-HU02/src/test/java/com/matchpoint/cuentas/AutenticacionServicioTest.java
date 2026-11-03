package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorioMemoria;
import com.matchpoint.cuentas.servicio.AutenticacionServicio;
import com.matchpoint.cuentas.servicio.CredencialesInvalidasException;
import com.matchpoint.cuentas.servicio.UsuarioServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-02: Iniciar sesión con rol. */
class AutenticacionServicioTest {

    private AutenticacionServicio autenticacion;

    @BeforeEach
    void preparar() {
        UsuarioRepositorioMemoria repositorio = new UsuarioRepositorioMemoria();
        UsuarioServicio usuarios = new UsuarioServicio(repositorio);
        usuarios.sembrarCuentasIniciales();
        usuarios.registrar("Ana Pérez", "12345678", "secreto1", "secreto1");
        autenticacion = new AutenticacionServicio(repositorio);
    }

    @Test
    void credencialesValidasDevuelvenUsuarioConSuRol() {
        Usuario participante = autenticacion.iniciarSesion("12345678", "secreto1");
        assertEquals(Rol.PARTICIPANTE, participante.rol());
        assertEquals(Rol.ORGANIZADOR, autenticacion.iniciarSesion("ORG0001", "organizador123").rol());
        assertEquals(Rol.AUDITOR, autenticacion.iniciarSesion("AUD0001", "auditor123").rol());
    }

    @Test
    void cadaRolSoloTieneSusOpciones() {
        assertEquals(true, Rol.ORGANIZADOR.getOpciones().contains("Crear torneo"));
        assertEquals(false, Rol.PARTICIPANTE.getOpciones().contains("Crear torneo"));
        assertEquals(false, Rol.AUDITOR.getOpciones().contains("Consultar mi saldo"));
    }

    @Test
    void claveIncorrectaNiegaElAcceso() {
        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> autenticacion.iniciarSesion("12345678", "equivocada"));
        assertEquals(AutenticacionServicio.MENSAJE_ACCESO_NEGADO, ex.getMessage());
    }

    @Test
    void usuarioInexistenteNiegaElAccesoConElMismoMensaje() {
        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> autenticacion.iniciarSesion("99999999", "secreto1"));
        assertEquals(AutenticacionServicio.MENSAJE_ACCESO_NEGADO, ex.getMessage());
    }

    @Test
    void camposVaciosIndicanCualFalta() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> autenticacion.iniciarSesion("", ""));
        assertEquals(2, ex.getErrores().size());
    }
}
