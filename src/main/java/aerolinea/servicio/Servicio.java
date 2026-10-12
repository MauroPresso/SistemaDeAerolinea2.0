package aerolinea.servicio;

import aerolinea.repositorio.IRepositorio;
import java.util.*;

/** Servicio generico basado en composicion y en el contrato del repositorio. */
public final class Servicio<T> implements IServicio<T> {
    private final IRepositorio<T> repositorio;
    public Servicio(IRepositorio<T> repositorio) { this.repositorio = Objects.requireNonNull(repositorio); }
    @Override public T guardar(T objeto) { return repositorio.guardar(objeto); }
    @Override public List<T> guardarLista(List<T> objetos) { return repositorio.guardarLista(objetos); }
    @Override public Optional<T> consultar(Object identidad) { return repositorio.consultar(identidad); }
    @Override public List<T> consultar() { return repositorio.consultar(); }
    @Override public List<T> consultar(String nombreQuery, Map<String, ?> parametros) { return repositorio.consultar(nombreQuery, parametros); }
    @Override public boolean eliminar(Object identidad) { return repositorio.eliminar(identidad); }
}
