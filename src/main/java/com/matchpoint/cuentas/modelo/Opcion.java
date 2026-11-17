package com.matchpoint.cuentas.modelo;

/** Opciones de menú del sistema. Cada rol (ver {@link Rol}) habilita un subconjunto. */
public enum Opcion {
    LISTAR_TORNEOS_Y_EVENTOS("Listar torneos y eventos"),       // HU-05 (Johan)
    VER_OPCIONES_PRONOSTICO("Ver opciones de pronóstico"),      // HU-06 (Johan)
    REGISTRAR_PRONOSTICO("Registrar pronóstico"),               // HU-10
    CAMBIAR_PRONOSTICO("Mis pronósticos (cambiar antes del cierre)"), // HU-12
    CONSULTAR_SALDO("Consultar mi saldo"),                      // HU-08 (Johan)
    CREAR_TORNEO("Crear torneo"),                               // HU-03 (Johan)
    REGISTRAR_EVENTO("Registrar evento y opciones"),            // HU-04
    ASIGNAR_CREDITOS("Asignar créditos iniciales"),             // HU-07 (Julian)
    REPORTE_CONSOLIDADO("Reporte consolidado"),                 // HU-24 (Johan)
    DETECTAR_INCONSISTENCIAS("Detectar inconsistencias");       // HU-25 (Johan)

    private final String etiqueta;

    Opcion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
