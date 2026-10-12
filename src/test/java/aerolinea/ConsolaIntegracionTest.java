package aerolinea;

import aerolinea.repositorio.ContextoJdo;
import aerolinea.servicio.ServicioAerolinea;
import aerolinea.ui.Menu;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsolaIntegracionTest {
    @Test void consolaRegistraReservaConsultaCancelaYRecuperaEntradaInvalida() {
        String datos = String.join("\n", "hola", "1", "1", "ar100", "Neuqu\u00e9n", "C\u00f3rdoba", "2027-01-10", "2", "C\u00f3rdoba",
            "2", "30111222", "Ana", "P\u00e9rez", "", "3", "30111222", "AR100", "S", "7", "4", "30111222", "AR100", "S", "0") + "\n";
        try (ContextoJdo contexto = new ContextoJdo("Aerolinea-H2", Map.of("javax.jdo.option.ConnectionURL", "jdbc:h2:mem:consola_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"))) {
            ServicioAerolinea servicio = new ServicioAerolinea(contexto);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            new Menu(servicio, new ByteArrayInputStream(datos.getBytes(StandardCharsets.UTF_8)), new PrintStream(salida, true, StandardCharsets.UTF_8)).iniciar();
            String texto = salida.toString(StandardCharsets.UTF_8);
            assertTrue(texto.contains("Debe ingresar un numero entero"));
            assertTrue(texto.contains("Asientos ocupados en vuelos programados: 1"));
            assertEquals(0, servicio.buscarVuelo("AR100").orElseThrow().getAsientosOcupados());
            assertEquals(1, servicio.listarPasajeros().size());
        }
    }
}
