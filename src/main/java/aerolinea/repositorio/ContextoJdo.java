package aerolinea.repositorio;

import aerolinea.excepcion.PersistenciaException;
import javax.jdo.*;
import java.util.*;
import java.util.function.Function;

/** Una fabrica por aplicacion; un PersistenceManager por operacion. */
public final class ContextoJdo implements AutoCloseable {
    private final PersistenceManagerFactory factory;

    public ContextoJdo(String unidad) { this(unidad, Map.of()); }

    public ContextoJdo(String unidad, Map<String, ?> ajustes) {
        factory = JDOHelper.getPersistenceManagerFactory(ajustes, unidad);
    }

    public static ContextoJdo desdeEntorno(String unidad) {
        Map<String, Object> ajustes = new HashMap<>();
        agregarEntorno(ajustes, "AEROLINEA_DB_URL", "javax.jdo.option.ConnectionURL");
        agregarEntorno(ajustes, "AEROLINEA_DB_USER", "javax.jdo.option.ConnectionUserName");
        agregarEntorno(ajustes, "AEROLINEA_DB_PASSWORD", "javax.jdo.option.ConnectionPassword");
        return new ContextoJdo(unidad, ajustes);
    }

    private static void agregarEntorno(Map<String, Object> ajustes, String variable, String propiedad) {
        String valor = System.getenv(variable);
        if (valor != null) ajustes.put(propiedad, valor);
    }

    public <R> R transaccion(Function<PersistenceManager, R> operacion) {
        PersistenceManager pm = factory.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
            pm.getFetchPlan().setGroup(FetchPlan.ALL).setMaxFetchDepth(-1);
            tx.begin();
            R resultado = operacion.apply(pm);
            // Carga y separa el resultado dentro de la transaccion. Si falla,
            // todavia es posible revertir tambien las escrituras de la operacion.
            pm.flush();
            R copia = separar(pm, resultado);
            tx.commit();
            return copia;
        } catch (JDOException error) {
            throw new PersistenciaException("No se pudo completar la operacion en la base de datos.", error);
        } finally {
            try { if (tx.isActive()) tx.rollback(); }
            finally { pm.close(); }
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R separar(PersistenceManager pm, R resultado) {
        if (resultado == null) return null;
        if (resultado instanceof Collection<?> coleccion) {
            return (R) new ArrayList<>(pm.detachCopyAll(coleccion));
        }
        return JDOHelper.isPersistent(resultado) ? pm.detachCopy(resultado) : resultado;
    }

    @Override public void close() { if (!factory.isClosed()) factory.close(); }
}
