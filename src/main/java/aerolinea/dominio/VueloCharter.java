package aerolinea.dominio;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable(table = "VUELOS_CHARTER")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
public class VueloCharter extends Vuelo {

    private static final long serialVersionUID = 1L;

    @Persistent
    @Column(name = "EMPRESA_CONTRATANTE", length = 150)
    private String empresaContratante;

    @Persistent
    @Column(name = "COSTO_TOTAL")
    private double costoTotal;

    protected VueloCharter() {
        super();
    }

    public VueloCharter(
            String numero,
            String origen,
            String destino,
            String fecha,
            int capacidad,
            String empresaContratante,
            double costoTotal) {

        super(
                numero,
                origen,
                destino,
                fecha,
                capacidad);

        setEmpresaContratante(
                empresaContratante);

        setCostoTotal(costoTotal);
    }

    public String getEmpresaContratante() {
        return empresaContratante;
    }

    public void setEmpresaContratante(
            String empresaContratante) {

        if (empresaContratante == null
                || empresaContratante.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "La empresa contratante "
                            + "no puede estar vacia.");
        }

        this.empresaContratante =
                empresaContratante.trim();
    }

    public double getCostoTotal() {
        return costoTotal;
    }

    public void setCostoTotal(double costoTotal) {
        if (!Double.isFinite(costoTotal) || costoTotal < 0) {
            throw new IllegalArgumentException(
                    "El costo total "
                            + "no puede ser negativo.");
        }

        this.costoTotal = costoTotal;
    }

    @Override
    public String getTipo() {
        return "Charter";
    }

    @Override
    protected String obtenerDetalleAdicional() {
        return "Empresa contratante: "
                + empresaContratante
                + " | Costo total: $"
                + costoTotal;
    }
}
