package com.matchpoint.torneos.modelo;

/** Deportes disponibles en la lista desplegable del formulario (HU-03). */
public enum Deporte {
    FUTBOL("Fútbol"),
    BALONCESTO("Baloncesto"),
    TENIS("Tenis"),
    VOLEIBOL("Voleibol"),
    BEISBOL("Béisbol"),
    FUTBOL_SALA("Fútbol sala"),
    ESPORTS("eSports");

    private final String etiqueta;

    Deporte(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
