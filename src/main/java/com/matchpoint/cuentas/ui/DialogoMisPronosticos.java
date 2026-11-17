package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.Aplicacion;
import com.matchpoint.cuentas.modelo.Evento;
import com.matchpoint.cuentas.modelo.Pronostico;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.FechasEvento;
import com.matchpoint.cuentas.servicio.PronosticoServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.awt.Dimension;
import java.awt.Window;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * HU-12: lista de los pronósticos propios. Antes del cierre se puede cambiar la
 * opción; después del cierre el cambio se rechaza con un mensaje claro.
 */
public class DialogoMisPronosticos extends JDialog {

    private final Aplicacion app;
    private final Usuario participante;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Evento", "Mi pronóstico", "Cierre", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JComboBox<String> nuevaOpcion = new JComboBox<>();
    private final Formulario form = new Formulario();
    private List<Pronostico> mios = List.of();

    public DialogoMisPronosticos(Window padre, Aplicacion app, Usuario participante) {
        super(padre, "Mis pronósticos", ModalityType.APPLICATION_MODAL);
        this.app = app;
        this.participante = participante;
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> cargarOpciones());
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(560, 180));

        JButton cambiar = new JButton("Cambiar pronóstico");
        JButton cerrar = new JButton("Cerrar");
        cambiar.addActionListener(e -> cambiar());
        cerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel();
        botones.add(cambiar);
        botones.add(cerrar);

        form.ancho(scroll)
                .campo(PronosticoServicio.CAMPO_OPCION, "Nueva opción", nuevaOpcion)
                .campo(PronosticoServicio.CAMPO_EVENTO, "", new JPanel());
        setContentPane(form.cerrar(botones));
        recargar();
        pack();
        setLocationRelativeTo(padre);
    }

    private void recargar() {
        mios = app.pronosticos.misPronosticos(participante);
        modelo.setRowCount(0);
        LocalDateTime ahora = LocalDateTime.now();
        for (Pronostico p : mios) {
            Evento e = app.repositorioEventos.buscarPorId(p.eventoId()).orElse(null);
            modelo.addRow(new Object[]{e == null ? "?" : e.nombre(), p.opcion(),
                    e == null ? "" : FechasEvento.escribir(e.cierre()), e == null ? "" : e.estado(ahora)});
        }
    }

    private void cargarOpciones() {
        int fila = tabla.getSelectedRow();
        form.limpiar();
        if (fila < 0) {
            nuevaOpcion.setModel(new DefaultComboBoxModel<>());
            return;
        }
        app.repositorioEventos.buscarPorId(mios.get(fila).eventoId()).ifPresent(e ->
                nuevaOpcion.setModel(new DefaultComboBoxModel<>(e.opciones().toArray(new String[0]))));
    }

    private void cambiar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            form.marcar(java.util.Map.of(PronosticoServicio.CAMPO_EVENTO, "Seleccione uno de sus pronósticos."));
            return;
        }
        try {
            Pronostico p = app.pronosticos.cambiar(participante, mios.get(fila).id(),
                    (String) nuevaOpcion.getSelectedItem());
            recargar();
            form.limpiar();
            JOptionPane.showMessageDialog(this, "Pronóstico cambiado a \"" + p.opcion() + "\". No se cobró de nuevo.");
        } catch (ValidacionException ex) {
            form.marcar(ex.getErrores());
        } catch (AccesoDenegadoException | RepositorioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo cambiar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
