package aerolinea.dominio;

import java.util.ArrayList;
import javax.jdo.annotations.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import aerolinea.excepcion.VueloNoDisponibleException;
import aerolinea.util.ComparadorVueloPorDestino;
import aerolinea.util.ComparadorVueloPorNumero;

@PersistenceCapable(table = "AEROLINEAS", detachable = "true")
public class Aerolinea {

    @PrimaryKey
    @Column(name = "NOMBRE", length = 100)
    private String nombre;

    @Persistent(table = "AEROLINEA_VUELOS", defaultFetchGroup = "true")
    @Join(column = "AEROLINEA_NOMBRE")
    @Element(column = "VUELO_NUMERO")
    @Order(column = "ORDEN_VUELO")
    private List<Vuelo> vuelos;

    @Persistent(table = "AEROLINEA_PERSONAS", defaultFetchGroup = "true")
    @Join(column = "AEROLINEA_NOMBRE")
    @Element(column = "PERSONA_DNI")
    @Order(column = "ORDEN_PERSONA")
    private List<Persona> personas;

    protected Aerolinea() {
        vuelos = new ArrayList<>();
        personas = new ArrayList<>();
    }

    public void quitarVuelo(Vuelo vuelo) { vuelos.remove(vuelo); }
    public void quitarPersona(Persona persona) { personas.remove(persona); }

    public Aerolinea(String nombre) {
        this(nombre, new ArrayList<>(), new ArrayList<>());
    }

    public Aerolinea(String nombre, List<Vuelo> vuelosIniciales) {
        this(nombre, vuelosIniciales, new ArrayList<>());
    }

    public Aerolinea(String nombre,
                     List<Vuelo> vuelosIniciales,
                     List<Persona> personasIniciales) {

        this.nombre = validarTextoObligatorio(nombre, "nombre de la aerolinea");
        if (this.nombre.length() > 100) throw new IllegalArgumentException("El nombre de la aerolinea es demasiado largo.");

        if (vuelosIniciales == null) {
            throw new IllegalArgumentException("La lista inicial de vuelos no puede ser nula.");
        }

        if (personasIniciales == null) {
            throw new IllegalArgumentException("La lista inicial de personas no puede ser nula.");
        }

        this.vuelos = new ArrayList<>(vuelosIniciales);
        this.personas = new ArrayList<>();

        reconstruirPersonasDesdeVuelos();

        for (Persona persona : personasIniciales) {
            registrarPersona(persona);
        }
    }
    
    private void reconstruirPersonasDesdeVuelos() {
        for (Vuelo vuelo : vuelos) {
            for (Pasajero pasajero : vuelo.getPasajeros()) {
                registrarPersonaAsociada(pasajero);
            }

            for (Tripulante tripulante : vuelo.getTripulacion()) {
                registrarPersonaAsociada(tripulante);
            }
        }
    }

    private void registrarPersonaAsociada(Persona persona) {
        if (persona != null) registrarPersona(persona);
    }

    public String getNombre() {
        return nombre;
    }

    public void agregarVuelo(Vuelo vuelo) {
        if (vuelo == null) {
            throw new IllegalArgumentException("El vuelo no puede ser nulo.");
        }

        if (buscarVueloPorNumero(vuelo.getNumero()) != null) {
            throw new IllegalArgumentException("Ya existe un vuelo con el numero " + vuelo.getNumero() + ".");
        }

        vuelos.add(vuelo);
    }

    public boolean registrarPersona(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser nula.");
        }

        if (personas.contains(persona)) {
            return false;
        }

