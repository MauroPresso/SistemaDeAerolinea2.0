# SistemaDeAerolinea 2.0

Proyecto independiente de Programacion II (IFES), basado en el dominio de `MauroPresso/SistemaDeAerolinea` y en los conceptos del ejemplo `juanjosenava75-bot/proyecto-jdo`. Java 17, Maven, DataNucleus/JDO y base de datos relacional.

Los mensajes, titulos y etiquetas del programa usan solo ASCII: sin tildes, enies ni simbolos especiales. La actualizacion conserva el esquema de H2 y no modifica los datos existentes.

## Empezar en Windows

Extraer la carpeta completa `SistemaDeAerolinea2.0` en una ubicacion distinta del proyecto anterior; por ejemplo, junto a `SistemaDeAerolinea`. Abrir como proyecto la carpeta que contiene `pom.xml`, `src` y `dist`.

**No hace falta instalar MySQL para ejecutarlo.** La distribucion incluye H2 y las demas dependencias. Con Java 17 disponible, se puede abrir `ejecutar-consola.bat` o `ejecutar-swing.bat`. Maven se necesita cuando se quiere recompilar el codigo.

El proyecto puede probarse antes de crear un repositorio en GitHub. Para mantener la version 2.0 independiente, crear despues un repositorio nuevo en la propia cuenta; el repositorio del profesor sigue siendo una referencia.

Para iniciar la distribucion compilada desde PowerShell:

```powershell
cd SistemaDeAerolinea2.0
java -jar dist\SistemaDeAerolinea2.0.jar --consola
```

Para la interfaz Swing, ejecutar el mismo comando sin `--consola`. La carpeta `dist/lib` debe permanecer junto al JAR: DataNucleus necesita conservar los metadatos de sus dependencias.

Para compilar el codigo y ejecutar las pruebas:

```powershell
mvn clean verify
java -jar target\SistemaDeAerolinea2.0.jar --consola
```

Requisitos: JDK 17 y, para compilar, Maven. `--orm` se acepta como opcion compatible con la version anterior; JDO es ahora el modo predeterminado. `--ayuda` muestra las opciones de inicio. En equipos sin escritorio se utiliza automaticamente la consola.

## Operaciones

- Alta, consulta, modificacion de datos comunes y eliminacion de vuelos nacionales, internacionales y charter.
- Alta, edicion, consulta y eliminacion de pasajeros.
- Reserva y cancelacion con validacion de disponibilidad, duplicados, estado y pasaporte.
- Registro de tripulantes y asignacion a vuelos.
- Consultas JDOQL nombradas por destino, apellido, vuelos programados y orden por destino.
- Ocupacion total de vuelos programados.

Las confirmaciones de reserva, cancelacion, cambio de estado y eliminacion se solicitan en ambas interfaces. El mensaje de exito se muestra despues del commit. Un error deja la base en su estado anterior.

## Persistencia

Por defecto se crea `data/aerolinea2.mv.db`. No hay que instalar un servidor para usar H2.

La version 2.0 utiliza **una base nueva**. No abre ni modifica `aerolinea_orm.mv.db`, `vuelos.dat` o `personas.dat` de la version 1.0. Los datos anteriores no estan incorporados en este entregable: deben migrarse a partir de los archivos reales, conservando sus relaciones y comprobando el resultado.

El proyecto tambien declara la unidad `Aerolinea-MySQL`, equivalente al motor utilizado en los videos. Crear previamente la base vacia `aerolinea2` y un usuario con acceso. Configurar las credenciales en el entorno de PowerShell:

```powershell
$env:AEROLINEA_DB_URL = 'jdbc:mysql://localhost:3306/aerolinea2'
$env:AEROLINEA_DB_USER = 'tu_usuario'
$env:AEROLINEA_DB_PASSWORD = 'tu_clave'
java -jar dist\SistemaDeAerolinea2.0.jar --mysql --consola
```

El codigo no incluye credenciales personales. H2 se utiliza para las pruebas automaticas; la conexion MySQL requiere una comprobacion con el servidor real.

## Arquitectura y clases

