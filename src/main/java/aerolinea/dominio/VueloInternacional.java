package aerolinea.dominio;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable(table = "VUELOS_INTERNACIONALES")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
public class VueloInternacional extends Vuelo {

    private static final long serialVersionUID = 1L;

    @Persistent
    @Column(name = "PAIS_DESTINO", length = 100)
    private String paisDestino;

    @Persistent
    @Column(name = "REQUIERE_PASAPORTE")
    private boolean requierePasaporte;

    protected VueloInternacional() {
        super();
    }

    public VueloInternacional(
            String numero,
            String origen,
            String destino,
            String fecha,
            int capacidad,
            String paisDestino,
            boolean requierePasaporte) {

        super(
                numero,
                origen,
                destino,
                fecha,
                capacidad);

        setPaisDestino(paisDestino);
        this.requierePasaporte =
                requierePasaporte;
    }

    public String getPaisDestino() {
        return paisDestino;
    }

    public void setPaisDestino(String paisDestino) {
        if (paisDestino == null
                || paisDestino.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "El pais de destino "
                            + "no puede estar vacio.");
        }

        this.paisDestino =
                paisDestino.trim();
    }

    public boolean isRequierePasaporte() {
        return requierePasaporte;
    }

    public void setRequierePasaporte(
            boolean requierePasaporte) {

        this.requierePasaporte =
                requierePasaporte;
    }

    @Override
    public String getTipo() {
        return "Internacional";
    }

    @Override
    protected String obtenerDetalleAdicional() {
        return "Pais destino: "
                + paisDestino
                + " | Requiere pasaporte: "
                + (requierePasaporte
                ? "Si"
                : "No");
    }
}