        personas.add(persona);
        return true;
    }

    public Persona buscarPersonaPorDni(int dni) {
        return getPersonasPorDni().get(dni);
    }

    public Vuelo buscarVueloPorNumero(String numeroVuelo) {
        if (numeroVuelo == null) {
            return null;
        }

        return vuelos.stream()
                .filter(vuelo -> vuelo.getNumero().equalsIgnoreCase(numeroVuelo.trim()))
                .findFirst()
                .orElse(null);
    }

    public void reservarVuelo(int dniPasajero, String numeroVuelo) throws VueloNoDisponibleException {
        Persona persona = buscarPersonaPorDni(dniPasajero);

        if (persona == null) {
            System.out.println("No existe una persona registrada con DNI " + dniPasajero);
            return;
        }

        if (!(persona instanceof Pasajero)) {
            System.out.println("La persona con DNI " + dniPasajero + " no es pasajero.");
            return;
        }

        Vuelo vuelo = buscarVueloPorNumero(numeroVuelo);

        if (vuelo == null) {
            System.out.println("No existe el vuelo numero " + numeroVuelo);
            return;
        }

        Pasajero pasajero = (Pasajero) persona;
        pasajero.reservarVuelo(vuelo);

    }

    public void cancelarReserva(int dniPasajero, String numeroVuelo) {
        Persona persona = buscarPersonaPorDni(dniPasajero);

        if (!(persona instanceof Pasajero)) {
            System.out.println("No existe un pasajero con DNI " + dniPasajero);
            return;
        }

        Vuelo vuelo = buscarVueloPorNumero(numeroVuelo);

        if (vuelo == null) {
            System.out.println("No existe el vuelo numero " + numeroVuelo);
            return;
        }

        Pasajero pasajero = (Pasajero) persona;
        boolean cancelada = vuelo.cancelarReserva(pasajero);

        if (cancelada) {
            System.out.println("Reserva cancelada correctamente.");

        } else {
            System.out.println("El pasajero no tenia reservado ese vuelo.");
        }
    }

    public ArrayList<Vuelo> getVuelos() {
        return new ArrayList<>(vuelos);
    }

    public HashMap<Integer, Persona> getPersonasPorDni() {
        HashMap<Integer, Persona> indice = new HashMap<>();
        personas.forEach(persona -> indice.put(persona.getDni(), persona));
        return indice;
    }

    public HashSet<Persona> getPasajerosConReservaActiva() {
        HashSet<Persona> activos = new HashSet<>();
        personas.stream().filter(p -> p instanceof Pasajero pasajero && pasajero.tieneReservaActiva())
                .forEach(activos::add);
        return activos;
    }

    public List<Vuelo> obtenerVuelosProgramadosStream() {
        return vuelos.stream()
                .filter(vuelo -> vuelo.getEstado() == EstadoVuelo.PROGRAMADO)
                .collect(Collectors.toList());
    }

    public void mostrarVuelosProgramadosStream() {
        obtenerVuelosProgramadosStream()
                .forEach(Vuelo::mostrarInfo);
    }

    public void mostrarVuelosOrdenadosPorDestinoStream() {
        vuelos.stream()
                .sorted((vuelo1, vuelo2) -> vuelo1.getDestino().compareToIgnoreCase(vuelo2.getDestino()))
                .forEach(Vuelo::mostrarInfo);
    }

    public int calcularTotalAsientosOcupadosProgramadosStream() {
        return vuelos.stream()
                .filter(vuelo -> vuelo.getEstado() == EstadoVuelo.PROGRAMADO)
                .mapToInt(Vuelo::getAsientosOcupados)
                .sum();
    }

    public ArrayList<Persona> obtenerPersonasOrdenadasPorApellido() {
        ArrayList<Persona> personas = new ArrayList<>(getPersonasPorDni().values());
        Collections.sort(personas);
        return personas;
    }

    public ArrayList<Vuelo> obtenerVuelosOrdenadosPorNumeroComparable() {
        ArrayList<Vuelo> vuelosOrdenados = new ArrayList<>(vuelos);
        Collections.sort(vuelosOrdenados);
        return vuelosOrdenados;
    }

    public ArrayList<Vuelo> obtenerVuelosOrdenadosPorDestino() {
        ArrayList<Vuelo> vuelosOrdenados = new ArrayList<>(vuelos);
        Collections.sort(vuelosOrdenados, new ComparadorVueloPorDestino());
        return vuelosOrdenados;
    }

    public ArrayList<Vuelo> obtenerVuelosOrdenadosPorNumeroComparator() {
        ArrayList<Vuelo> vuelosOrdenados = new ArrayList<>(vuelos);
        Collections.sort(vuelosOrdenados, new ComparadorVueloPorNumero());
        return vuelosOrdenados;
    }

    public void mostrarVuelos() {
        System.out.println("Vuelos de " + nombre + ":");

        if (vuelos.isEmpty()) {
            System.out.println("No hay vuelos cargados.");
            return;
        }

        for (Vuelo vuelo : vuelos) {
            vuelo.mostrarInfo();
        }
    }

    public void mostrarPasajerosConReservaActiva() {
        System.out.println("Pasajeros con reserva activa:");
        HashSet<Persona> pasajerosConReservaActiva = getPasajerosConReservaActiva();

        if (pasajerosConReservaActiva.isEmpty()) {
            System.out.println("No hay pasajeros con reservas activas.");
            return;
        }

        for (Persona persona : pasajerosConReservaActiva) {
            persona.mostrarInfo();
        }
    }

    public void operarVuelosPolimorficamente() {
        for (Vuelo vuelo : vuelos) {
            vuelo.embarcar();
            vuelo.mostrarInfo();
        }
    }

    private String validarTextoObligatorio(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede estar vacio.");
        }

        return valor.trim();
    }
}
