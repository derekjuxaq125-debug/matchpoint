package com.matchpoint.torneos;

import com.matchpoint.torneos.repositorio.TorneoRepositorioArchivo;
import com.matchpoint.torneos.servicio.TorneoServicio;
import com.matchpoint.torneos.ui.VentanaTorneos;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada: abre la lista de torneos (HU-03 Crear torneo). */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // se usa el aspecto por defecto
        }
        TorneoServicio servicio = new TorneoServicio(new TorneoRepositorioArchivo(Path.of("torneos.txt")));
        SwingUtilities.invokeLater(() -> new VentanaTorneos(servicio).setVisible(true));
    }
}
