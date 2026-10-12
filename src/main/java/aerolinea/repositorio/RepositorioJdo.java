package aerolinea.repositorio;

import javax.jdo.*;
import java.util.*;

/** Guarda objetos individuales y devuelve objetos detached, nunca un PM abierto. */
public class RepositorioJdo<T> implements IRepositorio<T> {
    private final ContextoJdo contexto;
    private final Class<T> clase;

    public RepositorioJdo(ContextoJdo contexto, Class<T> clase) {
        this.contexto = Objects.requireNonNull(contexto);
        this.clase = Objects.requireNonNull(clase);
    }

    @Override public T guardar(T objeto) {
        Objects.requireNonNull(objeto, "El objeto no puede ser nulo.");
        return contexto.transaccion(pm -> pm.makePersistent(objeto));
    }

    @Override public List<T> guardarLista(List<T> objetos) {
        Objects.requireNonNull(objetos, "La lista no puede ser nula.");
        if (objetos.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("La lista contiene un objeto nulo.");
        return contexto.transaccion(pm -> new ArrayList<>(pm.makePersistentAll(objetos)));
    }

    @Override public Optional<T> consultar(Object identidad) {
        T objeto = contexto.transaccion(pm -> {
            try { return pm.getObjectById(clase, identidad); }
            catch (JDOObjectNotFoundException error) { return null; }
        });
        return Optional.ofNullable(objeto);
    }

    @Override public List<T> consultar() {
        return contexto.transaccion(pm -> {
            Query<T> query = pm.newQuery(clase);
            try { return copiarResultado(query.execute()); }
            finally { query.closeAll(); }
        });
    }

    @Override public List<T> consultar(String nombreQuery, Map<String, ?> parametros) {
        Objects.requireNonNull(nombreQuery);
        Objects.requireNonNull(parametros);
        return contexto.transaccion(pm -> {
            Query<T> query = pm.newNamedQuery(clase, nombreQuery);
            try { return copiarResultado(query.executeWithMap(parametros)); }
            finally { query.closeAll(); }
        });
    }

    @Override public boolean eliminar(Object identidad) {
        return contexto.transaccion(pm -> {
            try { pm.deletePersistent(pm.getObjectById(clase, identidad)); return true; }
            catch (JDOObjectNotFoundException error) { return false; }
        });
    }

    @SuppressWarnings("unchecked")
    private List<T> copiarResultado(Object resultado) {
        return new ArrayList<>((Collection<T>) resultado);
    }
}
