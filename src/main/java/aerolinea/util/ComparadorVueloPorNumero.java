package aerolinea.util;

import java.util.Comparator;

import aerolinea.dominio.Vuelo;

public class ComparadorVueloPorNumero implements Comparator<Vuelo> {

    @Override
    public int compare(Vuelo vuelo1, Vuelo vuelo2) {
        return vuelo1.getNumero().compareToIgnoreCase(vuelo2.getNumero());
    }
}
