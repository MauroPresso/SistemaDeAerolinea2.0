package aerolinea.dominio;

import aerolinea.excepcion.VueloNoDisponibleException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable(table = "PASAJEROS")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@javax.jdo.annotations.Queries({
    @javax.jdo.annotations.Query(name = "PasajerosPorApellido", value = "SELECT FROM aerolinea.dominio.Pasajero WHERE apellido.toLowerCase() == :apellido ORDER BY dni ASC")
})
public class Pasajero extends Persona {

    private static final long serialVersionUID = 1L;

    @Persistent(defaultFetchGroup = "true", dependent = "true")
    @Column(name = "PASAPORTE_ID")
    @Unique(name = "UQ_PASAJERO_PASAPORTE")
    @ForeignKey(name = "FK_PASAJERO_PASAPORTE", deleteAction = ForeignKeyAction.NULL)
    private Pasaporte pasaporte;

    @Persistent(mappedBy = "pasajeros", defaultFetchGroup = "true")
    @Order(extensions = @Extension(
            vendorName = "datanucleus",
            key = "list-ordering",
            value = "numero ASC"))
    private ArrayList<Vuelo> vuelosReservados = new ArrayList<>();

    protected Pasajero() {
        super();
    }

    public Pasajero(
            int dni,
            String nombre,
            String apellido,
            String numeroPasaporte) {

        super(dni, nombre, apellido);
        setNumeroPasaporte(numeroPasaporte);
    }

    public String getNumeroPasaporte() {
        return pasaporte == null ? "" : pasaporte.getNumero();
    }

    public void setNumeroPasaporte(String numeroPasaporte) {
        String numero = normalizarTextoOpcional(numeroPasaporte);
        if (numero.isEmpty()) pasaporte = null;
        else if (pasaporte == null) pasaporte = new Pasaporte(numero, this);
        else pasaporte.setNumero(numero);
    }

    public Pasaporte getPasaporte() { return pasaporte; }

    public List<Vuelo> getVuelosReservados() {
        return Collections.unmodifiableList(vuelosReservados);
    }

    public void reservarVuelo(Vuelo vuelo)
            throws VueloNoDisponibleException {

        if (vuelo == null) {
            throw new IllegalArgumentException(
                    "El vuelo no puede ser nulo.");
        }

        vuelo.reservarPasajero(this);
    }

    public void cancelarReserva(Vuelo vuelo) {
        if (vuelo == null) {
            throw new IllegalArgumentException(
                    "El vuelo no puede ser nulo.");
        }

        vuelo.cancelarReserva(this);
    }

    public boolean tieneVueloReservado(Vuelo vuelo) {
        return vuelosReservados.contains(vuelo);
    }

    public boolean tieneReservaActiva() {
        return vuelosReservados.stream()
                .anyMatch(vuelo ->
                        vuelo.getEstado()
                                != EstadoVuelo.CANCELADO);
    }

    public void mostrarReservas() {
        System.out.println(
                "Reservas de " + getNombreCompleto() + ":");

        if (vuelosReservados.isEmpty()) {
            System.out.println("No registra reservas.");
            return;
        }

        vuelosReservados.forEach(Vuelo::mostrarInfo);
    }

    void agregarVueloReservado(Vuelo vuelo) {
        if (!vuelosReservados.contains(vuelo)) {
            vuelosReservados.add(vuelo);
        }
    }

    void quitarVueloReservado(Vuelo vuelo) {
        vuelosReservados.remove(vuelo);
    }

    @Override
    public void mostrarInfo() {
        System.out.println(
                "Pasajero: " + getNombreCompleto()
                        + " | DNI: " + getDni()
                        + " | Pasaporte: "
                        + (getNumeroPasaporte().isEmpty()
                        ? "No informado"
                        : getNumeroPasaporte())
                        + " | Reservas: "
                        + vuelosReservados.size());
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
