# Correspondencia con las clases 8 a 11

La reforma utiliza el repositorio original de la aerolinea, el ejemplo JDO del profesor y pantallas extraidas de los videos compartidos. La revision de los videos es un muestreo visual; no se afirma haber revisado integramente el audio.

| Clase | Contenido observado | Aplicacion |
| --- | --- | --- |
| 8, parte 1 | `PersistenceManagerFactory`, apertura y cierre de `PersistenceManager` (pantallas de 00:01:00 y 00:04:00; duracion total 23:15) | `ContextoJdo` con una fabrica compartida y managers por operacion |
| 8, parte 2 | Relaciones 1 a 1 unidireccionales y bidireccionales (00:01:00 a 00:22:00) | Pasajero y Pasaporte, FK unica y `mappedBy` inverso |
| 9, parte 1 | Colecciones 1 a N, tablas de union y `makePersistentAll` | Aerolinea y coleccion de vuelos; guardado de lotes |
| 9, parte 2 | Relaciones N a 1 y N a M, ambos lados y `equals/hashCode` | Vuelo/Pasajero bidireccional e identidades estables |
| 9, parte 3 | Herencia, estrategias de tablas y discriminadores | Clases padre/hijas persistentes, `NEW_TABLE` y discriminador de clase |
| 10, parte 1 | Transacciones locales y begin/commit/rollback (duracion total 16:27) | Operaciones atomicas y rollback en el bloque finally |
| 10, parte 2 | Ciclo de vida transient/persistent/detached (duracion total 24:42) | Retorno detached, consulta fuera del PM y actualizacion de objetos detached |
| 11, parte 1 | Persistencia por alcance, objetos relacionados y coleccion raiz | Grafo de aerolinea/vuelos y pasaporte/titular |
| 11, parte 2 | JDOQL, query nombrada, ejecucion y parametros | Consultas por destino, apellido, estado y orden |

## Estado de los archivos

Los tres videos que inicialmente se senalaron como incompletos se volvieron a obtener desde sus archivos guardados. Los originales recibidos se abren correctamente. El problema estaba en las primeras copias locales, descargadas parcialmente.

| Archivo | Duracion | Comprobacion |
| --- | --- | --- |
| `CLASE 8 VIDEO 1-1080.mp4` | 23:15 | Indice MP4, video y audio al inicio y al final correctos |
| `CLASE 10 VIDEO 1-1080.mp4` | 16:27 | Indice MP4, video y audio al inicio y al final correctos |
| `CLASE 10 VIDEO 2-1080.mp4` | 24:42 | Indice MP4, video y audio al inicio y al final correctos |

No hace falta volver a subir estos tres archivos. Los seis videos restantes ya se habian podido abrir. La comprobacion de integridad de los archivos es distinta de la revision del contenido: la correspondencia anterior se basa en muestreo visual y no en una transcripcion completa del audio.

## Decisiones propias de la adaptacion

- El profesor muestra MySQL; se mantiene soporte para MySQL y H2 como opcion inicial ejecutable sin servidor.
- Se elige `NEW_TABLE` para separar claramente las jerarquias, en lugar de alternar estrategias durante el laboratorio.
- El pasaporte vigente se modela como objeto opcional para aplicar una relacion 1 a 1 que pertenece al dominio de la aerolinea.
- La reserva se representa mediante la relacion N a M entre vuelo y pasajero, como en la version anterior. Esta version no anade historial de reservas canceladas ni un sistema de emision de boletos.
- La base 2.0 tiene un nombre distinto porque su esquema incluye entidades y relaciones nuevas; no se migra la base 1.0 por apertura automatica.

## Documentacion oficial

- [JDO Persistence Guide](https://www.datanucleus.org/products/accessplatform_6_0/jdo/persistence.html)
- [JDO Mapping Guide](https://www.datanucleus.org/products/accessplatform_6_0/jdo/mapping.html)
- [JDO Query Guide](https://www.datanucleus.org/products/accessplatform_6_0/jdo/query.html)

Estas guias respaldan el uso de una fabrica por datastore, cierre de managers, transacciones locales, detach, relaciones, herencia y consultas nombradas con parametros.
