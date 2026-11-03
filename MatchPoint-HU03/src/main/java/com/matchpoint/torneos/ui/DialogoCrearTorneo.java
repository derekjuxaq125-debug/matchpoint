package com.matchpoint.torneos.ui;

import com.matchpoint.torneos.modelo.Deporte;
import com.matchpoint.torneos.modelo.Torneo;
import com.matchpoint.torneos.repositorio.RepositorioException;
import com.matchpoint.torneos.servicio.TorneoServicio;
import com.matchpoint.torneos.servicio.ValidacionException;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * Formulario "Crear torneo": Nombre, Deporte (lista desplegable), Fecha de inicio
 * y Fecha de fin, con los botones "Crear" y "Cancelar".
 */
public class DialogoCrearTorneo extends JDialog {

    static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final Border BORDE_ERROR = BorderFactory.createLineBorder(Color.RED, 2);

    private final TorneoServicio servicio;
    private final JTextField txtNombre = new JTextField(22);
    private final JComboBox<Deporte> cmbDeporte = new JComboBox<>();
    private final JTextField txtInicio = new JTextField(10);
    private final JTextField txtFin = new JTextField(10);
    private final Map<String, JLabel> mensajes = new LinkedHashMap<>();
    private final Map<String, JComponent> campos = new LinkedHashMap<>();
    private Torneo torneoCreado;

    public DialogoCrearTorneo(JFrame padre, TorneoServicio servicio) {
        super(padre, "Crear torneo", true);
        this.servicio = servicio;
        construir();
        pack();
        setLocationRelativeTo(padre);
        setResizable(false);
    }

    /** Torneo creado, o null si se canceló. */
    public Torneo getTorneoCreado() {
        return torneoCreado;
    }

    private void construir() {
        cmbDeporte.addItem(null); // opción vacía: obliga a elegir un deporte
        for (Deporte d : Deporte.values()) {
            cmbDeporte.addItem(d);
        }
        cmbDeporte.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> l, Object v,
                    int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v == null ? "Seleccione un deporte..." : v, i, s, f);
            }
        });

        campos.put(TorneoServicio.CAMPO_NOMBRE, txtNombre);
        campos.put(TorneoServicio.CAMPO_DEPORTE, cmbDeporte);
        campos.put(TorneoServicio.CAMPO_INICIO, txtInicio);
        campos.put(TorneoServicio.CAMPO_FIN, txtFin);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 0, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int fila = 0;
        fila = agregarFila(form, c, fila, "Nombre *", txtNombre, TorneoServicio.CAMPO_NOMBRE);
        fila = agregarFila(form, c, fila, "Deporte *", cmbDeporte, TorneoServicio.CAMPO_DEPORTE);
        fila = agregarFila(form, c, fila, "Fecha de inicio * (dd/mm/aaaa)", txtInicio, TorneoServicio.CAMPO_INICIO);
        agregarFila(form, c, fila, "Fecha de fin * (dd/mm/aaaa)", txtFin, TorneoServicio.CAMPO_FIN);

        JButton btnCrear = new JButton("Crear");
        JButton btnCancelar = new JButton("Cancelar");
        btnCrear.addActionListener(e -> crear());
        btnCancelar.addActionListener(e -> dispose()); // no se guarda nada
        getRootPane().setDefaultButton(btnCrear);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCancelar);
        botones.add(btnCrear);

        add(form, java.awt.BorderLayout.CENTER);
        add(botones, java.awt.BorderLayout.SOUTH);
    }

    private int agregarFila(JPanel form, GridBagConstraints c, int fila, String etiqueta,
            JComponent campo, String clave) {
        c.gridx = 0;
        c.gridy = fila;
        c.weightx = 0;
        form.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(campo, c);

        JLabel msg = new JLabel(" ");
        msg.setForeground(Color.RED);
        msg.setFont(msg.getFont().deriveFont(11f));
        mensajes.put(clave, msg);
        c.gridx = 1;
        c.gridy = fila + 1;
        form.add(msg, c);
        return fila + 2;
    }

    private void limpiarErrores() {
        mensajes.values().forEach(l -> l.setText(" "));
        campos.values().forEach(f -> f.setBorder(null));
        pack();
    }

    private void mostrarErrores(Map<String, String> errores) {
        errores.forEach((clave, texto) -> {
            mensajes.get(clave).setText("<html>" + texto + "</html>");
            campos.get(clave).setBorder(BORDE_ERROR);
        });
        pack();
    }

    private void crear() {
        limpiarErrores();
        Map<String, String> erroresFormato = new LinkedHashMap<>();
        LocalDate inicio = leerFecha(txtInicio, TorneoServicio.CAMPO_INICIO, erroresFormato);
        LocalDate fin = leerFecha(txtFin, TorneoServicio.CAMPO_FIN, erroresFormato);
        if (!erroresFormato.isEmpty()) {
            mostrarErrores(erroresFormato);
            return;
        }
        try {
            Torneo torneo = servicio.crear(txtNombre.getText(), (Deporte) cmbDeporte.getSelectedItem(),
                    inicio, fin);
            torneoCreado = torneo;
            JOptionPane.showMessageDialog(this,
                    "El torneo \"" + torneo.nombre() + "\" se creó correctamente.",
                    "Torneo creado", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (ValidacionException e) {
            mostrarErrores(e.getErrores());
        } catch (RepositorioException e) {
            JOptionPane.showMessageDialog(this,
                    "Ocurrió un error al guardar. No se creó el torneo.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Devuelve null si el campo está vacío (lo reporta el servicio) o tiene formato inválido. */
    private LocalDate leerFecha(JTextField campo, String clave, Map<String, String> errores) {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(texto, FORMATO);
        } catch (DateTimeParseException e) {
            errores.put(clave, "Fecha no válida. Use el formato dd/mm/aaaa.");
            return null;
        }
    }
}
