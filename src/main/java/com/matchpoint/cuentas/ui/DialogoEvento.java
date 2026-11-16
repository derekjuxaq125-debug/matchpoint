package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.Aplicacion;
import com.matchpoint.cuentas.modelo.ModalidadPronostico;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.EventoServicio;
import com.matchpoint.cuentas.servicio.FechasEvento;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

/** Formulario para registrar un evento con sus opciones de pronóstico (HU-04). Solo organizador. */
public class DialogoEvento extends JDialog {

    private static final Border BORDE_ERROR = BorderFactory.createLineBorder(Color.RED, 2);

    private final Aplicacion app;
    private final Usuario usuario;

    private final JComboBox<Torneo> comboTorneo = new JComboBox<>();
    private final JTextField campoNombre = new JTextField(24);
    private final JTextField campoInicio = new JTextField(
            FechasEvento.escribir(LocalDateTime.now().plusDays(1).withMinute(0)), 24);
    private final JComboBox<ModalidadPronostico> comboModalidad = new JComboBox<>(ModalidadPronostico.values());
    private final JTextArea campoOpciones = new JTextArea(5, 24);
    private final JScrollPane panelOpciones = new JScrollPane(campoOpciones);
    private final JTextField campoCierre = new JTextField(
            FechasEvento.escribir(LocalDateTime.now().plusHours(12).withMinute(0)), 24);
    private final JTextField campoCosto = new JTextField("10", 24);
    private final JLabel etiquetaError = new JLabel(" ");
    private final Map<String, JComponent> camposPorClave = new LinkedHashMap<>();

    public DialogoEvento(Window padre, Aplicacion app, Usuario usuario) {
        super(padre, "Registrar evento", ModalityType.APPLICATION_MODAL);
        this.app = app;
        this.usuario = usuario;
        app.torneos.listar().forEach(comboTorneo::addItem);
        camposPorClave.put(EventoServicio.CAMPO_TORNEO, comboTorneo);
        camposPorClave.put(EventoServicio.CAMPO_NOMBRE, campoNombre);
        camposPorClave.put(EventoServicio.CAMPO_INICIO, campoInicio);
        camposPorClave.put(EventoServicio.CAMPO_MODALIDAD, comboModalidad);
        camposPorClave.put(EventoServicio.CAMPO_OPCIONES, panelOpciones);
        camposPorClave.put(EventoServicio.CAMPO_CIERRE, campoCierre);
        camposPorClave.put(EventoServicio.CAMPO_COSTO, campoCosto);
        construir();
    }

    private void construir() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        agregarFila(panel, c, 0, "Torneo *", comboTorneo);
        agregarFila(panel, c, 1, "Nombre del evento *", campoNombre);
        agregarFila(panel, c, 2, "Fecha y hora (" + FechasEvento.PATRON + ") *", campoInicio);
        agregarFila(panel, c, 3, "Modalidad de pronóstico *", comboModalidad);
        agregarFila(panel, c, 4, "Opciones (una por línea) *", panelOpciones);
        agregarFila(panel, c, 5, "Hora de cierre (" + FechasEvento.PATRON + ") *", campoCierre);
        agregarFila(panel, c, 6, "Costo en créditos *", campoCosto);

        etiquetaError.setForeground(Color.RED);
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        panel.add(etiquetaError, c);

        JButton registrar = new JButton("Registrar");
        JButton cancelar = new JButton("Cancelar");
        JPanel botones = new JPanel();
        botones.add(registrar);
        botones.add(cancelar);
        c.gridy = 8;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(botones, c);

        registrar.addActionListener(e -> registrar());
        cancelar.addActionListener(e -> dispose()); // no guarda nada
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
        Torneo torneo = (Torneo) comboTorneo.getSelectedItem();
        try {
            app.eventos.registrar(usuario,
                    torneo == null ? null : torneo.id(),
                    campoNombre.getText(),
                    campoInicio.getText(),
                    (ModalidadPronostico) comboModalidad.getSelectedItem(),
                    Arrays.asList(campoOpciones.getText().split("\\R")),
                    campoCierre.getText(),
                    campoCosto.getText());
            JOptionPane.showMessageDialog(this, "Evento registrado correctamente.",
                    "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (ValidacionException ex) {
            ex.getErrores().keySet().forEach(k -> camposPorClave.get(k).setBorder(BORDE_ERROR));
            etiquetaError.setText("<html>" + String.join("<br>", ex.getErrores().values()) + "</html>");
            pack();
        } catch (AccesoDenegadoException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Acceso denegado", JOptionPane.WARNING_MESSAGE);
        } catch (RepositorioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() + " No se registró el evento.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarErrores() {
        Border normal = new JTextField().getBorder();
        camposPorClave.forEach((clave, campo) ->
                campo.setBorder(campo == panelOpciones ? new JScrollPane().getBorder() : normal));
        etiquetaError.setText(" ");
    }
}
