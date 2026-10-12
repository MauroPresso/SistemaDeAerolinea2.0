package aerolinea.ui;

import aerolinea.dominio.*;
import aerolinea.servicio.ServicioAerolinea;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Interfaz de gestion que comparte ServicioAerolinea con la consola. */
public final class Ventana extends JFrame {
    private final ServicioAerolinea servicio;
    private final DefaultTableModel vuelos = modelo("Numero", "Tipo", "Origen", "Destino", "Fecha", "Capacidad", "Ocupados", "Estado");
    private final DefaultTableModel pasajeros = modelo("DNI", "Nombre", "Apellido", "Pasaporte", "Reservas");
    private final DefaultTableModel tripulantes = modelo("DNI", "Nombre", "Apellido", "Legajo", "Rol");
    private final DefaultTableModel reservas = modelo("DNI", "Pasajero", "Vuelo", "Destino", "Estado del vuelo");
    private final JLabel estado = new JLabel(" ");

    public Ventana(ServicioAerolinea servicio) {
        super("Sistema de Aerolinea 2.0");
        this.servicio = servicio;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        JTable tablaVuelos = new JTable(vuelos), tablaPasajeros = new JTable(pasajeros), tablaTripulantes = new JTable(tripulantes);
        JPanel panelVuelos = tabla(tablaVuelos);
        JPanel accionesVuelos = acciones(panelVuelos);
        boton(accionesVuelos, "Agregar", this::altaVuelo);
        boton(accionesVuelos, "Editar", () -> editarVuelo(seleccion(tablaVuelos)));
        boton(accionesVuelos, "Eliminar", () -> { String numero = seleccion(tablaVuelos); if (confirmar("Eliminar el vuelo " + numero + "?")) escribir(() -> servicio.eliminarVuelo(numero)); });
        boton(accionesVuelos, "Cambiar estado", () -> cambiarEstado(seleccion(tablaVuelos)));
        boton(accionesVuelos, "Buscar destino", () -> { String destino = pedir("Destino:"); if (destino != null) cargarVuelos(servicio.vuelosPorDestino(destino)); });
        boton(accionesVuelos, "Programados", () -> cargarVuelos(servicio.vuelosProgramados()));
        boton(accionesVuelos, "Todos", this::refrescar);
        tabs.addTab("Vuelos", panelVuelos);

        JPanel panelPasajeros = tabla(tablaPasajeros), accionesPasajeros = acciones(panelPasajeros);
        boton(accionesPasajeros, "Registrar", () -> formularioPasajero(null));
        boton(accionesPasajeros, "Editar", () -> formularioPasajero(Integer.parseInt(seleccion(tablaPasajeros))));
        boton(accionesPasajeros, "Eliminar", () -> { int dni = Integer.parseInt(seleccion(tablaPasajeros)); if (confirmar("Eliminar el pasajero " + dni + "?")) escribir(() -> servicio.eliminarPasajero(dni)); });
        boton(accionesPasajeros, "Buscar apellido", () -> { String apellido = pedir("Apellido:"); if (apellido != null) cargarPasajeros(servicio.pasajerosPorApellido(apellido)); });
        boton(accionesPasajeros, "Todos", this::refrescar);
        tabs.addTab("Pasajeros", panelPasajeros);

        JPanel panelReservas = tabla(new JTable(reservas)), accionesReservas = acciones(panelReservas);
        boton(accionesReservas, "Reservar", () -> operarReserva(true));
        boton(accionesReservas, "Cancelar reserva", () -> operarReserva(false));
        boton(accionesReservas, "Actualizar", this::refrescar);
        tabs.addTab("Reservas", panelReservas);

        JPanel panelTripulantes = tabla(tablaTripulantes), accionesTripulantes = acciones(panelTripulantes);
        boton(accionesTripulantes, "Registrar", () -> {
            String[] datos = formulario("Registrar tripulante", new String[]{"DNI", "Nombre", "Apellido", "Legajo", "Rol"}, null);
            if (datos != null) escribir(() -> servicio.registrarPersona(new Tripulante(Integer.parseInt(datos[0]), datos[1], datos[2], Integer.parseInt(datos[3]), datos[4])));
        });
        boton(accionesTripulantes, "Asignar a vuelo", () -> {
            int dni = Integer.parseInt(seleccion(tablaTripulantes)); String numero = pedir("Numero de vuelo:");
            if (numero != null) escribir(() -> servicio.asignarTripulante(dni, numero));
        });
        boton(accionesTripulantes, "Actualizar", this::refrescar);
        tabs.addTab("Tripulacion", panelTripulantes);
        add(tabs, BorderLayout.CENTER);
        estado.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        add(estado, BorderLayout.SOUTH);
        refrescar();
    }

