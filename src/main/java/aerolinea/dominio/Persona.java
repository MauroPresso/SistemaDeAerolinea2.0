package aerolinea.dominio;

import java.io.Serializable;
import java.util.Objects;

import javax.jdo.annotations.*;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(
        table = "PERSONAS",
        identityType = IdentityType.APPLICATION,
        detachable = "true")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
@Version(strategy = VersionStrategy.VERSION_NUMBER, column = "VERSION")
public abstract class Persona implements Comparable<Persona>, Serializable {

    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent
    @Column(name = "DNI")
    private int dni;

    @Persistent
    @Column(name = "NOMBRE", length = 100)
    private String nombre;

    @Persistent
    @Column(name = "APELLIDO", length = 100)
    private String apellido;

    protected Persona() {
    }

    public Persona(int dni, String nombre, String apellido) {
        if (dni <= 0) {
            throw new IllegalArgumentException("El DNI debe ser mayor que cero.");
        }
        this.dni = dni;
        setNombre(nombre);
        setApellido(apellido);
    }

    public int getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio.");
        }
        if (nombre.trim().length() > 100) throw new IllegalArgumentException("El nombre es demasiado largo.");
        this.nombre = nombre.trim();
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        if (apellido == null || apellido.trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido no puede estar vacio.");
        }
        if (apellido.trim().length() > 100) throw new IllegalArgumentException("El apellido es demasiado largo.");
        this.apellido = apellido.trim();
    }

    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    public abstract void mostrarInfo();

    @Override
    public int compareTo(Persona otra) {
        int comparacionApellido =
                this.apellido.compareToIgnoreCase(otra.apellido);

        if (comparacionApellido != 0) {
            return comparacionApellido;
        }

        int comparacionNombre =
                this.nombre.compareToIgnoreCase(otra.nombre);

        if (comparacionNombre != 0) {
            return comparacionNombre;
        }

        return Integer.compare(this.dni, otra.dni);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Persona)) {
            return false;
        }

        Persona otra = (Persona) obj;
        return dni == otra.dni;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dni);
    }
}
