package aerolinea.util;

public class CalculadoraTarifas {

    public double calcularTotal(double tarifaPorPasajero, int cantidadPasajeros) {
        if (tarifaPorPasajero < 0 || cantidadPasajeros < 0) {
            throw new IllegalArgumentException("La tarifa y la cantidad no pueden ser negativas.");
        }

        return tarifaPorPasajero * cantidadPasajeros;
    }

    public double calcularTotal(double tarifaPorPasajero,
                                int cantidadPasajeros,
                                double porcentajeAdicional) {

        if (porcentajeAdicional < 0) {
            throw new IllegalArgumentException("El porcentaje adicional no puede ser negativo.");
        }

        double subtotal = calcularTotal(tarifaPorPasajero, cantidadPasajeros);
        return subtotal + subtotal * porcentajeAdicional / 100.0;
    }

    public double calcularTotal(double... importes) {
        if (importes == null) {
            throw new IllegalArgumentException("Los importes no pueden ser nulos.");
        }

        double total = 0.0;

        for (double importe : importes) {
            if (importe < 0) {
                throw new IllegalArgumentException("Los importes no pueden ser negativos.");
            }

            total += importe;
        }

        return total;
    }
}
