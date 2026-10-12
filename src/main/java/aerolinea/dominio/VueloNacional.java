package aerolinea.dominio;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable(table = "VUELOS_NACIONALES")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
public class VueloNacional extends Vuelo {

    private static final long serialVersionUID = 1L;

    @Persistent
    @Column(name = "PROVINCIA_DESTINO", length = 100)
    private String provinciaDestino;

    protected VueloNacional() {
        super();
    }

    public VueloNacional(
            String numero,
            String origen,
            String destino,
            String fecha,
            int capacidad,
            String provinciaDestino) {

        super(
                numero,
                origen,
                destino,
                fecha,
                capacidad);

        setProvinciaDestino(provinciaDestino);
    }

    public String getProvinciaDestino() {
        return provinciaDestino;
    }

    public void setProvinciaDestino(
            String provinciaDestino) {

        if (provinciaDestino == null
                || provinciaDestino.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "La provincia de destino "
                            + "no puede estar vacia.");
        }

        this.provinciaDestino =
                provinciaDestino.trim();
    }

    @Override
    public String getTipo() {
        return "Nacional";
    }

    @Override
    protected String obtenerDetalleAdicional() {
        return "Provincia destino: "
                + provinciaDestino;
    }
}
