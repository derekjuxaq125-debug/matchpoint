package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.Aplicacion;
import com.matchpoint.cuentas.modelo.TorneoConEventos;
import com.matchpoint.cuentas.modelo.TorneoConEventos.EventoConEstado;
import com.matchpoint.cuentas.servicio.FechasEvento;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * HU-05: torneos con sus eventos y el estado de cada uno.
 * HU-06: al seleccionar un evento se ven sus opciones de pronóstico y la fecha y hora de cierre.
 */
public class VentanaTorneosEventos extends JDialog {

    private final Aplicacion app;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Torneo", "Deporte", "Evento", "Fecha y hora", "Cierre", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel detalle = new JLabel(" ");
    private final List<EventoConEstado> filas = new ArrayList<>();

    public VentanaTorneosEventos(Window padre, Aplicacion app, String titulo) {
        super(padre, titulo, ModalityType.APPLICATION_MODAL);
        this.app = app;
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> mostrarDetalle());
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(760, 260));

        detalle.setBorder(BorderFactory.createTitledBorder("Opciones de pronóstico (HU-06)"));
        JButton actualizar = new JButton("Actualizar estados");
        actualizar.addActionListener(e -> cargar());
        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel();
        botones.add(actualizar);
        botones.add(cerrar);

        JPanel sur = new JPanel(new BorderLayout(6, 6));
        sur.add(detalle, BorderLayout.CENTER);
        sur.add(botones, BorderLayout.SOUTH);

        JPanel contenido = new JPanel(new BorderLayout(8, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(scroll, BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);
        setContentPane(contenido);
        cargar();
        pack();
        setLocationRelativeTo(padre);
    }

    private void cargar() {
        modelo.setRowCount(0);
        filas.clear();
        for (TorneoConEventos t : app.consultas.torneosConEventos()) {
            if (t.eventos().isEmpty()) {
                modelo.addRow(new Object[]{t.torneo().nombre(), t.torneo().deporte(), "(sin eventos)", "", "", ""});
                filas.add(null);
            }
            for (EventoConEstado e : t.eventos()) {
                modelo.addRow(new Object[]{t.torneo().nombre(), t.torneo().deporte(), e.evento().nombre(),
                        FechasEvento.escribir(e.evento().inicio()), FechasEvento.escribir(e.evento().cierre()),
                        e.estado()});
                filas.add(e);
            }
        }
        if (filas.isEmpty()) {
            detalle.setText("Todavía no hay torneos disponibles.");
        }
    }

    private void mostrarDetalle() {
        int fila = tabla.getSelectedRow();
        EventoConEstado e = fila < 0 ? null : filas.get(fila);
        if (e == null) {
            detalle.setText(" ");
            return;
        }
        detalle.setText("<html><b>" + e.evento().nombre() + "</b> — " + e.evento().modalidad()
                + "<br>Opciones: " + String.join(" · ", e.evento().opciones())
                + "<br>Cierre: " + FechasEvento.escribir(e.evento().cierre())
                + " · Costo: " + e.evento().costoCreditos() + " créditos · Estado: " + e.estado() + "</html>");
    }
}
