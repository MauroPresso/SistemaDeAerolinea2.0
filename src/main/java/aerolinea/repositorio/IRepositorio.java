package aerolinea.repositorio;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Contrato generico equivalente al trabajado en las clases 8 a 11. */
public interface IRepositorio<T> {
    T guardar(T objeto);
    List<T> guardarLista(List<T> objetos);
    Optional<T> consultar(Object identidad);
    List<T> consultar();
    List<T> consultar(String nombreQuery, Map<String, ?> parametros);
    boolean eliminar(Object identidad);
}
