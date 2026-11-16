package com.matchpoint.cuentas;

import com.matchpoint.cuentas.repositorio.UsuarioRepositorioArchivo;
import com.matchpoint.cuentas.servicio.AutenticacionServicio;
import com.matchpoint.cuentas.servicio.UsuarioServicio;
import com.matchpoint.cuentas.ui.VentanaLogin;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada: abre el inicio de sesión (HU-01 Registrar usuario, HU-02 Iniciar sesión con rol). */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // se usa el aspecto por defecto
        }
        UsuarioRepositorioArchivo repositorio = new UsuarioRepositorioArchivo(Path.of("usuarios.txt"));
        UsuarioServicio usuarios = new UsuarioServicio(repositorio);
        AutenticacionServicio autenticacion = new AutenticacionServicio(repositorio);
        usuarios.sembrarCuentasIniciales();
        SwingUtilities.invokeLater(() -> new VentanaLogin(usuarios, autenticacion).setVisible(true));
    }
}
