# Validacion del entregable

Fecha: 11 de octubre de 2026.

La version inicial supero el ciclo `mvn clean verify` con Java 17 y Maven 3.9.9: 18 pruebas aprobadas.

En esta actualizacion se normalizaron a ASCII los mensajes, titulos y archivos de texto del proyecto. Los datos internacionales de las pruebas se conservaron usando escapes Unicode en el codigo Java. Se recompilo con el compilador de Java 17 y las dependencias originales del POM, se ejecuto el enhancement de las nueve entidades y se corrieron nuevamente las mismas pruebas con JUnit Platform Console 1.11.4 y H2 real.

Resultado de la actualizacion: **18 pruebas ejecutadas, 18 aprobadas, 0 fallos y 0 omitidas**. Tambien se comprobo que la salida de consola del menu, validaciones, alta, consulta y reserva contiene solo ASCII cuando se ingresan datos ASCII. La reapertura conserva la reserva. El esquema de persistencia no cambia.

| Comprobacion | Resultado |
| --- | --- |
| Integracion de persistencia | 17 pruebas aprobadas sobre H2 real |
| Interaccion de consola | 1 prueba aprobada con alta, reserva, consulta y cancelacion |
| Jerarquias de entidades | Consulta de clases padre y recuperacion de clases concretas |
| Relaciones | Pasaporte/titular, vuelo/pasajero, aerolinea/vuelos/personas y tripulacion |
| Transacciones | Rollback de edicion invalida y lote con pasaporte duplicado |
| Integridad | Borrados protegidos; pasaporte dependiente sin huerfanos; documento opcional |
| Ciclo de vida | Lectura tras cerrar el PM y actualizacion de objetos detached |
| Consultas | JDOQL nombrado, parametros literales, filtros y orden |
| Persistencia en disco | Cierre de la fabrica y reapertura de una base H2 con sus relaciones |
| Concurrencia | Dos transacciones intentan reservar el ultimo asiento; se confirma solo una |
| Indices de colecciones | Personas sin reserva y reservas activas despues de cancelar |

Los resumenes originales de Surefire y el resultado de esta actualizacion (`resultado-ascii.txt`) estan en `pruebas/`. La distribucion incluye el JAR de la aplicacion y nueve dependencias de ejecucion en `dist/lib`. Las clases fueron compiladas para Java 17.

Ademas se comprobo el JAR con `--ayuda` y una sesion de consola que crea un vuelo, registra un pasajero y reserva un asiento. Un segundo proceso recupero el vuelo, el pasaporte y la ocupacion desde la base H2 predeterminada.

La correccion del borrado de pasaportes utiliza una clave foranea nullable y la politica de borrado de DataNucleus que respeta esa clave. Esto evita actualizar el titular despues de haberlo eliminado, sin desactivar el control de version de las personas o los vuelos.

## Comprobaciones pendientes

- La conexion a un servidor MySQL real. El controlador y la unidad de persistencia estan incluidos.
- La presentacion visual e interaccion manual de Swing en un escritorio. La interfaz compila y utiliza el mismo servicio probado por la consola.
- La ejecucion del workflow en GitHub. La conexion disponible devolvio `403 Resource not accessible by integration` al intentar escribir; este paquete no esta publicado alli.
- La migracion de datos de la version anterior. No se proporcionaron los archivos reales de la base o de serializacion; esta version crea una base nueva.
- La revision integra del audio y de eventuales consignas verbales. La revision del contenido fue visual y por muestreo. Los tres archivos inicialmente senalados como truncados ya se obtuvieron completos y se verificaron; ver `CLASES_8_A_11.md`.
