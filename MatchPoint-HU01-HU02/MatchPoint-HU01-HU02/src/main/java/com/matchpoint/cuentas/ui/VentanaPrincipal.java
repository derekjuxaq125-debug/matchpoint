package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.modelo.Usuario;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Pantalla posterior al inicio de sesión (HU-02): muestra solo las opciones
 * que corresponden al rol del usuario.
 */
public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal(Usuario usuario, VentanaLogin login) {
        super("MatchPoint — " + usuario.rol());

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(new JLabel("Bienvenido, " + usuario.nombre() + " (" + usuario.rol() + ")"), BorderLayout.NORTH);

        JPanel opciones = new JPanel(new GridLayout(0, 1, 6, 6));
        for (String opcion : usuario.rol().getOpciones()) {
            JButton boton = new JButton(opcion);
            boton.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    "\"" + opcion + "\" se habilitará con su historia de usuario correspondiente.",
                    opcion, JOptionPane.INFORMATION_MESSAGE));
            opciones.add(boton);
        }
        panel.add(opciones, BorderLayout.CENTER);

        JButton cerrarSesion = new JButton("Cerrar sesión");
        cerrarSesion.addActionListener(e -> {
            dispose();
            login.mostrarDeNuevo();
        });
        panel.add(cerrarSesion, BorderLayout.SOUTH);

        setContentPane(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(380, 120 + usuario.rol().getOpciones().size() * 40);
        setLocationRelativeTo(null);
    }
}
