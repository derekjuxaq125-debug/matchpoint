package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.Aplicacion;
import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.FechasEvento;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Formulario para registrar un pronóstico (HU-10). Muestra todos los eventos con
 * su estado; si el elegido ya cerró, el servicio lo rechaza (HU-12). Solo participante.
 */
public class DialogoPronostico extends JDialog {

    private static final Border BORDE_ERROR = BorderFactory.createLineBorder(Color.RED, 2);

    private final Aplicacion app;
    private final Usuario usuario;

    private final JComboBox<EventoItem> comboEvento = new JComboBox<>();
    private final JComboBox<String> comboOpcion = new JComboBox<>();
    private final JLabel etiquetaDetalle = new JLabel(" ");
    private final JLabel etiquetaError = new JLabel(" ");
    private final Map<String, JComponent> camposPorClave = new LinkedHashMap<>();
    /** HU-14: una clave por diálogo; si el usuario reintenta, se reutiliza y no se cobra dos veces. */
    private final String claveSolicitud = UUID.randomUUID().toString();

    /** Evento con el texto que se muestra en la lista. */
    private record EventoItem(Evento evento, String texto) {
        @Override
        public String toString() {
            return texto;
        }
    }

    public DialogoPronostico(Window padre, Aplicacion app, Usuario usuario) {
        super(padre, "Registrar pronóstico", ModalityType.APPLICATION_MODAL);
        this.app = app;
        this.usuario = usuario;
        camposPorClave.put(PronosticoServicio.CAMPO_EVENTO, comboEvento);
        camposPorClave.put(PronosticoServicio.CAMPO_OPCION, comboOpcion);
        cargarEventos();
        construir();
    }

    private void cargarEventos() {
        LocalDateTime ahora = LocalDateTime.now();
        for (Evento e : app.repositorioEventos.listar()) {
            String torneo = app.torneos.buscarPorId(e.torneoId()).map(t -> t.nombre()).orElse("?");
            comboEvento.addItem(new EventoItem(e, torneo + " — " + e.nombre() + " [" + e.estado(ahora) + "]"));
        }
    }

    private void construir() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Evento *"), c);
        c.gridx = 1;
        panel.add(comboEvento, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("Su pronóstico *"), c);
        c.gridx = 1;
        panel.add(comboOpcion, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        panel.add(etiquetaDetalle, c);

        etiquetaError.setForeground(Color.RED);
        c.gridy = 3;
        panel.add(etiquetaError, c);

        JButton registrar = new JButton("Registrar pronóstico");
        JButton cancelar = new JButton("Cancelar");
        JPanel botones = new JPanel();
        botones.add(registrar);
        botones.add(cancelar);
        c.gridy = 4;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(botones, c);

        comboEvento.addActionListener(e -> mostrarOpciones());
        registrar.addActionListener(e -> registrar());
        cancelar.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(registrar);
        mostrarOpciones();

        if (comboEvento.getItemCount() == 0) {
            etiquetaError.setText("Todavía no hay eventos registrados.");
            registrar.setEnabled(false);
        }

        setContentPane(panel);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    private void mostrarOpciones() {
        EventoItem item = (EventoItem) comboEvento.getSelectedItem();
        if (item == null) {
            comboOpcion.setModel(new DefaultComboBoxModel<>());
            return;
        }
        comboOpcion.setModel(new DefaultComboBoxModel<>(item.evento().opciones().toArray(new String[0])));
        etiquetaDetalle.setText("Cierre: " + FechasEvento.escribir(item.evento().cierre())
                + " · Costo: " + item.evento().costoCreditos() + " créditos");
    }

    private void registrar() {
        limpiarErrores();
        EventoItem item = (EventoItem) comboEvento.getSelectedItem();
        try {
            boolean repetida = app.pronosticos.registrar(usuario,
                    item == null ? null : item.evento().id(),
                    (String) comboOpcion.getSelectedItem(), claveSolicitud).repetida();
            JOptionPane.showMessageDialog(this, repetida
                            ? "La operación ya fue procesada; no se cobró de nuevo."
                            : "Pronóstico registrado correctamente. Saldo: "
                              + app.libro.saldoVigente(usuario.identificacion()) + " créditos.",
                    "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (ValidacionException ex) {
            ex.getErrores().keySet().forEach(k -> camposPorClave.get(k).setBorder(BORDE_ERROR));
            etiquetaError.setText("<html>" + String.join("<br>", ex.getErrores().values()) + "</html>");
            pack();
        } catch (AccesoDenegadoException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Acceso denegado", JOptionPane.WARNING_MESSAGE);
        } catch (RepositorioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() + " No se registró el pronóstico.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarErrores() {
        Border normal = new JComboBox<String>().getBorder();
        camposPorClave.values().forEach(campo -> campo.setBorder(normal));
        etiquetaError.setText(" ");
    }
}