    private void altaVuelo() {
        Object tipo = JOptionPane.showInputDialog(this, "Tipo de vuelo", "Agregar vuelo", JOptionPane.QUESTION_MESSAGE, null, new String[]{"Nacional", "Internacional", "Charter"}, "Nacional");
        if (tipo == null) return;
        String adicional = tipo.equals("Nacional") ? "Provincia" : tipo.equals("Internacional") ? "Pais" : "Empresa";
        String[] datos = formulario("Agregar " + tipo, new String[]{"Numero", "Origen", "Destino", "Fecha (yyyy-MM-dd)", "Capacidad", adicional}, null);
        if (datos == null) return;
        Vuelo vuelo;
        if (tipo.equals("Nacional")) vuelo = new VueloNacional(datos[0], datos[1], datos[2], datos[3], Integer.parseInt(datos[4]), datos[5]);
        else if (tipo.equals("Internacional")) {
            int respuesta = JOptionPane.showConfirmDialog(this, "Requiere pasaporte?", "Vuelo internacional", JOptionPane.YES_NO_CANCEL_OPTION);
            if (respuesta != JOptionPane.YES_OPTION && respuesta != JOptionPane.NO_OPTION) return;
            vuelo = new VueloInternacional(datos[0], datos[1], datos[2], datos[3], Integer.parseInt(datos[4]), datos[5], respuesta == JOptionPane.YES_OPTION);
        } else {
            String costo = pedir("Costo total:"); if (costo == null) return;
            vuelo = new VueloCharter(datos[0], datos[1], datos[2], datos[3], Integer.parseInt(datos[4]), datos[5], Double.parseDouble(costo));
        }
        escribir(() -> servicio.registrarVuelo(vuelo));
    }
    private void editarVuelo(String numero) {
        Vuelo vuelo = servicio.buscarVuelo(numero).orElseThrow();
        String[] datos = formulario("Editar " + numero, new String[]{"Origen", "Destino", "Fecha", "Capacidad"}, new String[]{vuelo.getOrigen(), vuelo.getDestino(), vuelo.getFecha(), String.valueOf(vuelo.getCapacidad())});
        if (datos != null) escribir(() -> servicio.actualizarVuelo(numero, datos[0], datos[1], datos[2], Integer.parseInt(datos[3])));
    }
    private void cambiarEstado(String numero) {
        Object estado = JOptionPane.showInputDialog(this, "Estado", "Vuelo " + numero, JOptionPane.QUESTION_MESSAGE, null, EstadoVuelo.values(), EstadoVuelo.PROGRAMADO);
        if (estado != null && confirmar("Cambiar el estado del vuelo " + numero + "?")) escribir(() -> servicio.cambiarEstado(numero, (EstadoVuelo) estado));
    }
    private void formularioPasajero(Integer dni) {
        String[] valores = null;
        if (dni != null) { Pasajero p = servicio.buscarPasajero(dni).orElseThrow(); valores = new String[]{p.getNombre(), p.getApellido(), p.getNumeroPasaporte()}; }
        String[] datos = formulario(dni == null ? "Registrar pasajero" : "Editar pasajero " + dni,
            dni == null ? new String[]{"DNI", "Nombre", "Apellido", "Pasaporte (opcional)"} : new String[]{"Nombre", "Apellido", "Pasaporte (opcional)"}, valores);
        if (datos == null) return;
        if (dni == null) escribir(() -> servicio.registrarPersona(new Pasajero(Integer.parseInt(datos[0]), datos[1], datos[2], datos[3])));
        else escribir(() -> servicio.actualizarPasajero(dni, datos[0], datos[1], datos[2]));
    }
    private void operarReserva(boolean reservar) {
        String[] datos = formulario(reservar ? "Reservar vuelo" : "Cancelar reserva", new String[]{"DNI del pasajero", "Numero de vuelo"}, null);
        if (datos != null && confirmar(reservar ? "Confirmar la reserva?" : "Cancelar esta reserva?")) escribir(() -> {
            if (reservar) servicio.reservar(Integer.parseInt(datos[0]), datos[1]);
            else servicio.cancelarReserva(Integer.parseInt(datos[0]), datos[1]);
        });
    }
    private void escribir(Runnable operacion) { operacion.run(); refrescar(); JOptionPane.showMessageDialog(this, "Operacion guardada correctamente."); }
    private void refrescar() {
        cargarVuelos(servicio.listarVuelos()); cargarPasajeros(servicio.listarPasajeros());
        tripulantes.setRowCount(0);
        for (Tripulante t : servicio.listarTripulantes()) tripulantes.addRow(new Object[]{t.getDni(), t.getNombre(), t.getApellido(), t.getLegajo(), t.getRol()});
        reservas.setRowCount(0);
        for (Pasajero p : servicio.listarPasajeros()) for (Vuelo v : p.getVuelosReservados()) reservas.addRow(new Object[]{p.getDni(), p.getNombreCompleto(), v.getNumero(), v.getDestino(), v.getEstado()});
        estado.setText("Asientos ocupados en vuelos programados: " + servicio.totalAsientosOcupadosProgramados());
    }
    private void cargarVuelos(List<Vuelo> lista) { vuelos.setRowCount(0); for (Vuelo v : lista) vuelos.addRow(new Object[]{v.getNumero(), v.getTipo(), v.getOrigen(), v.getDestino(), v.getFecha(), v.getCapacidad(), v.getAsientosOcupados(), v.getEstado()}); }
    private void cargarPasajeros(List<Pasajero> lista) { pasajeros.setRowCount(0); for (Pasajero p : lista) pasajeros.addRow(new Object[]{p.getDni(), p.getNombre(), p.getApellido(), p.getNumeroPasaporte(), p.getVuelosReservados().size()}); }
    private String[] formulario(String titulo, String[] etiquetas, String[] valores) {
        JPanel panel = new JPanel(new GridLayout(etiquetas.length, 2, 10, 10)); JTextField[] campos = new JTextField[etiquetas.length];
        for (int i = 0; i < campos.length; i++) { panel.add(new JLabel(etiquetas[i])); campos[i] = new JTextField(valores == null ? "" : valores[i], 22); panel.add(campos[i]); }
        if (JOptionPane.showConfirmDialog(this, panel, titulo, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;
        return java.util.Arrays.stream(campos).map(c -> c.getText().trim()).toArray(String[]::new);
    }
    private String pedir(String etiqueta) { return JOptionPane.showInputDialog(this, etiqueta); }
    private boolean confirmar(String texto) { return JOptionPane.showConfirmDialog(this, texto, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }
    private String seleccion(JTable tabla) { int fila = tabla.getSelectedRow(); if (fila < 0) throw new IllegalArgumentException("Seleccione una fila de la tabla."); return tabla.getValueAt(fila, 0).toString(); }
    private JPanel tabla(JTable tabla) { tabla.setAutoCreateRowSorter(true); JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); panel.add(new JScrollPane(tabla)); return panel; }
    private JPanel acciones(JPanel panel) { JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT)); panel.add(botones, BorderLayout.NORTH); return botones; }
    private void boton(JPanel panel, String texto, Runnable accion) { JButton b = new JButton(texto); b.addActionListener(e -> { try { accion.run(); } catch (RuntimeException error) { JOptionPane.showMessageDialog(this, error.getMessage(), "Operacion no realizada", JOptionPane.ERROR_MESSAGE); } }); panel.add(b); }
    private static DefaultTableModel modelo(String... columnas) { return new DefaultTableModel(columnas, 0) { @Override public boolean isCellEditable(int fila, int columna) { return false; } }; }
}
