package aerolinea.servicio;

import java.util.*;

public interface IServicio<T> {
    T guardar(T objeto);
    List<T> guardarLista(List<T> objetos);
    Optional<T> consultar(Object identidad);
    List<T> consultar();
    List<T> consultar(String nombreQuery, Map<String, ?> parametros);
    boolean eliminar(Object identidad);
}
