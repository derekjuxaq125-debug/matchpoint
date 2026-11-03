package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.UsuarioServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.Border;

/** Formulario de registro de usuario nuevo (HU-01). */
public class DialogoRegistro extends JDialog {

    private static final Border BORDE_ERROR = BorderFactory.createLineBorder(Color.RED, 2);

    private final UsuarioServicio servicio;

    private final JTextField campoNombre = new JTextField(20);
    private final JTextField campoIdentificacion = new JTextField(20);
    private final JPasswordField campoClave = new JPasswordField(20);
    private final JPasswordField campoConfirmacion = new JPasswordField(20);
    private final JLabel etiquetaError = new JLabel(" ");
    private final Map<String, JComponent> camposPorClave = new LinkedHashMap<>();

    public DialogoRegistro(Window padre, UsuarioServicio servicio) {
        super(padre, "Registrar usuario", ModalityType.APPLICATION_MODAL);
        this.servicio = servicio;
        camposPorClave.put(UsuarioServicio.CAMPO_NOMBRE, campoNombre);
        camposPorClave.put(UsuarioServicio.CAMPO_IDENTIFICACION, campoIdentificacion);
        camposPorClave.put(UsuarioServicio.CAMPO_CLAVE, campoClave);
        camposPorClave.put(UsuarioServicio.CAMPO_CONFIRMACION, campoConfirmacion);
        construir();
    }

    private void construir() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        agregarFila(panel, c, 0, "Nombre *", campoNombre);
        agregarFila(panel, c, 1, "Identificación *", campoIdentificacion);
        agregarFila(panel, c, 2, "Contraseña *", campoClave);
        agregarFila(panel, c, 3, "Confirmar contraseña *", campoConfirmacion);

        etiquetaError.setForeground(Color.RED);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        panel.add(etiquetaError, c);

        JButton registrar = new JButton("Registrar");
        JButton cancelar = new JButton("Cancelar");
        JPanel botones = new JPanel();
        botones.add(registrar);
        botones.add(cancelar);
        c.gridy = 5;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(botones, c);

        registrar.addActionListener(e -> registrar());
        cancelar.addActionListener(e -> dispose()); // no guarda nada y vuelve al inicio de sesión
        getRootPane().setDefaultButton(registrar);

        setContentPane(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private void agregarFila(JPanel panel, GridBagConstraints c, int fila, String texto, JComponent campo) {
        c.gridx = 0;
        c.gridy = fila;
        c.gridwidth = 1;
        panel.add(new JLabel(texto), c);
        c.gridx = 1;
        panel.add(campo, c);
    }

    private void registrar() {
        limpiarErrores();
        try {
            servicio.registrar(
                    campoNombre.getText(),
                    campoIdentificacion.getText(),
                    new String(campoClave.getPassword()),
                    new String(campoConfirmacion.getPassword()));
            JOptionPane.showMessageDialog(this,
                    "Cuenta creada correctamente. Ya puede iniciar sesión.",
                    "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (ValidacionException ex) {
            ex.getErrores().keySet().forEach(k -> camposPorClave.get(k).setBorder(BORDE_ERROR));
            etiquetaError.setText("<html>" + String.join("<br>", ex.getErrores().values()) + "</html>");
            pack();
        } catch (RepositorioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() + " No se creó la cuenta.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarErrores() {
        Border normal = new JTextField().getBorder();
        camposPorClave.values().forEach(campo -> campo.setBorder(normal));
        etiquetaError.setText(" ");
    }
}
