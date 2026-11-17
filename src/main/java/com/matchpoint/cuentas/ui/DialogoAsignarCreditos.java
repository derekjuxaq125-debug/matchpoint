package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.Aplicacion;
import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.Usuario;
import com.matchpoint.cuentas.repositorio.RepositorioException;
import com.matchpoint.cuentas.servicio.AccesoDenegadoException;
import com.matchpoint.cuentas.servicio.CreditoServicio;
import com.matchpoint.cuentas.servicio.ValidacionException;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** HU-07: lista de usuarios con su saldo inicial y asignación con confirmación. Solo organizador. */
public class DialogoAsignarCreditos extends JDialog {

    private final Aplicacion app;
    private final Usuario organizador;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Identificación", "Nombre", "Saldo inicial"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JTextField cantidad = new JTextField(10);
    private final Formulario form = new Formulario();
    private List<Usuario> usuarios = List.of();

    public DialogoAsignarCreditos(Window padre, Aplicacion app, Usuario organizador) {
        super(padre, "Asignar créditos iniciales", ModalityType.APPLICATION_MODAL);
        this.app = app;
        this.organizador = organizador;
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(460, 200));
        tabla.getSelectionModel().addListSelectionListener(e -> form.limpiar());

        JButton asignar = new JButton("Asignar");
        JButton cerrar = new JButton("Cerrar");
        JPanel botones = new JPanel();
        botones.add(asignar);
        botones.add(cerrar);
        asignar.addActionListener(e -> asignar());
        cerrar.addActionListener(e -> dispose());

        form.ancho(scroll).campo(CreditoServicio.CAMPO_CANTIDAD, "Cantidad de créditos *", cantidad);
        form.campo(CreditoServicio.CAMPO_USUARIO, "", new JPanel());
        setContentPane(form.cerrar(botones));
        getRootPane().setDefaultButton(asignar);
        recargar();
        pack();
        setLocationRelativeTo(padre);
    }

    private void recargar() {
        usuarios = app.repositorioUsuarios.listar();
        modelo.setRowCount(0);
        for (Usuario u : usuarios) {
            Object inicial = app.creditos.saldoInicialDe(u.identificacion()).map(Object.class::cast).orElse("Sin asignar");
            modelo.addRow(new Object[]{u.identificacion(), u.nombre(), inicial});
        }
    }

    private void asignar() {
        int fila = tabla.getSelectedRow();
        String usuarioId = fila < 0 ? null : usuarios.get(fila).identificacion();
        try {
            int valor = app.creditos.validar(organizador, usuarioId, cantidad.getText());
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Asignar " + valor + " créditos a " + usuarios.get(fila).nombre() + "?",
                    "Confirmar asignación", JOptionPane.YES_NO_OPTION);
            if (respuesta != JOptionPane.YES_OPTION) {
                return; // "No": no se registra nada y se conservan los datos del formulario
            }
            Movimiento m = app.creditos.asignarInicial(organizador, usuarioId, cantidad.getText());
            form.limpiar();
            cantidad.setText("");
            recargar();
            JOptionPane.showMessageDialog(this, "Saldo inicial registrado: " + m.cantidad()
                    + " créditos. Saldo actual: " + m.saldoResultante() + ".");
        } catch (ValidacionException ex) {
            form.marcar(ex.getErrores());
        } catch (AccesoDenegadoException | RepositorioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() + " El saldo no se modificó.",
                    "No se pudo asignar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
