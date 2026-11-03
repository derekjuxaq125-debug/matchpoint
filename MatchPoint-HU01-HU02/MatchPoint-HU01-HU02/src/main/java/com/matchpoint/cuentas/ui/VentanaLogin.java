package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.AutenticacionServicio;
import com.matchpoint.cuentas.servicio.CredencialesInvalidasException;
import com.matchpoint.cuentas.servicio.UsuarioServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.Border;

/** Pantalla de inicio de sesión (HU-02) con acceso al registro (HU-01). */
public class VentanaLogin extends JFrame {

    private static final Border BORDE_ERROR = BorderFactory.createLineBorder(Color.RED, 2);

    private final UsuarioServicio usuarios;
    private final AutenticacionServicio autenticacion;

    private final JTextField campoIdentificacion = new JTextField(18);
    private final JPasswordField campoClave = new JPasswordField(18);
    private final JLabel etiquetaError = new JLabel(" ");

    public VentanaLogin(UsuarioServicio usuarios, AutenticacionServicio autenticacion) {
        super("MatchPoint — Iniciar sesión");
        this.usuarios = usuarios;
        this.autenticacion = autenticacion;
        construir();
    }

    private void construir() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        JLabel titulo = new JLabel("MatchPoint");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(titulo, c);

        c.gridwidth = 1;
        c.gridy = 1;
        panel.add(new JLabel("Identificación *"), c);
        c.gridx = 1;
        panel.add(campoIdentificacion, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel("Contraseña *"), c);
        c.gridx = 1;
        panel.add(campoClave, c);

        etiquetaError.setForeground(Color.RED);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        panel.add(etiquetaError, c);

        JButton ingresar = new JButton("Ingresar");
        JButton registrarme = new JButton("Registrarme");
        JPanel botones = new JPanel();
        botones.add(ingresar);
        botones.add(registrarme);
        c.gridy = 4;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(botones, c);

        ingresar.addActionListener(e -> ingresar());
        registrarme.addActionListener(e -> new DialogoRegistro(this, usuarios).setVisible(true));
        getRootPane().setDefaultButton(ingresar);

        setContentPane(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void ingresar() {
        limpiarErrores();
        try {
            Usuario usuario = autenticacion.iniciarSesion(
                    campoIdentificacion.getText(), new String(campoClave.getPassword()));
            campoClave.setText("");
            new VentanaPrincipal(usuario, this).setVisible(true);
            setVisible(false);
        } catch (ValidacionException ex) {
            marcar(ex.getErrores());
            etiquetaError.setText(ex.getMessage());
        } catch (CredencialesInvalidasException ex) {
            etiquetaError.setText(ex.getMessage()); // acceso negado
            campoClave.setText("");
        } catch (RepositorioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Vuelve a mostrar el inicio de sesión (después de cerrar sesión). */
    void mostrarDeNuevo() {
        limpiarErrores();
        campoIdentificacion.setText("");
        campoClave.setText("");
        setVisible(true);
    }

    private void marcar(Map<String, String> errores) {
        if (errores.containsKey(AutenticacionServicio.CAMPO_IDENTIFICACION)) {
            campoIdentificacion.setBorder(BORDE_ERROR);
        }
        if (errores.containsKey(AutenticacionServicio.CAMPO_CLAVE)) {
            campoClave.setBorder(BORDE_ERROR);
        }
    }

    private void limpiarErrores() {
        Border normal = new JTextField().getBorder();
        campoIdentificacion.setBorder(normal);
        campoClave.setBorder(normal);
        etiquetaError.setText(" ");
    }
}
