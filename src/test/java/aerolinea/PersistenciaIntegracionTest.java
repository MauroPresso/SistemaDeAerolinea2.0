package aerolinea;

import aerolinea.dominio.*;
import aerolinea.excepcion.*;
import aerolinea.repositorio.*;
import aerolinea.servicio.ServicioAerolinea;
import javax.jdo.JDOHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Integraci\u00f3n real con H2, enhancement JDO y objetos de ambas jerarqu\u00edas. */
class PersistenciaIntegracionTest {
    private ContextoJdo contexto;
    private ServicioAerolinea servicio;
    @BeforeEach void preparar() {
        contexto = contexto("jdbc:h2:mem:prueba_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        servicio = new ServicioAerolinea(contexto);
    }
    @AfterEach void cerrar() { contexto.close(); }
    private ContextoJdo contexto(String url) { return new ContextoJdo("Aerolinea-H2", Map.of("javax.jdo.option.ConnectionURL", url)); }
    private VueloNacional nacional(String numero, int capacidad) {
        return new VueloNacional(numero, "Neuqu\u00e9n", "C\u00f3rdoba", "2027-01-10", capacidad, "C\u00f3rdoba");
    }
    private Pasajero pasajero(int dni, String pasaporte) { return new Pasajero(dni, "Ana", "P\u00e9rez", pasaporte); }

    @Test void consultaPolimorficaRecuperaPadresHijasYDetalles() {
        servicio.registrarVuelo(nacional("AR100", 3));
        servicio.registrarVuelo(new VueloInternacional("AR200", "Neuqu\u00e9n", "Santiago", "2027-01-11", 4, "Chile", true));
        servicio.registrarVuelo(new VueloCharter("CH300", "Neuqu\u00e9n", "A\u00f1elo", "2027-01-12", 5, "Empresa", 1000));
        assertEquals(3, servicio.listarVuelos().size());
        assertInstanceOf(VueloNacional.class, servicio.buscarVuelo("ar100").orElseThrow());
        assertEquals("Chile", ((VueloInternacional) servicio.buscarVuelo("AR200").orElseThrow()).getPaisDestino());
        assertEquals(1000, ((VueloCharter) servicio.buscarVuelo("CH300").orElseThrow()).getCostoTotal());
        servicio.registrarPersona(pasajero(30111222, "AA123"));
        servicio.registrarPersona(new Tripulante(30222333, "Luis", "G\u00f3mez", 7, "Piloto"));
        IRepositorio<Persona> personas = new RepositorioJdo<>(contexto, Persona.class);
        assertEquals(2, personas.consultar().size());
        assertInstanceOf(Tripulante.class, personas.consultar(30222333).orElseThrow());
    }

    @Test void vueloYPasajeroMantienenRelacionBidireccionalDespuesDeCerrarElPM() {
        servicio.registrarVuelo(nacional("AR100", 2));
        servicio.registrarVuelo(nacional("AR101", 2));
        servicio.registrarPersona(pasajero(30111222, "AA123"));
        servicio.reservar(30111222, "ar100");
        servicio.reservar(30111222, "AR101");
        Pasajero p = servicio.buscarPasajero(30111222).orElseThrow();
        assertTrue(JDOHelper.isDetached(p));
        assertEquals(2, p.getVuelosReservados().size());
        assertEquals(1, servicio.buscarVuelo("AR100").orElseThrow().getPasajeros().size());
        servicio.cancelarReserva(30111222, "AR100");
        Pasajero actualizado = servicio.buscarPasajero(30111222).orElseThrow();
        assertEquals(List.of("AR101"), actualizado.getVuelosReservados().stream().map(Vuelo::getNumero).toList());
        assertTrue(actualizado.tieneReservaActiva());
        assertEquals(0, servicio.buscarVuelo("AR100").orElseThrow().getAsientosOcupados());
    }

    @Test void pasaporteUnoAUnoSeCargaActualizaYEliminaConElTitular() {
        servicio.registrarPersona(pasajero(30111222, "aa123"));
        Pasajero p = servicio.buscarPasajero(30111222).orElseThrow();
        assertEquals("AA123", p.getPasaporte().getNumero());
        assertEquals(30111222, p.getPasaporte().getTitular().getDni());
        servicio.actualizarPasajero(30111222, "Ana", "P\u00e9rez", "BB456");
        assertEquals("BB456", servicio.buscarPasajero(30111222).orElseThrow().getNumeroPasaporte());
        servicio.eliminarPasajero(30111222);
        assertTrue(new RepositorioJdo<>(contexto, Pasaporte.class).consultar().isEmpty());
    }

    @Test void quitarPasaporteOpcionalNoDejaRegistrosHuerfanos() {
        servicio.registrarPersona(pasajero(30111222, "AA123"));
        servicio.actualizarPasajero(30111222, "Ana", "P\u00e9rez", "");
        assertEquals("", servicio.buscarPasajero(30111222).orElseThrow().getNumeroPasaporte());
        assertTrue(new RepositorioJdo<>(contexto, Pasaporte.class).consultar().isEmpty());
    }

    @Test void eliminarPasaporteDirectamenteConservaAlPasajeroSinDocumento() {
        servicio.registrarPersona(pasajero(30111222, "AA123"));
        long id = servicio.buscarPasajero(30111222).orElseThrow().getPasaporte().getId();
        assertTrue(new RepositorioJdo<>(contexto, Pasaporte.class).eliminar(id));
        assertEquals("", servicio.buscarPasajero(30111222).orElseThrow().getNumeroPasaporte());
    }

    @Test void indicesDeAerolineaIncluyenPersonasSinReservaYSeRecalculanTrasCancelar() {
        servicio.registrarPersona(pasajero(30111222, ""));
        servicio.registrarPersona(pasajero(30111223, ""));
        servicio.registrarVuelo(nacional("AR100", 2));
        servicio.reservar(30111222, "AR100");
        Aerolinea a = servicio.consultarAerolinea().orElseThrow();
        assertEquals(2, a.getPersonasPorDni().size());
        assertEquals(1, a.getPasajerosConReservaActiva().size());
        servicio.cancelarReserva(30111222, "AR100");
        servicio.eliminarPasajero(30111223);
        Aerolinea actualizada = servicio.consultarAerolinea().orElseThrow();
        assertTrue(actualizada.getPasajerosConReservaActiva().isEmpty());
        assertEquals(1, actualizada.getPersonasPorDni().size());
    }

    @Test void aerolineaMantieneColeccionUnoAMuchosYTripulacionPersistida() {
        servicio.registrarVuelo(nacional("AR100", 2));
        servicio.registrarVuelo(nacional("AR101", 2));
        servicio.registrarPersona(new Tripulante(30222333, "Luis", "G\u00f3mez", 7, "Piloto"));
        servicio.asignarTripulante(30222333, "AR100");
        assertEquals(2, servicio.consultarAerolinea().orElseThrow().getVuelos().size());
        assertEquals(7, servicio.buscarVuelo("AR100").orElseThrow().getTripulacion().get(0).getLegajo());
        servicio.eliminarVuelo("AR101");
        assertEquals(1, servicio.consultarAerolinea().orElseThrow().getVuelos().size());
        assertEquals(1, servicio.listarTripulantes().size());
    }

    @Test void consultasNombradasFiltranOrdenanYParametrizanSinConcatenar() {
        servicio.registrarVuelo(nacional("AR100", 2));
        servicio.registrarVuelo(new VueloNacional("AR101", "Neuqu\u00e9n", "Bariloche", "2027-01-10", 3, "R\u00edo Negro"));
        servicio.registrarPersona(pasajero(30111222, ""));
        assertEquals("AR100", servicio.vuelosPorDestino(" C\u00d3RDOBA ").get(0).getNumero());
        assertTrue(servicio.vuelosPorDestino("x\" || true || \"x").isEmpty());
        assertEquals("AR101", servicio.listarVuelos().get(0).getNumero());
        assertEquals(30111222, servicio.pasajerosPorApellido("P\u00c9REZ").get(0).getDni());
        servicio.cambiarEstado("AR100", EstadoVuelo.CANCELADO);
        assertEquals(List.of("AR101"), servicio.vuelosProgramados().stream().map(Vuelo::getNumero).toList());
    }

    @Test void reservaDuplicadaVueloLlenoYPasaporteSeValidanEnServicio() {
        servicio.registrarVuelo(nacional("AR100", 1));
        servicio.registrarPersona(pasajero(30111222, ""));
        servicio.registrarPersona(pasajero(30111223, ""));
        servicio.reservar(30111222, "AR100");
        assertThrows(IllegalArgumentException.class, () -> servicio.reservar(30111222, "AR100"));
        assertThrows(VueloNoDisponibleException.class, () -> servicio.reservar(30111223, "AR100"));
        assertEquals(1, servicio.totalAsientosOcupadosProgramados());
        servicio.registrarVuelo(new VueloInternacional("AR200", "Neuqu\u00e9n", "Santiago", "2027-01-11", 4, "Chile", true));
        assertThrows(IllegalArgumentException.class, () -> servicio.reservar(30111222, "AR200"));
        assertEquals(0, servicio.buscarVuelo("AR200").orElseThrow().getAsientosOcupados());
    }

    @Test void transaccionFallidaRevierteCambiosAnterioresALaValidacion() {
        servicio.registrarVuelo(nacional("AR100", 2));
        servicio.registrarPersona(pasajero(30111222, ""));
        servicio.registrarPersona(pasajero(30111223, ""));
        servicio.reservar(30111222, "AR100"); servicio.reservar(30111223, "AR100");
        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarVuelo("AR100", "Mendoza", "Rosario", "2027-02-01", 1));
        Vuelo persistido = servicio.buscarVuelo("AR100").orElseThrow();
        assertEquals("Neuqu\u00e9n", persistido.getOrigen());
        assertEquals("C\u00f3rdoba", persistido.getDestino());
        assertEquals("2027-01-10", persistido.getFecha());
        assertEquals(2, persistido.getAsientosOcupados());
    }

