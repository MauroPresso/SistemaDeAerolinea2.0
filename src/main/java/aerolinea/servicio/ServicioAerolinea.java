package aerolinea.servicio;

import aerolinea.dominio.*;
import aerolinea.repositorio.*;
import javax.jdo.*;
import java.util.*;

/** Reglas de aplicacion compartidas por Swing y consola. Toda escritura es atomica. */
public final class ServicioAerolinea {
    private static final String NOMBRE = "Aerolinea IFES";
    private final ContextoJdo contexto;
    private final IServicio<Vuelo> vuelos;
    private final IServicio<Pasajero> pasajeros;
    private final IServicio<Tripulante> tripulantes;

    public ServicioAerolinea(ContextoJdo contexto) {
        this.contexto = Objects.requireNonNull(contexto);
        vuelos = new Servicio<>(new RepositorioJdo<>(contexto, Vuelo.class));
        pasajeros = new Servicio<>(new RepositorioJdo<>(contexto, Pasajero.class));
        tripulantes = new Servicio<>(new RepositorioJdo<>(contexto, Tripulante.class));
    }

    public List<Vuelo> listarVuelos() { return vuelos.consultar("VuelosOrdenadosPorDestino", Map.of()); }
    public List<Vuelo> vuelosProgramados() { return vuelos.consultar("VuelosProgramados", Map.of()); }
    public List<Vuelo> vuelosPorDestino(String destino) {
        return vuelos.consultar("VuelosPorDestino", Map.of("destino", normalizarTexto(destino)));
    }
    public Optional<Vuelo> buscarVuelo(String numero) { return vuelos.consultar(normalizarNumero(numero)); }
    public List<Pasajero> listarPasajeros() {
        List<Pasajero> lista = pasajeros.consultar();
        lista.sort(Comparator.naturalOrder());
        return lista;
    }
    public List<Pasajero> pasajerosPorApellido(String apellido) {
        return pasajeros.consultar("PasajerosPorApellido", Map.of("apellido", normalizarTexto(apellido)));
    }
    public Optional<Pasajero> buscarPasajero(int dni) { return pasajeros.consultar(dni); }
    public List<Tripulante> listarTripulantes() {
        List<Tripulante> lista = tripulantes.consultar();
        lista.sort(Comparator.naturalOrder());
        return lista;
    }
    public int totalAsientosOcupadosProgramados() {
        return vuelosProgramados().stream().mapToInt(Vuelo::getAsientosOcupados).sum();
    }
    public Optional<Aerolinea> consultarAerolinea() {
        return new RepositorioJdo<>(contexto, Aerolinea.class).consultar(NOMBRE);
    }

    public void registrarVuelo(Vuelo vuelo) {
        Objects.requireNonNull(vuelo);
        contexto.transaccion(pm -> {
            if (buscar(pm, Vuelo.class, vuelo.getNumero()) != null)
                throw new IllegalArgumentException("Ya existe un vuelo con ese numero.");
            Aerolinea aerolinea = obtenerAerolinea(pm);
            aerolinea.agregarVuelo(pm.makePersistent(vuelo));
            return null;
        });
    }

    public void registrarPersona(Persona persona) {
        Objects.requireNonNull(persona);
        contexto.transaccion(pm -> {
            if (buscar(pm, Persona.class, persona.getDni()) != null)
                throw new IllegalArgumentException("Ya existe una persona con ese DNI.");
            obtenerAerolinea(pm).registrarPersona(pm.makePersistent(persona));
            return null;
        });
    }

    public void reservar(int dni, String numero) {
        contexto.transaccion(pm -> {
            Vuelo vuelo = exigir(pm, Vuelo.class, normalizarNumero(numero));
            Pasajero pasajero = exigir(pm, Pasajero.class, dni);
            vuelo.reservarPasajero(pasajero);
            return null;
        });
    }

    public void cancelarReserva(int dni, String numero) {
        contexto.transaccion(pm -> {
            Vuelo vuelo = exigir(pm, Vuelo.class, normalizarNumero(numero));
            Pasajero pasajero = exigir(pm, Pasajero.class, dni);
            if (!vuelo.cancelarReserva(pasajero)) throw new IllegalArgumentException("No existe esa reserva.");
            return null;
        });
    }

    public void asignarTripulante(int dni, String numero) {
        contexto.transaccion(pm -> {
            Vuelo vuelo = exigir(pm, Vuelo.class, normalizarNumero(numero));
            if (vuelo.getEstado() != EstadoVuelo.PROGRAMADO)
                throw new IllegalArgumentException("La tripulacion se asigna a vuelos programados.");
            vuelo.agregarTripulante(exigir(pm, Tripulante.class, dni));
            return null;
        });
    }

