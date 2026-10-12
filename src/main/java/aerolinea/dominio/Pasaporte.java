package aerolinea.dominio;

import javax.jdo.annotations.*;

/** Pasaporte vigente: relacion 1 a 1 bidireccional con su titular. */
@PersistenceCapable(table = "PASAPORTES", detachable = "true")
@Unique(name = "UQ_PASAPORTE_NUMERO", members = "numero")
public class Pasaporte implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private long id;
    @Column(name = "NUMERO", length = 50, allowsNull = "false")
    private String numero;
    @Persistent(mappedBy = "pasaporte")
    private Pasajero titular;

    protected Pasaporte() {}
    public Pasaporte(String numero, Pasajero titular) {
        setNumero(numero);
        this.titular = java.util.Objects.requireNonNull(titular);
    }
    public long getId() { return id; }
    public String getNumero() { return numero; }
    public Pasajero getTitular() { return titular; }
    public void setNumero(String numero) {
        if (numero == null || numero.isBlank() || numero.trim().length() > 50)
            throw new IllegalArgumentException("El pasaporte debe tener entre 1 y 50 caracteres.");
        this.numero = numero.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
