package aerolinea.main;

import aerolinea.repositorio.ContextoJdo;
import aerolinea.servicio.ServicioAerolinea;
import aerolinea.ui.*;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.SwingUtilities;

public final class Main {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--ayuda")) {
            System.out.println("java -jar target/SistemaDeAerolinea2.0.jar [--consola] [--mysql]\nH2 y JDO son el modo predeterminado. --orm se acepta por compatibilidad.");
            return;
        }
        boolean mysql = Arrays.asList(args).contains("--mysql");
        boolean consola = Arrays.asList(args).contains("--consola") || GraphicsEnvironment.isHeadless();
        try {
            ContextoJdo contexto = ContextoJdo.desdeEntorno(mysql ? "Aerolinea-MySQL" : "Aerolinea-H2");
            ServicioAerolinea servicio = new ServicioAerolinea(contexto);
            // Comprueba la conexion antes de mostrar una interfaz operativa.
            try { servicio.listarVuelos(); }
            catch (RuntimeException error) { contexto.close(); throw error; }
            System.out.println("Persistencia: DataNucleus/JDO + " + (mysql ? "MySQL" : "H2"));
            if (consola) {
                try (contexto) { new Menu(servicio).iniciar(); }
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread(contexto::close));
                SwingUtilities.invokeLater(() -> new Ventana(servicio).setVisible(true));
            }
        } catch (RuntimeException error) {
            System.err.println("No se pudo iniciar el sistema: " + error.getMessage());
            System.exit(1);
        }
    }
}
