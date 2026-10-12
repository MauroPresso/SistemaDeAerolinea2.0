package aerolinea.util;

import java.util.Comparator;

import aerolinea.dominio.Vuelo;

public class ComparadorVueloPorDestino implements Comparator<Vuelo> {

    @Override
    public int compare(Vuelo vuelo1, Vuelo vuelo2) {
        int comparacionDestino = vuelo1.getDestino().compareToIgnoreCase(vuelo2.getDestino());

        if (comparacionDestino != 0) {
            return comparacionDestino;
        }

        return vuelo1.getNumero().compareToIgnoreCase(vuelo2.getNumero());
    }
}
