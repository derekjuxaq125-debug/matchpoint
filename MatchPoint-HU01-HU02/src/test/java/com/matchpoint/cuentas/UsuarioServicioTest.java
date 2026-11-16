package com.matchpoint.cuentas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.matchpoint.cuentas.modelo.Rol;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorio;
import com.matchpoint.cuentas.repositorio.UsuarioRepositorioMemoria;
import com.matchpoint.cuentas.servicio.UsuarioServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de HU-01: Registrar usuario. */
class UsuarioServicioTest {

    private UsuarioRepositorioMemoria repositorio;
    private UsuarioServicio servicio;

    @BeforeEach
    void preparar() {
        repositorio = new UsuarioRepositorioMemoria();
        servicio = new UsuarioServicio(repositorio);
    }

    @Test
    void registraConIdentificacionUnicaComoParticipante() {
        Usuario u = servicio.registrar("Ana Pérez", "12345678", "secreto1", "secreto1");
        assertEquals("12345678", u.identificacion());
        assertEquals(Rol.PARTICIPANTE, u.rol());
        assertEquals(1, repositorio.listar().size());
    }

    @Test
    void noGuardaLaClaveEnClaro() {
        Usuario u = servicio.registrar("Ana Pérez", "12345678", "secreto1", "secreto1");
        assertNotEquals("secreto1", u.hashClave());
        assertTrue(!u.hashClave().contains("secreto1"));
    }

    @Test
    void rechazaIdentificacionRepetida() {
        servicio.registrar("Ana Pérez", "12345678", "secreto1", "secreto1");
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.registrar("Otra Persona", "12345678", "otraClave", "otraClave"));
        assertEquals("La identificación ya está registrada. Use otra o inicie sesión.",
                ex.getErrores().get(UsuarioServicio.CAMPO_IDENTIFICACION));
        assertEquals(1, repositorio.listar().size());
    }

    @Test
    void identificacionRepetidaNoDistingueMayusculas() {
        servicio.registrar("Ana", "ABC12345", "secreto1", "secreto1");
        assertThrows(ValidacionException.class,
                () -> servicio.registrar("Otra", "abc12345", "secreto1", "secreto1"));
    }

    @Test
    void indicaCualCampoFalta() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.registrar("  ", "", "secreto1", "secreto1"));
        assertTrue(ex.getErrores().containsKey(UsuarioServicio.CAMPO_NOMBRE));
        assertTrue(ex.getErrores().containsKey(UsuarioServicio.CAMPO_IDENTIFICACION));
        assertEquals(2, ex.getErrores().size());
        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void rechazaIdentificacionConFormatoInvalido() {
        assertThrows(ValidacionException.class,
                () -> servicio.registrar("Ana", "12 3", "secreto1", "secreto1"));
    }

    @Test
    void rechazaClaveCortaOQueNoCoincide() {
        assertThrows(ValidacionException.class,
                () -> servicio.registrar("Ana", "12345678", "123", "123"));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> servicio.registrar("Ana", "12345678", "secreto1", "secreto2"));
        assertEquals("Las contraseñas no coinciden.", ex.getErrores().get(UsuarioServicio.CAMPO_CONFIRMACION));
        assertTrue(repositorio.listar().isEmpty());
    }

    @Test
    void siFallaElGuardadoNoSeCreaLaCuenta() {
        UsuarioRepositorio roto = new UsuarioRepositorioMemoria() {
            @Override
            public void guardar(Usuario usuario) {
                throw new RepositorioException("No se pudo guardar el usuario.", new RuntimeException());
            }
        };
        UsuarioServicio s = new UsuarioServicio(roto);
        assertThrows(RepositorioException.class,
                () -> s.registrar("Ana", "12345678", "secreto1", "secreto1"));
        assertTrue(roto.listar().isEmpty());
    }

    @Test
    void sembrarCreaOrganizadorYAuditorSoloUnaVez() {
        servicio.sembrarCuentasIniciales();
        servicio.sembrarCuentasIniciales();
        assertEquals(2, repositorio.listar().size());
    }
}