| Paquete | Responsabilidad |
| --- | --- |
| `aerolinea.dominio` | Entidades, herencia y reglas de disponibilidad |
| `aerolinea.repositorio` | Repositorio generico y ciclo de vida JDO |
| `aerolinea.servicio` | Servicio generico y operaciones atomicas de aplicacion |
| `aerolinea.ui` | Consola y Swing, con el mismo servicio |
| `aerolinea.main` | Seleccion del motor e inicio/cierre |
| `aerolinea.util` | Comparadores y calculo de tarifas conservados |

Las jerarquias `Persona -> Pasajero/Tripulante` y `Vuelo -> Nacional/Internacional/Charter` usan `NEW_TABLE`, con discriminador de clase y control de version. Las consultas a las clases padre recuperan sus subclases.

| Relacion | Implementacion |
| --- | --- |
| Pasajero <-> Pasaporte vigente | 1 a 1 bidireccional; documento opcional, numero unico |
| Aerolinea -> Vuelos | 1 a N con tabla de union y orden |
| Aerolinea -> Personas registradas | 1 a N; incluye personas que aun no reservaron |
| Vuelo <-> Pasajero | N a M bidireccional; representa las reservas actuales |
| Vuelo -> Tripulante | Coleccion unidireccional con tabla de union |

La reserva mantiene los dos lados de la relacion. Cancelarla libera el asiento y conserva las otras reservas del pasajero. El numero de vuelo y el DNI son identidades estables. Un mismo numero de vuelo representa un unico vuelo fechado en esta version academica.

Los indices `HashMap` de personas por DNI y `HashSet` de pasajeros con reserva activa se calculan desde las colecciones persistidas. Se reconstruyen correctamente tras volver a abrir la base y despues de cancelar una reserva. La relacion con el pasaporte declara una clave foranea nullable; al eliminar el documento, el pasajero permanece registrado sin el.

`ContextoJdo` conserva una fabrica por aplicacion, crea un `PersistenceManager` por operacion, realiza begin/commit/rollback y lo cierra siempre. Las consultas ejecutan la query y devuelven copias detached con el grafo cargado. El bloqueo optimista del vuelo impide que dos reservas concurrentes excedan su capacidad.

Los repositorios y servicios genericos ofrecen operaciones CRUD de infraestructura. Las interfaces de usuario acceden a `ServicioAerolinea`, que aplica las restricciones del negocio, incluidas las protecciones de borrado.

## Validacion

La version inicial supero `mvn clean verify` con Java 17. Esta actualizacion ASCII se recompilo con Java 17, se ejecuto el enhancement JDO y se volvieron a ejecutar las mismas pruebas con JUnit Platform Console: **18 pruebas aprobadas, sin errores ni fallos**. Las pruebas de integracion utilizan H2 real y clases enhanced. Verifican las jerarquias, las relaciones en ambos sentidos, consultas parametrizadas, edicion sin perdida de reservas, rollback, lotes atomicos, proteccion de borrados, objetos detached, cierre/reapertura de la base, reservas concurrentes y consola completa.

El informe de pruebas se genera en `target/surefire-reports`. El workflow incluido ejecuta `mvn clean verify`, inicia el JAR y prepara una distribucion con fuentes y dependencias. La interfaz grafica requiere un escritorio para comprobar su presentacion visual.

La validacion de este entregable y sus limites se detallan en [docs/VALIDACION.md](docs/VALIDACION.md). El workflow esta preparado para un repositorio independiente; todavia no se ejecuto en GitHub.

## Material de clase

La correspondencia y las limitaciones de los videos recibidos se detallan en [docs/CLASES_8_A_11.md](docs/CLASES_8_A_11.md). Las clases se revisaron mediante muestreo visual de pantallas y codigo; este documento no es una transcripcion del audio.

## Autoria y procedencia

El proyecto original de Programacion I fue desarrollado por Agustin Almiron, Juan Cruz Barria y Mauro Presso. La version 2.0 conserva su dominio y lo organiza como un proyecto independiente con persistencia directa por objeto. Referencias de codigo: version original `dc6dc31b21f78aea4da8067aa1a299336665da16` y ejemplo JDO del profesor `0ee6add02ff9aea6f2c92fb46e362208ae9cad52`.
