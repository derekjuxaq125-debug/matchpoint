package com.matchpoint.cuentas.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Ayuda para armar formularios de etiqueta + campo y resaltar en rojo los
 * campos con error (mismo comportamiento que los formularios de HU-01 y HU-04).
 */
final class Formulario {

    private static final Border BORDE_ERROR = BorderFactory.createLineBorder(Color.RED, 2);

    final JPanel panel = new JPanel(new GridBagLayout());
    final JLabel error = new JLabel(" ");
    private final Map<String, JComponent> campos = new LinkedHashMap<>();
    private final Map<JComponent, Border> bordes = new LinkedHashMap<>();
    private int fila;

    Formulario() {
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        error.setForeground(Color.RED);
    }

    Formulario campo(String clave, String etiqueta, JComponent control) {
        GridBagConstraints c = base();
        c.gridx = 0;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(control, c);
        if (clave != null) {
            campos.put(clave, control);
            bordes.put(control, control.getBorder());
        }
        fila++;
        return this;
    }

    Formulario ancho(JComponent control) {
        GridBagConstraints c = base();
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        panel.add(control, c);
        fila++;
        return this;
    }

    /** Agrega la etiqueta de error y los botones al final. */
    JPanel cerrar(JComponent botones) {
        GridBagConstraints c = base();
        c.gridx = 0;
        c.gridwidth = 2;
        panel.add(error, c);
        c.gridy = ++fila;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(botones, c);
        return panel;
    }

    void limpiar() {
        bordes.forEach(JComponent::setBorder);
        error.setText(" ");
    }

    void marcar(Map<String, String> errores) {
        limpiar();
        errores.keySet().forEach(k -> {
            JComponent control = campos.get(k);
            if (control != null) {
                control.setBorder(BORDE_ERROR);
            }
        });
        error.setText("<html>" + String.join("<br>", errores.values()) + "</html>");
    }

    private GridBagConstraints base() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = fila;
        return c;
    }
}