    @Test void loteCompletoSeRevierteSiUnPasaporteEsDuplicado() {
        IRepositorio<Pasajero> repo = new RepositorioJdo<>(contexto, Pasajero.class);
        assertThrows(PersistenciaException.class, () -> repo.guardarLista(List.of(pasajero(30111222, "AA123"), pasajero(30111223, "AA123"))));
        assertTrue(repo.consultar().isEmpty());
        assertTrue(new RepositorioJdo<>(contexto, Pasaporte.class).consultar().isEmpty());
    }

    @Test void edicionNoRecreaVuelosNiPierdeSusReservas() {
        servicio.registrarVuelo(nacional("AR100", 2)); servicio.registrarVuelo(nacional("AR101", 2));
        servicio.registrarPersona(pasajero(30111222, "")); servicio.reservar(30111222, "AR100");
        servicio.actualizarVuelo("AR101", "Neuqu\u00e9n", "Mendoza", "2027-01-13", 10);
        assertEquals(1, servicio.buscarVuelo("AR100").orElseThrow().getAsientosOcupados());
        assertEquals("Mendoza", servicio.buscarVuelo("AR101").orElseThrow().getDestino());
        assertEquals(2, servicio.listarVuelos().size());
    }

    @Test void borradosProtegenRelacionesYNoBorranPasajerosDelVuelo() {
        servicio.registrarVuelo(nacional("AR100", 2)); servicio.registrarPersona(pasajero(30111222, "AA123")); servicio.reservar(30111222, "AR100");
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminarVuelo("AR100"));
        assertThrows(IllegalArgumentException.class, () -> servicio.eliminarPasajero(30111222));
        servicio.cancelarReserva(30111222, "AR100"); servicio.eliminarVuelo("AR100");
        assertTrue(servicio.buscarVuelo("AR100").isEmpty());
        assertTrue(servicio.buscarPasajero(30111222).isPresent());
    }

    @Test void inicioEmbarqueImpideCambiarReservasYReabrirElVuelo() {
        servicio.registrarVuelo(nacional("AR100", 2)); servicio.registrarPersona(pasajero(30111222, "")); servicio.reservar(30111222, "AR100");
        servicio.cambiarEstado("AR100", EstadoVuelo.EN_VUELO);
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarReserva(30111222, "AR100"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarEstado("AR100", EstadoVuelo.PROGRAMADO));
    }

    @Test void repositorioGenericoActualizaUnDetachedYPermiteConsultaYBorrado() {
        IRepositorio<Pasajero> repo = new RepositorioJdo<>(contexto, Pasajero.class);
        Pasajero copia = repo.guardar(pasajero(30111222, "AA123"));
        assertTrue(JDOHelper.isDetached(copia));
        copia.setNombre("Mar\u00eda"); repo.guardar(copia);
        assertEquals("Mar\u00eda", repo.consultar(30111222).orElseThrow().getNombre());
        assertTrue(repo.eliminar(30111222)); assertFalse(repo.eliminar(30111222));
    }

    @Test void datosYRelacionesSobrevivenAlCierreYReapertura(@TempDir Path carpeta) {
        String url = "jdbc:h2:" + carpeta.resolve("aerolinea");
        try (ContextoJdo primero = contexto(url)) {
            ServicioAerolinea s = new ServicioAerolinea(primero);
            s.registrarVuelo(nacional("AR100", 2)); s.registrarPersona(pasajero(30111222, "AA123")); s.reservar(30111222, "AR100");
        }
        try (ContextoJdo segundo = contexto(url)) {
            ServicioAerolinea s = new ServicioAerolinea(segundo);
            assertEquals(1, s.buscarVuelo("AR100").orElseThrow().getAsientosOcupados());
            assertEquals("AA123", s.buscarPasajero(30111222).orElseThrow().getNumeroPasaporte());
            assertEquals(1, s.consultarAerolinea().orElseThrow().getVuelos().size());
        }
    }

    @Test void reservasConcurrentesNoSuperanLaCapacidad() throws Exception {
        servicio.registrarVuelo(nacional("AR100", 1)); servicio.registrarPersona(pasajero(30111222, "")); servicio.registrarPersona(pasajero(30111223, ""));
        CyclicBarrier barrera = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> tareas = List.of(tareaReserva(30111222, barrera), tareaReserva(30111223, barrera));
            List<Future<Boolean>> resultados = executor.invokeAll(tareas, 20, TimeUnit.SECONDS);
            long exitos = 0;
            for (Future<Boolean> resultado : resultados) if (resultado.get()) exitos++;
            assertEquals(1, exitos);
            assertEquals(1, servicio.buscarVuelo("AR100").orElseThrow().getAsientosOcupados());
        } finally { executor.shutdownNow(); }
    }
    private Callable<Boolean> tareaReserva(int dni, CyclicBarrier barrera) {
        return () -> {
            try {
                contexto.transaccion(pm -> {
                    Vuelo v = pm.getObjectById(Vuelo.class, "AR100"); Pasajero p = pm.getObjectById(Pasajero.class, dni);
                    v.getPasajeros().size();
                    try { barrera.await(10, TimeUnit.SECONDS); } catch (Exception e) { throw new IllegalStateException(e); }
                    v.reservarPasajero(p); return null;
                });
                return true;
            } catch (PersistenciaException error) { return false; }
        };
    }
}
