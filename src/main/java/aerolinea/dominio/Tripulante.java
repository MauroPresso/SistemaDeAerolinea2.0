package aerolinea.dominio;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable(table = "TRIPULANTES")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
public class Tripulante extends Persona {

    private static final long serialVersionUID = 1L;

    @Persistent
    @Column(name = "LEGAJO")
    private int legajo;

    @Persistent
    @Column(name = "ROL", length = 100)
    private String rol;

    protected Tripulante() {
        super();
    }

    public Tripulante(
            int dni,
            String nombre,
            String apellido,
            int legajo,
            String rol) {

        super(dni, nombre, apellido);
        setLegajo(legajo);
        setRol(rol);
    }

    public int getLegajo() {
        return legajo;
    }

    public void setLegajo(int legajo) {
        if (legajo <= 0) {
            throw new IllegalArgumentException(
                    "El legajo debe ser mayor que cero.");
        }
        this.legajo = legajo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        if (rol == null || rol.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El rol no puede estar vacio.");
        }
        this.rol = rol.trim();
    }

    @Override
    public void mostrarInfo() {
        System.out.println(
                "Tripulante: " + getNombreCompleto()
                        + " | DNI: " + getDni()
                        + " | Legajo: " + legajo
                        + " | Rol: " + rol);
    }
}
