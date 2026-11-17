package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.Aplicacion;
import com.matchpoint.cuentas.modelo.Torneo;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.TorneoServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * HU-03: lista de torneos y formulario "Crear torneo" (nombre, deporte en lista
 * desplegable, fecha de inicio y de fin). "Cancelar" no guarda nada y vuelve a la lista.
 */
public class DialogoTorneo extends JDialog {

    private final Aplicacion app;
    private final Usuario organizador;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Nombre", "Deporte", "Inicio", "Fin"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTextField nombre = new JTextField(22);
    private final JComboBox<String> deporte = new JComboBox<>();
    private final JTextField inicio = new JTextField(10);
    private final JTextField fin = new JTextField(10);
    private final Formulario form = new Formulario();

    public DialogoTorneo(Window padre, Aplicacion app, Usuario organizador) {
        super(padre, "Torneos", ModalityType.APPLICATION_MODAL);
        this.app = app;
        this.organizador = organizador;
        deporte.addItem("");
        TorneoServicio.DEPORTES.forEach(deporte::addItem);
        inicio.setToolTipText(TorneoServicio.PATRON_FECHA);
        fin.setToolTipText(TorneoServicio.PATRON_FECHA);

        JButton crear = new JButton("Crear");
        JButton cancelar = new JButton("Cancelar");
        JPanel botones = new JPanel();
        botones.add(crear);
        botones.add(cancelar);
        crear.addActionListener(e -> crear());
        cancelar.addActionListener(e -> limpiar());

        form.campo(TorneoServicio.CAMPO_NOMBRE, "Nombre *", nombre)
                .campo(TorneoServicio.CAMPO_DEPORTE, "Deporte *", deporte)
                .campo(TorneoServicio.CAMPO_INICIO, "Fecha de inicio * (aaaa-mm-dd)", inicio)
                .campo(TorneoServicio.CAMPO_FIN, "Fecha de fin * (aaaa-mm-dd)", fin);
        JPanel formulario = form.cerrar(botones);
        formulario.setBorder(BorderFactory.createTitledBorder("Crear torneo"));

        JScrollPane lista = new JScrollPane(new JTable(modelo));
        lista.setPreferredSize(new Dimension(520, 170));
        lista.setBorder(BorderFactory.createTitledBorder("Lista de torneos"));

        JPanel contenido = new JPanel(new BorderLayout(8, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(lista, BorderLayout.CENTER);
        contenido.add(formulario, BorderLayout.SOUTH);
        setContentPane(contenido);
        getRootPane().setDefaultButton(crear);
        recargar();
        pack();
        setLocationRelativeTo(padre);
    }

    private void recargar() {
        modelo.setRowCount(0);
        for (Torneo t : app.torneoServicio.listar()) {
            modelo.addRow(new Object[]{t.nombre(), t.deporte(), t.fechaInicio(), t.fechaFin()});
        }
    }

    private void limpiar() {
        nombre.setText("");
        deporte.setSelectedIndex(0);
        inicio.setText("");
        fin.setText("");
        form.limpiar();
    }

    private void crear() {
        try {
            Torneo t = app.torneoServicio.crear(organizador, nombre.getText(), (String) deporte.getSelectedItem(),
                    inicio.getText(), fin.getText());
            limpiar();
            recargar();
            JOptionPane.showMessageDialog(this, "Torneo \"" + t.nombre() + "\" creado.");
        } catch (ValidacionException ex) {
            form.marcar(ex.getErrores());
        } catch (AccesoDenegadoException | RepositorioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() + " No se creó ningún torneo.",
                    "No se pudo crear", JOptionPane.ERROR_MESSAGE);
        }
    }
}
