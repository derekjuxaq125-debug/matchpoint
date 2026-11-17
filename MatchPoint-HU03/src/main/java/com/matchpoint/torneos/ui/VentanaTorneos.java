package com.matchpoint.torneos.ui;

import com.matchpoint.torneos.modelo.Torneo;
import com.matchpoint.torneos.servicio.TorneoServicio;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Lista de torneos con la opción "Crear torneo". */
public class VentanaTorneos extends JFrame {

    private final TorneoServicio servicio;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[] {"Nombre", "Deporte", "Fecha de inicio", "Fecha de fin"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public VentanaTorneos(TorneoServicio servicio) {
        super("MatchPoint - Torneos");
        this.servicio = servicio;

        JButton btnCrear = new JButton("Crear torneo");
        btnCrear.addActionListener(e -> abrirFormulario());

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(btnCrear);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        centro.add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);

        add(new JLabel("  Lista de torneos"), BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(barra, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 380);
        setLocationRelativeTo(null);
        refrescar();
    }

    private void abrirFormulario() {
        DialogoCrearTorneo dialogo = new DialogoCrearTorneo(this, servicio);
        dialogo.setVisible(true);
        refrescar(); // si se canceló, la lista queda igual
    }

    private void refrescar() {
        modelo.setRowCount(0);
        for (Torneo t : servicio.listar()) {
            modelo.addRow(new Object[] {t.nombre(), t.deporte(),
                    DialogoCrearTorneo.FORMATO.format(t.fechaInicio()),
                    DialogoCrearTorneo.FORMATO.format(t.fechaFin())});
        }
    }
}
