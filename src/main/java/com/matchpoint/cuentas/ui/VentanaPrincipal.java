package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.Aplicacion;
import com.matchpoint.cuentas.modelo.Opcion;
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
 * que corresponden al rol del usuario. Las opciones ya implementadas abren su
 * pantalla; las demás se habilitan con su historia de usuario.
 */
public class VentanaPrincipal extends JFrame {

    private final Aplicacion app;
    private final Usuario usuario;

    public VentanaPrincipal(Aplicacion app, Usuario usuario, VentanaLogin login) {
        super("MatchPoint — " + usuario.rol());
        this.app = app;
        this.usuario = usuario;

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(new JLabel("Bienvenido, " + usuario.nombre() + " (" + usuario.rol() + ")"), BorderLayout.NORTH);

        JPanel opciones = new JPanel(new GridLayout(0, 1, 6, 6));
        for (Opcion opcion : usuario.rol().getOpciones()) {
            JButton boton = new JButton(opcion.toString());
            boton.addActionListener(e -> abrir(opcion));
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

    private void abrir(Opcion opcion) {
        switch (opcion) {
            case REGISTRAR_EVENTO -> new DialogoEvento(this, app, usuario).setVisible(true);
            case REGISTRAR_PRONOSTICO -> new DialogoPronostico(this, app, usuario).setVisible(true);
            case LISTAR_TORNEOS_Y_EVENTOS, VER_OPCIONES_PRONOSTICO ->
                    new VentanaTorneosEventos(this, app, opcion.toString()).setVisible(true);
            case CREAR_TORNEO -> new DialogoTorneo(this, app, usuario).setVisible(true);
            case ASIGNAR_CREDITOS -> new DialogoAsignarCreditos(this, app, usuario).setVisible(true);
            case CAMBIAR_PRONOSTICO -> new DialogoMisPronosticos(this, app, usuario).setVisible(true);
            default -> JOptionPane.showMessageDialog(this,
                    "\"" + opcion + "\" se habilitará con su historia de usuario correspondiente.",
                    opcion.toString(), JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
