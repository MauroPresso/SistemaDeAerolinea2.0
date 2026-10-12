package aerolinea.excepcion;

/** Informa un fallo de persistencia; la operacion ya fue revertida. */
public class PersistenciaException extends RuntimeException {
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
