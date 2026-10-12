package aerolinea.dominio;

import aerolinea.excepcion.VueloNoDisponibleException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.time.LocalDate;
import javax.jdo.annotations.*;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(
        table = "VUELOS",
        identityType = IdentityType.APPLICATION,
        detachable = "true")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
@Version(strategy = VersionStrategy.VERSION_NUMBER, column = "VERSION")
@Queries({
    @Query(name = "VuelosProgramados", value = "SELECT FROM aerolinea.dominio.Vuelo WHERE estado == aerolinea.dominio.EstadoVuelo.PROGRAMADO ORDER BY numero ASC"),
    @Query(name = "VuelosPorDestino", value = "SELECT FROM aerolinea.dominio.Vuelo WHERE destino.toLowerCase() == :destino ORDER BY numero ASC"),
    @Query(name = "VuelosOrdenadosPorDestino", value = "SELECT FROM aerolinea.dominio.Vuelo ORDER BY destino ASC, numero ASC")
})
public abstract class Vuelo
        implements IOperable, Comparable<Vuelo>, Serializable {

    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent
    @Column(name = "NUMERO", length = 30)
    private String numero;

    @Persistent
    @Column(name = "ORIGEN", length = 100)
    private String origen;

    @Persistent
    @Column(name = "DESTINO", length = 100)
    private String destino;

    @Persistent
    @Column(name = "FECHA", length = 30)
    private String fecha;

    @Persistent
    @Column(name = "CAPACIDAD")
    private int capacidad;

    @Persistent
    @Column(name = "ESTADO")
    private EstadoVuelo estado;

    @Persistent(table = "VUELO_PASAJERO", defaultFetchGroup = "true")
    @Join(column = "VUELO_NUMERO")
    @Element(column = "PASAJERO_DNI")
    @Order(extensions = @Extension(
            vendorName = "datanucleus",
            key = "list-ordering",
            value = "dni ASC"))
    private ArrayList<Pasajero> pasajeros = new ArrayList<>();

    @Persistent(table = "VUELO_TRIPULANTE", defaultFetchGroup = "true")
    @Join(column = "VUELO_NUMERO")
    @Element(column = "TRIPULANTE_DNI")
    @Order(column = "ORDEN_TRIPULANTE")
    private ArrayList<Tripulante> tripulacion = new ArrayList<>();

    protected Vuelo() {
        this.estado = EstadoVuelo.PROGRAMADO;
    }

    public Vuelo(
            String numero,
            String origen,
            String destino,
            String fecha,
            int capacidad) {

        setNumero(numero);
        setOrigen(origen);
        setDestino(destino);
        setFecha(fecha);
        setCapacidad(capacidad);
        this.estado = EstadoVuelo.PROGRAMADO;
    }

    public abstract String getTipo();

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        String nuevo = validarTextoObligatorio(numero, "numero").toUpperCase(Locale.ROOT);
        if (!nuevo.matches("[A-Z0-9-]{1,30}")) throw new IllegalArgumentException("El numero de vuelo debe tener hasta 30 letras, digitos o guiones.");
        if (this.numero != null && !this.numero.equals(nuevo)) throw new IllegalArgumentException("La identidad del vuelo no puede cambiar.");
        this.numero = nuevo;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen =
                validarTextoObligatorio(origen, "origen");
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino =
                validarTextoObligatorio(destino, "destino");
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = LocalDate.parse(validarTextoObligatorio(fecha, "fecha")).toString();
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException(
                    "La capacidad debe ser mayor que cero.");
        }

        if (pasajeros != null
                && capacidad < pasajeros.size()) {

            throw new IllegalArgumentException(
                    "La capacidad no puede ser menor "
                            + "a los asientos ya ocupados.");
        }

        this.capacidad = capacidad;
    }

    public EstadoVuelo getEstado() {
        return estado;
    }

    public void setEstado(EstadoVuelo estado) {
        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado no puede ser nulo.");
        }
        this.estado = estado;
    }

    public List<Pasajero> getPasajeros() {
        return Collections.unmodifiableList(pasajeros);
    }

    public List<Tripulante> getTripulacion() {
        return Collections.unmodifiableList(tripulacion);
    }

    public void reservarPasajero(Pasajero pasajero)
            throws VueloNoDisponibleException {

        if (pasajero == null) {
            throw new IllegalArgumentException(
                    "El pasajero no puede ser nulo.");
        }

        if (pasajeros.contains(pasajero)) throw new IllegalArgumentException("El pasajero ya tiene una reserva en este vuelo.");
        if (this instanceof VueloInternacional internacional && internacional.isRequierePasaporte()
                && pasajero.getNumeroPasaporte().isEmpty())
            throw new IllegalArgumentException("El vuelo requiere un pasaporte registrado.");

        validarDisponibilidadParaReserva();

        pasajeros.add(pasajero);
        pasajero.agregarVueloReservado(this);
    }

    public boolean cancelarReserva(Pasajero pasajero) {
        if (pasajero == null) {
            throw new IllegalArgumentException(
                    "El pasajero no puede ser nulo.");
        }

        if (estado == EstadoVuelo.EN_VUELO) throw new IllegalArgumentException("No se puede cancelar una reserva despues del embarque.");
        boolean eliminado = pasajeros.remove(pasajero);

        if (eliminado) {
            pasajero.quitarVueloReservado(this);
        }

        return eliminado;
    }

    public void agregarTripulante(Tripulante tripulante) {
        if (tripulante == null) {
            throw new IllegalArgumentException(
                    "El tripulante no puede ser nulo.");
        }

        if (!tripulacion.contains(tripulante)) {
            tripulacion.add(tripulante);
        }
    }

    public int getAsientosOcupados() {
        return pasajeros.size();
    }

    public int getAsientosDisponibles() {
        return capacidad - pasajeros.size();
    }

    public boolean hayAsientosDisponibles() {
        return getAsientosDisponibles() > 0;
    }

    @Override
    public void embarcar() {
        if (estado == EstadoVuelo.PROGRAMADO) {
            estado = EstadoVuelo.EN_VUELO;

            System.out.println(
                    "Embarque iniciado para el vuelo "
                            + numero + ".");
        } else {
            System.out.println(
                    "No se puede embarcar el vuelo "
                            + numero
                            + " porque esta "
                            + estado
                            + ".");
        }
    }

    @Override
    public void cancelar() {
        estado = EstadoVuelo.CANCELADO;

        System.out.println(
                "Vuelo " + numero + " cancelado.");
    }

    public void mostrarInfo() {
        System.out.println(
                "Vuelo " + numero
                        + " | Tipo: " + getTipo()
                        + " | Origen: " + origen
                        + " | Destino: " + destino
                        + " | Fecha: " + fecha
                        + " | Estado: " + estado
                        + " | Ocupados: "
                        + getAsientosOcupados()
                        + "/" + capacidad
                        + " | Disponibles: "
                        + getAsientosDisponibles());

        String detalle = obtenerDetalleAdicional();

        if (!detalle.isEmpty()) {
            System.out.println("Detalle: " + detalle);
        }
    }

    protected String obtenerDetalleAdicional() {
        return "";
    }

    private void validarDisponibilidadParaReserva()
            throws VueloNoDisponibleException {

        if (estado == EstadoVuelo.EN_VUELO) {
            throw new VueloNoDisponibleException(
                    "El vuelo " + numero
                            + " ya esta en vuelo.");
        }

        if (estado == EstadoVuelo.CANCELADO) {
            throw new VueloNoDisponibleException(
                    "El vuelo " + numero
                            + " esta cancelado.");
        }

        if (!hayAsientosDisponibles()) {
            throw new VueloNoDisponibleException(
                    "El vuelo " + numero
                            + " no tiene asientos disponibles.");
        }
    }

    private String validarTextoObligatorio(
            String valor,
            String campo) {

        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El campo " + campo
                            + " no puede estar vacio.");
        }

        String texto = valor.trim();
        if (texto.length() > (campo.equals("numero") ? 30 : 100)) throw new IllegalArgumentException("El campo " + campo + " es demasiado largo.");
        return texto;
    }

    @Override
    public int compareTo(Vuelo otro) {
        return this.numero.compareToIgnoreCase(
                otro.numero);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Vuelo)) {
            return false;
        }

        Vuelo otro = (Vuelo) obj;

        return numero.equalsIgnoreCase(
                otro.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero.toUpperCase());
    }
}