    public void actualizarPasajero(int dni, String nombre, String apellido, String pasaporte) {
        contexto.transaccion(pm -> {
            Pasajero pasajero = exigir(pm, Pasajero.class, dni);
            if ((pasaporte == null || pasaporte.isBlank()) && pasajero.getVuelosReservados().stream()
                    .anyMatch(v -> v.getEstado() == EstadoVuelo.PROGRAMADO
                        && v instanceof VueloInternacional internacional && internacional.isRequierePasaporte()))
                throw new IllegalArgumentException("Hay una reserva internacional que requiere este pasaporte.");
            pasajero.setNombre(nombre);
            pasajero.setApellido(apellido);
            Pasaporte anterior = pasajero.getPasaporte();
            pasajero.setNumeroPasaporte(pasaporte);
            if (anterior != null && pasajero.getPasaporte() == null) pm.deletePersistent(anterior);
            return null;
        });
    }

    public void actualizarVuelo(String numero, String origen, String destino, String fecha, int capacidad) {
        contexto.transaccion(pm -> {
            Vuelo vuelo = exigir(pm, Vuelo.class, normalizarNumero(numero));
            if (vuelo.getEstado() != EstadoVuelo.PROGRAMADO)
                throw new IllegalArgumentException("Solo se pueden editar vuelos programados.");
            vuelo.setOrigen(origen);
            vuelo.setDestino(destino);
            vuelo.setFecha(fecha);
            vuelo.setCapacidad(capacidad);
            return null;
        });
    }

    public void cambiarEstado(String numero, EstadoVuelo estado) {
        Objects.requireNonNull(estado);
        contexto.transaccion(pm -> {
            Vuelo vuelo = exigir(pm, Vuelo.class, normalizarNumero(numero));
            if (vuelo.getEstado() != EstadoVuelo.PROGRAMADO && vuelo.getEstado() != estado)
                throw new IllegalArgumentException("No se puede reabrir un vuelo iniciado o cancelado.");
            vuelo.setEstado(estado);
            return null;
        });
    }

    public void eliminarVuelo(String numero) {
        contexto.transaccion(pm -> {
            Vuelo vuelo = exigir(pm, Vuelo.class, normalizarNumero(numero));
            if (!vuelo.getPasajeros().isEmpty()) throw new IllegalArgumentException("Primero debe cancelar las reservas del vuelo.");
            if (vuelo.getEstado() == EstadoVuelo.EN_VUELO) throw new IllegalArgumentException("No se puede eliminar un vuelo iniciado.");
            Aerolinea aerolinea = buscar(pm, Aerolinea.class, NOMBRE);
            if (aerolinea != null) aerolinea.quitarVuelo(vuelo);
            pm.deletePersistent(vuelo);
            return null;
        });
    }

    public void eliminarPasajero(int dni) {
        contexto.transaccion(pm -> {
            Pasajero pasajero = exigir(pm, Pasajero.class, dni);
            if (!pasajero.getVuelosReservados().isEmpty()) throw new IllegalArgumentException("Primero debe cancelar las reservas del pasajero.");
            Aerolinea aerolinea = buscar(pm, Aerolinea.class, NOMBRE);
            if (aerolinea != null) aerolinea.quitarPersona(pasajero);
            pm.deletePersistent(pasajero);
            return null;
        });
    }

    private static Aerolinea obtenerAerolinea(PersistenceManager pm) {
        Aerolinea aerolinea = buscar(pm, Aerolinea.class, NOMBRE);
        return aerolinea == null ? pm.makePersistent(new Aerolinea(NOMBRE)) : aerolinea;
    }

    private static <T> T buscar(PersistenceManager pm, Class<T> clase, Object id) {
        try { return pm.getObjectById(clase, id); }
        catch (JDOObjectNotFoundException error) { return null; }
    }
    private static <T> T exigir(PersistenceManager pm, Class<T> clase, Object id) {
        T encontrado = buscar(pm, clase, id);
        if (encontrado == null) throw new IllegalArgumentException("No existe " + clase.getSimpleName() + " con identidad " + id + ".");
        return encontrado;
    }
    private static String normalizarNumero(String numero) {
        if (numero == null || numero.isBlank()) throw new IllegalArgumentException("Debe indicar el numero de vuelo.");
        return numero.trim().toUpperCase(Locale.ROOT);
    }
    private static String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Debe indicar el texto de busqueda.");
        return texto.trim().toLowerCase(Locale.ROOT);
    }
}
