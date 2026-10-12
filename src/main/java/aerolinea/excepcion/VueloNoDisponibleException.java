package aerolinea.excepcion;

public class VueloNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VueloNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public VueloNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
