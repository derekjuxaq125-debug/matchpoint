package com.matchpoint.cuentas.modelo;

/** Tipos de movimiento del libro de créditos (HU-09). */
public enum TipoMovimiento {
    ASIGNACION_INICIAL("Asignación inicial"),   // HU-07
    PARTICIPACION("Participación"),             // HU-11 (débito por pronóstico)
    PREMIO("Premio"),                           // HU-20 (Sprint 3)
    AJUSTE("Ajuste"),                           // HU-23 (Sprint 3)
    REVERSION("Reversión");                     // HU-22 / HU-23 (Sprint 3)

    private final String etiqueta;

    TipoMovimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Ajustes y reversiones exigen justificación (HU-23). */
    public boolean exigeJustificacion() {
        return this == AJUSTE || this == REVERSION;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
