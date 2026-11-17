package com.matchpoint.cuentas.ui;

import com.matchpoint.cuentas.Aplicacion;
import com.matchpoint.cuentas.modelo.ConsultaSaldo;
import com.matchpoint.cuentas.modelo.Movimiento;
import com.matchpoint.cuentas.modelo.Usuario;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** HU-08: saldo actual del participante y verificación contra la suma de sus movimientos. */
public class DialogoSaldo extends JDialog {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public DialogoSaldo(Window padre, Aplicacion app, Usuario participante) {
        super(padre, "Mi saldo de créditos", ModalityType.APPLICATION_MODAL);
        ConsultaSaldo c = app.saldos.consultar(participante);

        JLabel saldo = new JLabel(c.saldo() + " créditos");
        saldo.setFont(saldo.getFont().deriveFont(Font.BOLD, 26f));
        JLabel verificacion = new JLabel(c.coincide()
                ? "✔ Coincide con la suma de sus " + c.movimientos().size() + " movimientos (" + c.sumaMovimientos() + ")."
                : "✘ No coincide: la suma de movimientos es " + c.sumaMovimientos() + ". Informe al auditor.");
        verificacion.setForeground(c.coincide() ? new Color(0, 120, 60) : Color.RED);

        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"Fecha", "Tipo", "Cantidad", "Saldo después", "Origen"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        for (Movimiento m : c.movimientos()) {
            modelo.addRow(new Object[]{m.registrado().format(FORMATO), m.tipo(),
                    (m.cantidad() > 0 ? "+" : "") + m.cantidad(), m.saldoResultante(), m.origen()});
        }
        JScrollPane tabla = new JScrollPane(new JTable(modelo));
        tabla.setPreferredSize(new Dimension(600, 180));

        JPanel arriba = new JPanel(new BorderLayout(4, 4));
        arriba.add(saldo, BorderLayout.NORTH);
        arriba.add(verificacion, BorderLayout.CENTER);
        arriba.add(new JLabel("Créditos virtuales: sin valor monetario, sin retiro ni conversión a dinero."),
                BorderLayout.SOUTH);

        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel();
        botones.add(cerrar);

        JPanel contenido = new JPanel(new BorderLayout(8, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contenido.add(arriba, BorderLayout.NORTH);
        contenido.add(tabla, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);
        setContentPane(contenido);
        pack();
        setLocationRelativeTo(padre);
    }
}
