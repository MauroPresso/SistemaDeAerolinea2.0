package aerolinea.ui;

import aerolinea.dominio.*;
import aerolinea.servicio.ServicioAerolinea;
import java.io.*;
import java.util.*;

/** Consola que invoca las mismas operaciones transaccionales que Swing. */
public final class Menu {
    private final ServicioAerolinea servicio;
    private final Scanner entrada;
    private final PrintStream salida;
    public Menu(ServicioAerolinea servicio) { this(servicio, System.in, System.out); }
    public Menu(ServicioAerolinea servicio, InputStream entrada, PrintStream salida) {
        this.servicio = servicio;
        this.entrada = new Scanner(entrada, java.nio.charset.StandardCharsets.UTF_8);
        this.salida = salida;
    }

    public void iniciar() {
        while (true) {
            mostrarMenu();
            try {
                int opcion = entero("Seleccione una opcion: ");
                if (opcion == 0) { salida.println("Saliendo del sistema."); break; }
                ejecutar(opcion);
            } catch (NoSuchElementException fin) { break; }
            catch (RuntimeException error) { salida.println("Operacion no realizada: " + error.getMessage()); }
        }
    }

    private void mostrarMenu() {
        salida.println("\nSISTEMA DE AEROLINEA 2.0");
        salida.println("1. Agregar vuelo | 2. Registrar pasajero | 3. Reservar | 4. Cancelar reserva");
        salida.println("5. Vuelos programados | 6. Ordenados por destino | 7. Asientos ocupados");
        salida.println("8. Buscar destino | 9. Listar pasajeros | 10. Editar pasajero | 11. Eliminar pasajero");
        salida.println("12. Editar vuelo | 13. Eliminar vuelo | 14. Cambiar estado del vuelo");
        salida.println("15. Registrar tripulante | 16. Asignar tripulante | 17. Listar tripulacion");
        salida.println("18. Buscar apellido | 0. Salir");
    }

    private void ejecutar(int opcion) {
        switch (opcion) {
            case 1 -> servicio.registrarVuelo(leerVuelo());
            case 2 -> servicio.registrarPersona(new Pasajero(entero("DNI: "), texto("Nombre: "), texto("Apellido: "), texto("Pasaporte (opcional): ")));
            case 3 -> { int dni = entero("DNI: "); String numero = texto("Numero de vuelo: "); if (!confirmar()) return; servicio.reservar(dni, numero); }
            case 4 -> { int dni = entero("DNI: "); String numero = texto("Numero de vuelo: "); if (!confirmar()) return; servicio.cancelarReserva(dni, numero); }
            case 5 -> { mostrarVuelos(servicio.vuelosProgramados()); return; }
            case 6 -> { mostrarVuelos(servicio.listarVuelos()); return; }
            case 7 -> { salida.println("Asientos ocupados en vuelos programados: " + servicio.totalAsientosOcupadosProgramados()); return; }
            case 8 -> { mostrarVuelos(servicio.vuelosPorDestino(texto("Destino: "))); return; }
            case 9 -> { mostrarPersonas(servicio.listarPasajeros()); return; }
            case 10 -> servicio.actualizarPasajero(entero("DNI: "), texto("Nombre: "), texto("Apellido: "), texto("Pasaporte (opcional): "));
            case 11 -> { int dni = entero("DNI: "); if (!confirmar()) return; servicio.eliminarPasajero(dni); }
            case 12 -> servicio.actualizarVuelo(texto("Numero: "), texto("Origen: "), texto("Destino: "), texto("Fecha (yyyy-MM-dd): "), entero("Capacidad: "));
            case 13 -> { String numero = texto("Numero: "); if (!confirmar()) return; servicio.eliminarVuelo(numero); }
            case 14 -> {
                String numero = texto("Numero: ");
                EstadoVuelo estado = EstadoVuelo.valueOf(texto("Estado (PROGRAMADO, EN_VUELO, CANCELADO): ").toUpperCase(Locale.ROOT));
                if (!confirmar()) return;
                servicio.cambiarEstado(numero, estado);
            }
            case 15 -> servicio.registrarPersona(new Tripulante(entero("DNI: "), texto("Nombre: "), texto("Apellido: "), entero("Legajo: "), texto("Rol: ")));
            case 16 -> servicio.asignarTripulante(entero("DNI del tripulante: "), texto("Numero de vuelo: "));
            case 17 -> { mostrarPersonas(servicio.listarTripulantes()); return; }
            case 18 -> { mostrarPersonas(servicio.pasajerosPorApellido(texto("Apellido: "))); return; }
            default -> { salida.println("Opcion invalida."); return; }
        }
        salida.println("Operacion guardada correctamente.");
    }

    private Vuelo leerVuelo() {
        int tipo = entero("Tipo (1 Nacional, 2 Internacional, 3 Charter): ");
        if (tipo < 1 || tipo > 3) throw new IllegalArgumentException("Tipo de vuelo invalido.");
        String numero = texto("Numero: "), origen = texto("Origen: "), destino = texto("Destino: "), fecha = texto("Fecha (yyyy-MM-dd): ");
        int capacidad = entero("Capacidad: ");
        return switch (tipo) {
            case 1 -> new VueloNacional(numero, origen, destino, fecha, capacidad, texto("Provincia de destino: "));
            case 2 -> new VueloInternacional(numero, origen, destino, fecha, capacidad, texto("Pais: "), siNo("Requiere pasaporte (S/N): "));
            default -> new VueloCharter(numero, origen, destino, fecha, capacidad, texto("Empresa: "), Double.parseDouble(texto("Costo total: ")));
        };
    }

    private void mostrarVuelos(List<Vuelo> vuelos) {
        if (vuelos.isEmpty()) salida.println("No hay vuelos para esta consulta.");
        for (Vuelo vuelo : vuelos) salida.printf("%s | %s | %s -> %s | %s | %s | Ocupados: %d/%d%n",
            vuelo.getNumero(), vuelo.getTipo(), vuelo.getOrigen(), vuelo.getDestino(), vuelo.getFecha(), vuelo.getEstado(), vuelo.getAsientosOcupados(), vuelo.getCapacidad());
    }
    private void mostrarPersonas(List<? extends Persona> personas) {
        if (personas.isEmpty()) salida.println("No hay personas para esta consulta.");
        for (Persona persona : personas) {
            salida.printf("%d | %s", persona.getDni(), persona.getNombreCompleto());
            if (persona instanceof Pasajero pasajero) salida.print(" | Pasaporte: " + pasajero.getNumeroPasaporte()
                + " | Reservas: " + pasajero.getVuelosReservados().stream().map(Vuelo::getNumero).toList());
            if (persona instanceof Tripulante tripulante) salida.print(" | " + tripulante.getLegajo() + " | " + tripulante.getRol());
            salida.println();
        }
    }
    private String texto(String mensaje) { salida.print(mensaje); return entrada.nextLine().trim(); }
    private int entero(String mensaje) {
        try { return Integer.parseInt(texto(mensaje)); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Debe ingresar un numero entero."); }
    }
    private boolean confirmar() { return siNo("Confirma la operacion (S/N): "); }
    private boolean siNo(String mensaje) {
        String valor = texto(mensaje);
        if (!valor.equalsIgnoreCase("S") && !valor.equalsIgnoreCase("N")) throw new IllegalArgumentException("Debe responder S o N.");
        return valor.equalsIgnoreCase("S");
    }
}
