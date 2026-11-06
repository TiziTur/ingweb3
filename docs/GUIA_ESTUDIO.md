---
title: "Guía de estudio - Trabajo Integrador IW3"
subtitle: "La teoría de las clases y cómo se aplica en nuestro sistema de órdenes de carga"
date: "Preparada para la defensa del 10/11/2026"
lang: es
toc-title: "Índice"
---

# 1. Cómo usar esta guía

Esta guía junta dos cosas:

- **La teoría que dio el profesor en clase**, sacada de las grabaciones de todas las clases (del 4/8 al
  20/10) y de los dos videos de la consigna.
- **Cómo aplicamos esa teoría en nuestro trabajo integrador**, con los nombres reales de las clases,
  métodos y archivos del repo.

Cada tema tiene tres partes: **Lo que vimos en clase**, **En nuestro TP** y **Para responder rápido**
(frases cortas para la defensa). El detalle completo de cada punto del TP sigue estando en
`docs/DOCUMENTACION.md` (el Word del TP); esta guía lo complementa con el "por qué" teórico.

## 1.1. Cómo es la defensa (lo que dijo el profesor)

| Qué dijo | Qué significa para nosotros |
|----------------------------------------------|------------------------------------------------------------|
| "Tenemos un grupo de cuatro, no hay problema con eso" (video de criterios) | El grupo de 4 está aprobado. Duda resuelta. |
| Presentación de 15 a 20 minutos por grupo, mostrando el sistema **con Postman**; "tiene que estar todo listo y funcionando" | Llevar la colección corrida antes (local y VM) y el entorno con la contraseña cargada. |
| Después hace "una o dos preguntas puntuales a cada una de las personas", sin importar el rol | Todos tienen que saber todo, no solo su parte. |
| No repregunta; quiere respuestas concisas y sin vueltas. "La persona que no me contesta, no aprueba" | Practicar respuestas de 2 o 3 oraciones. |
| "Cada uno tiene que tener disponible su código en su computadora; le voy a decir **búscame tal parte y explicámela**. Si tardaste más de 30 segundos en encontrarlo y explicármelo, se acabó" (clase de autenticación) | Tener el proyecto abierto en el IDE y saber de memoria en qué clase está cada cosa (ver sección 2). |
| "Esto [la autenticación] me interesa mucho que lo sepan; suelo hacer bastantes preguntas de esto" | Estudiar muy bien la sección 13 (JWT, filtros, roles). |
| La documentación con OpenAPI/Swagger y una colección de Postman con tests del circuito completo dan "valor agregado muy grande" | Lo tenemos: mostrarlo. |
| La definición de los endpoints "queda a criterio de los grupos" | Hay que poder justificar nuestras rutas (ver 4.3). |

## 1.2. Plan de estudio hasta el martes

| Día | Qué estudiar | Cómo |
|-------------|------------------------------------------|------------------------------------------------------------|
| Viernes | Secciones 2 a 8 (HTTP, REST, Docker, Spring, patrón de capas) | Leer y abrir cada clase nombrada en el IDE. Correr la app y seguir un request con el debugger. |
| Sábado | Secciones 9 a 12 (JPA, estados, errores, serialización) y sección 17 (circuito) | Correr la colección de Postman y mirar en la base qué tablas cambian en cada paso. |
| Domingo | Secciones 13 a 16 (seguridad, configuración, Swagger, deploy) | Probar login con cada usuario, un 401 y un 403. Leer `deploy.sh` línea por línea. |
| Lunes | Secciones 18 y 19 (puntos débiles y preguntas) + ensayo de la presentación | Simulacro: uno pregunta "buscame tal cosa" y el otro tiene 30 segundos. |

# 2. Mapa rápido: dónde está cada cosa

Esto es lo que hay que encontrar en menos de 30 segundos. Todo está bajo
`iw3/src/main/java/ar/edu/iua/iw3/`.

| Si te piden... | Abrí |
|------------------------------------------|------------------------------------------------------------|
| Una entidad (tabla) | `model/Orden.java`, `Camion`, `Cisterna`, `Chofer`, `Cliente`, `Producto`, `DetalleCarga`, `CambioEstado` |
| Los estados | `model/EstadoOrden.java` (enum) |
| Un repositorio / una consulta | `model/persistence/` (por ejemplo `DetalleCargaRepository` con los `AVG`) |
| Los cuerpos de request/response | `model/dto/` (`PesajeRequest`, `ActivacionRequest`, `DatoCargaRequest`, `PasswordResponse`, `PresetResponse`, `Conciliacion`) |
| La lógica de un punto | `business/OrdenBusiness.java` (`recibirOrden`, `registrarPesajeInicial`, `activarCarga`, `recibirDatoCarga`, `cerrarOrden`, `registrarPesajeFinal`, `conciliacion`) |
| El control de estados y la auditoría | `OrdenBusiness`: `controlarEstado()`, `cambiarEstado()`, `usuarioActual()` |
| La frecuencia del detalle | `OrdenBusiness.correspondeGuardarDetalle()` y la propiedad `carga.detalle.frecuencia.segundos` |
| La generación de la contraseña | `OrdenBusiness.generarPassword()` |
| La sincronización por código externo | `business/CamionBusiness.java` (y `ChoferBusiness`, `ClienteBusiness`, `ProductoBusiness`) |
| Las excepciones | `business/execption/` |
| Los endpoints y los roles | `controllers/OrdenRestController.java` (`@PreAuthorize` en cada método) |
| Las rutas | `controllers/Constants.java` |
| El usuario logueado | `controllers/BaseRestController.getUserLogged()` |
| El login y el token | `auth/controller/AuthRestController.java` |
| El filtro que valida el token | `auth/filters/JWTAuthorizationFilter.java` |
| Validar usuario y contraseña | `auth/custom/CustomAuthenticationManager.java` |
| Qué rutas son públicas, BCrypt, stateless | `security/SecurityConfiguration.java` |
| Usuarios y roles iniciales | `auth/DatosIniciales.java` |
| La respuesta de error | `util/StandartResponse.java` y `StandartResponseBusiness` |
| Swagger | `config/OpenApiConfig.java` |
| Configuración | `src/main/resources/application.properties`, `application-mysqldev.properties`, `application-mysqlprod.properties` |
| Perfiles de empaquetado (jar/war) | `iw3/pom.xml` (`<profiles>`) |
| Arranque, zona horaria, perfil | `Iw3Application.java` |
| Deploy | `infra/deploy.sh` |
| Postman | `postman/IW3-Integrador.postman_collection.json` |

# 3. HTTP y la arquitectura cliente-servidor

## Lo que vimos en clase

- **Cliente-servidor.** El servidor es *pasivo*: abre un puerto y espera. El cliente es *activo*: inicia
  con un *request* y recibe un *response*. Programar un backend es programar el rol de servidor.
- **Backend enterprise.** Sistemas con muchas entidades y reglas de negocio: consultan la base,
  procesan y devuelven en el formato que pide el cliente.
- **B2B (business to business).** Un sistema se comunica con otro de forma automática y desatendida
  (antes era H2B, humano-sistema). Para eso los datos necesitan **metadatos** que digan qué es cada
  dato: XML (muchos metadatos), **JSON** (el estándar actual de transporte) y YAML (más usado para
  configuración).
- **HTTP** es un protocolo de aplicación, de texto plano y **sin estado**: cada request-response es
  independiente (más robusto y escalable). La "sesión" se arma con cabeceras (en la materia, con un
  token JWT).
- Se extiende con **cabeceras** (`nombre: valor`). Muchos comportamientos del navegador y muchas
  protecciones de seguridad son cabeceras.
- **HTTPS** = HTTP sobre TLS (cifrado) sobre TCP sobre IP.
- **Forma del request:** `MÉTODO PATH VERSIÓN`, cabeceras, una **línea en blanco obligatoria**, body
  opcional. **Response:** `VERSIÓN CÓDIGO DESCRIPCIÓN`, cabeceras, línea en blanco, body.
- **Códigos de estado:** 2xx éxito (200 OK, **201 Created**), 3xx redirección (**302 + `Location`**),
  4xx error del cliente (**401** no autenticado, **403** autenticado pero sin permiso, **404** no
  existe), 5xx error del servidor (**500**).
- `Content-Type` (tipo MIME) dice cómo interpretar el body.
- Puertos conocidos: 80 HTTP, 443 HTTPS, 22 SSH, 25 SMTP. Abrir un puerto menor a 1024 requiere sudo.

## En nuestro TP

- Los cuatro sistemas del caso son un ejemplo de **B2B**: SAP, TMS y el sistema de carga hablan con
  nuestro backend por HTTP + JSON, sin intervención humana.
- Usamos los códigos así: **201** al crear la orden (con `Location: /api/v1/ordenes/{numero}`), **200**
  en el resto de los éxitos, **302** si la orden ya existe, **400** datos inválidos o dato descartado,
  **401** sin token o login incorrecto, **403** rol sin permiso, **404** orden inexistente, **409**
  estado incorrecto, **500** error inesperado.
- El 409 (`Conflict`) no se vio en clase con ese nombre, pero sigue la misma idea: un código distinto
  para cada situación (lo que el profe llama "hypermedia types").

## Para responder rápido

- *¿Por qué 201 y no 200 al crear?* Porque 201 significa "Created": dice más que un simple OK, y va con
  `Location` para que el cliente encuentre lo creado si lo necesita.
- *¿Diferencia entre 401 y 403?* 401: no sé quién sos (no hay token o es inválido). 403: sé quién sos,
  pero tu rol no puede usar ese endpoint.

# 4. REST

## Lo que vimos en clase

REST no es una tecnología sino un **conjunto de restricciones** para aplicaciones en red:

| Restricción | Idea |
|------------------------|------------------------------------------------------------|
| Cliente-servidor | Responsabilidades separadas: frontend y backend evolucionan por separado. |
| **Sin estado** (stateless) | Cada request trae todo lo necesario. El estado lo guarda el cliente; si lo guardara el servidor, la memoria crecería con cada cliente. |
| Caché | Cada respuesta dice si se puede cachear (una foto sí, un saldo bancario no). |
| Contrato uniforme | La **API** es el contrato que respetan servidor y clientes. Servicios abstractos y genéricos, sin pensar en un cliente en particular. |
| Sistema en capas | Puede haber intermediarios (proxy, caché, balanceador) y nadie se tiene que dar cuenta. |

Terminología: **recurso** (fuente de información), identificado por una **URI**; **representación**
(el estado del recurso en un formato, por ejemplo JSON).

Buenas prácticas para las URIs:

- Centradas en recursos: **sustantivos**, no verbos (`/cuenta/pago` y no `/cuenta/pagar`). El verbo
  lo pone el método HTTP: GET obtener, POST crear, PUT modificar, DELETE borrar.
- Plural o singular, pero siempre igual (lo más usado es plural).
- Minúsculas y **guion medio** para separar palabras (`usuario-principal`), nada de camelCase.
- El formato no va en la URL (`/usuarios/27.json` está mal): va en la cabecera `Accept`.
- **Versionar** la API (`/api/v1/...`) desde el principio, con constantes para cambiarla en un solo lugar.

## En nuestro TP

- Rutas: `/api/v1/ordenes`, en plural, minúsculas, con guion medio (`pesaje-inicial`, `pesaje-final`) y
  versión en `Constants.URL_BASE`.
- La orden se identifica por su `numeroOrden` en la URL (`/ordenes/{numeroOrden}/...`), porque es lo
  que conocen los sistemas externos.
- **Stateless:** el backend no guarda sesiones (`SessionCreationPolicy.STATELESS` en
  `SecurityConfiguration`); toda la información del usuario viaja en el token JWT.
- **Sistema en capas:** en la VM hay un nginx adelante del Tomcat y ni el cliente ni el backend lo
  notan.

## 4.3. Ojo: `activar` y `cerrar` son verbos

El profe dijo que las URIs no deben tener verbos. Dos de nuestras rutas los tienen
(`POST /ordenes/{n}/activar` y `POST /ordenes/{n}/cerrar`). Si lo pregunta, la respuesta es:

> No son operaciones CRUD sobre un recurso sino **transiciones de estado** de la orden que pide la
> consigna ("un endpoint que cierra la orden"). Elegimos nombres que se entienden al leerlos. La
> alternativa más REST sería modelarlas como sustantivos, por ejemplo `POST /ordenes/{n}/cierre` o
> `PUT /ordenes/{n}/estado`. La consigna deja la definición de los endpoints a criterio del grupo.

No cambiar las rutas antes de la defensa: la colección de Postman y la documentación dependen de ellas.

# 5. Docker

## Lo que vimos en clase

- **Imagen** (tiempo de diseño, se define con un `Dockerfile` que empieza con `FROM`) y **contenedor**
  (tiempo de ejecución). Como clase y objeto.
- `docker run` = crear + iniciar (se hace una sola vez por contenedor). `start`, `stop`, `restart`,
  `exec -it <contenedor> bash` (ejecutar algo en un contenedor que ya está corriendo).
- Diferencia con una VM: la VM tiene un sistema operativo completo sobre un hipervisor; Docker comparte
  el kernel, con lo cual entran docenas de contenedores donde entrarían 2 o 3 VMs.
- **Docker Hub**, imágenes oficiales y **tags** (versiones; `latest` por defecto).
- `docker ps` (contenedores andando; *ps* porque son procesos), `ps -a`, `docker logs --tail 100 -f`
  ("la herramienta más útil: en un backend los errores no se ven, están en el log").
- **Volúmenes:** con nombre (persisten datos, por ejemplo la base de MySQL; permiten actualizar MySQL
  sin perder datos) y *bind* (una carpeta del host).
- Parámetros de `run`: `-d` (segundo plano), `--name`, `--restart always`, `-e VAR=valor` (variables
  de entorno, la forma estándar de configurar un contenedor), `-p host:contenedor` (regla de todos los
  mapeos: **a la izquierda el host, a la derecha el contenedor**), `-v`.
- **Docker Compose:** un archivo YAML con `services:` (cada contenedor es un servicio), redes y
  volúmenes, para manejar toda la infraestructura junta. El nombre del servicio es el nombre de host.
- `ports` abre el puerto en la máquina real (queda expuesto a internet); `expose` solo lo comparte con
  los otros servicios del compose.
- Las contraseñas van en un archivo oculto **`.env`** y en el compose se usan como `${VARIABLE}`.

## En nuestro TP

- En la PC, MySQL corre en Docker: `docker run -d --name mysql-iw3 -p 33306:3306 -e ... mysql:8.0.43`
  (README).
- En la VM, la infraestructura del profesor tiene cuatro servicios: **nginx** (único puerto abierto:
  443), **backend** (Tomcat con Java 21, `expose: 8080`), **mysql** (sin puerto hacia afuera, volumen
  para la base) y **adminer**.
- El backend de producción se conecta a `jdbc:mysql://mysql:3306/...`: `mysql` es el **nombre del
  servicio** en el compose, por la red interna de Docker.
- La contraseña de los usuarios de la VM está en `~/infra/.env` como `USUARIOS_PASSWORD` y se pasa al
  backend en `environment:` del compose. No está en el repo.
- `deploy.sh` compila con Maven **dentro de un contenedor temporal** (`docker run --rm`), así no hace
  falta instalar Maven en la VM.

## Para responder rápido

- *¿Por qué MySQL no tiene `ports` en la VM?* Para no exponer la base a internet; solo la ven los
  otros contenedores por la red interna.
- *¿Para qué el volumen de MySQL?* Para que los datos sobrevivan si se borra o actualiza el contenedor.

# 6. El proyecto Spring Boot: Maven, empaquetado y perfiles

## Lo que vimos en clase

- El proyecto se generó con **start.spring.io**. **Maven** maneja las dependencias (incluidas las
  transitivas) y el ciclo de compilación. Un artefacto se identifica con **groupId + artifactId +
  version**. Las dependencias se guardan una sola vez en `~/.m2`.
- Dependencias base: Spring Web, Spring Data JPA, MySQL Driver, Spring Security, Lombok (no funcional,
  genera código en compilación).
- Dos archivos centrales: `pom.xml` (Maven) y `application.properties` (Spring).
- **jar** autocontenido: aplicación + librerías + Tomcat embebido; se corre con `java -jar`.
  **war**: todo menos el servidor; se deposita en un Tomcat que ya existe (`webapps/ROOT`).
- **Perfiles de ejecución:** archivos `application-<perfil>.properties` que funcionan como herencia:
  sobrescriben lo del archivo padre y heredan el resto. Se activan con `spring.profiles.active`.
- **Perfiles de empaquetado** en el `pom.xml`: `war-build` (por defecto, Tomcat `provided`, genera
  `ROOT.war`) y `jar-build` (`-Dbuild=jar`, Tomcat `runtime`, genera `iw3.jar`).
- Comando: `mvn clean package -Dbuild=war -Dmaven.test.skip=true` (`clean` para compilar de cero;
  los tests se saltean porque necesitan base y perfil).
- Para que la misma aplicación sirva como jar y como war, la clase principal extiende
  **`SpringBootServletInitializer`**.
- En producción: `dev.info.enabled=false`, sin `show-sql`, logs solo WARN/ERROR y `ddl-auto` en
  `validate` o `none` ("sería una locura poner update en producción").
- En la clase de extensión del modelo se vieron perfiles por cliente (`cli1`, `cli2`) que cargaban o no
  paquetes enteros con `@Profile` y clases `ScanConfig`: una forma de hacer backends *multitenancy*.

## En nuestro TP

- Perfiles: `mysqldev` (PC, `localhost:33306`, base `iw3_2026`) y `mysqlprod` (VM, host `mysql`, base
  `iw3_prod_2026`, `dev.info.enabled=false`). Por defecto queda `mysqldev`.
- En la VM el perfil lo define `catalina.properties` del Tomcat (`spring.profiles.active=mysqlprod`).
- `Iw3Application extends SpringBootServletInitializer implements CommandLineRunner`: se despliega
  como war y al arrancar muestra en el log la zona horaria y el perfil activo.
- Quitamos los perfiles `cli1`/`cli2`, sus paquetes y las `ScanConfig` porque eran ejemplos de clase
  (el profesor pidió dejar solo dev y prod).
- **Punto a saber:** en `application-mysqlprod.properties` dejamos `ddl-auto=update` para que las
  tablas se creen solas en la VM. El profe recomienda `validate` en producción. Respuesta: con
  `validate` la primera vez falla porque las tablas no existen (eso mismo pasó en la clase); en un
  sistema real se crearían las tablas con un script y se pasaría a `validate`.

# 7. El patrón de capas del backend

## Lo que vimos en clase

El profe lo llamó "patrón arquitectónico de diseño del backend" y se repite para cada entidad:

1. **Modelo** (`model/`): la entidad JPA. "Diseñar bien las entidades es una de las partes más
   importantes."
2. **Persistencia** (`model/persistence/`): una interfaz que extiende `JpaRepository<Entidad, Long>`.
   Es el **DAO**; Spring lo implementa solo. Está dentro de `model` porque es *no funcional*: no es el
   problema de negocio, es una ayuda para el modelo.
3. **Excepciones propias** (una sola vez): pocas excepciones (6 a 8) que envuelven a todas las de las
   librerías sin perder la original.
4. **Negocio** (`business/`): una interfaz `IXxxBusiness` con los servicios y una implementación
   `XxxBusiness` con `@Service`. "Acá es donde se pone cabeza y se escribe código." Siempre se usa la
   interfaz: así se puede cambiar la implementación sin tocar a quien la usa.
5. **Controlador REST** (`controllers/`): expone los servicios de negocio a la web; mapea el mundo
   Java con el mundo HTTP. Sin lógica.

Conceptos que van con el patrón:

- **Inversión de control (IoC) / inyección de dependencias:** no hacemos `new`; con `@Autowired`
  Spring busca el candidato y lo instancia. Spring nació como contenedor de IoC.
- **Candidatos de instanciación:** `@Repository`, `@Service`, `@RestController`, `@Configuration`,
  `@Bean` (el resultado de un método).
- **Anotaciones:** inyectan código en compilación o ejecución (programación declarativa, orientada a
  aspectos).
- **Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Slf4j`.
- **Constants** (clase `final`) para no repetir textos y **BaseRestController** como padre de todos los
  controladores.
- "A veces uno mira la estructura de paquetes y dice: volvete a tu casa." Paquetes bien separados por
  responsabilidad.

## En nuestro TP

| Capa | Nuestro código |
|----------|------------------------------------------------------------|
| Modelo | `Orden`, `Camion`, `Cisterna`, `Chofer`, `Cliente`, `Producto`, `DetalleCarga`, `CambioEstado`, `EstadoOrden` |
| Persistencia | `OrdenRepository`, `CamionRepository`, ..., `DetalleCargaRepository`, `CambioEstadoRepository` |
| Excepciones | `BusinessException` (500), `NotFoundException` (404), `FoundException` (302) del proyecto base, más `BadRequestException` (400) y `ConflictException` (409) nuestras |
| Negocio | `IOrdenBusiness`/`OrdenBusiness` (puntos 1 a 5) y un `*Business` por entidad externa |
| DTO | `model/dto`: clases de request/response que no son tablas |
| Controlador | `OrdenRestController extends BaseRestController` |

El recorrido de un request es siempre **controlador → negocio → repositorio → base**, y la respuesta
vuelve por el mismo camino. El controlador solo hace `try/catch` y traduce excepciones a códigos.

## Para responder rápido

- *¿Por qué una interfaz y una implementación?* Para que el resto del código dependa de la interfaz;
  si mañana cambia la implementación, no se toca nada más. Y la interfaz se lee rápido: dice qué hace
  el negocio sin cómo.
- *¿Quién instancia `OrdenBusiness`?* Spring, porque tiene `@Service` y el controlador lo pide con
  `@Autowired IOrdenBusiness`.

# 8. JPA, Hibernate y la base de datos

## Lo que vimos en clase

- **ORM** (Object Relational Mapping): mapea objetos a tablas. **JPA** es la especificación estándar
  de Java; **Hibernate** es la implementación que viene con Spring y escribe el SQL.
- Mínimo para que una clase sea tabla: `@Entity` y `@Id`. Además: `@Table(name=...)`,
  `@GeneratedValue(strategy = GenerationType.IDENTITY)` (autonumérico), `@Column(length, unique,
  nullable)`. `columnDefinition` (SQL a mano) es mala práctica porque ata al motor.
- **Clave principal:** siempre un entero largo autonumérico; la clave "del negocio" se vuelve clave
  secundaria (`unique`). Los autonuméricos no reutilizan huecos (el motor guarda el último valor y
  suma 1: eficiencia).
- **`long` vs `Long`:** un primitivo no puede ser `null`. Un JSON sin `id` fallaba al deserializar
  hasta que se cambió a `Long`.
- **Relaciones:** `@ManyToOne` + `@JoinColumn(name="id_...")` (clave foránea), `@OneToMany`,
  `@ManyToMany` + `@JoinTable`. Herencia con `@Inheritance(JOINED)`.
- **Fetch LAZY** (por defecto en colecciones, carga los hijos cuando se piden) vs **EAGER** (carga
  todo siempre; "peligroso", solo si los hijos se usan sí o sí). **Set** no admite repetidos, **List** sí.
- **Query methods:** Spring arma la consulta por el nombre del método
  (`findByProduct`, `findOneByCategory`, `findByExpirationDateBeforeOrderByExpirationDateDesc`).
- **`@Query`:** consultas propias (JPQL, o SQL nativo con `nativeQuery = true`); `@Modifying` y
  `@Transactional` para las que modifican. **Proyecciones:** traer solo algunas columnas.
- `spring.jpa.hibernate.ddl-auto`: `update` (desarrollo), `validate`/`none` (producción), `create`
  (borra todo). **Dialecto** MySQL para que Hibernate genere SQL óptimo.
- **Pool de conexiones (Hikari):** abre conexiones al arrancar y las reutiliza; mínimo 10, máximo 20 en
  la configuración de clase.
- **"Todo lo que sea calculable no hay que almacenarlo"** (salvo que el cálculo sea muy caro): si se
  guarda, hay que mantenerlo cada vez que cambia algo.
- `Optional<T>`: contenedor de algo que puede no existir; evita los `if (x == null)`.

## En nuestro TP

- Todas las entidades tienen `id` autonumérico `IDENTITY`. Las externas tienen además
  **`codigoExterno` único**: es la clave alternativa de integración que el profe mostró en clase
  ("crear un código externo de tipo unique es un formato bastante estándar de integración"). La orden
  tiene `numeroOrden` único.
- `Orden` → `Camion`, `Chofer`, `Cliente`, `Producto` con `@ManyToOne` (muchas órdenes pueden usar el
  mismo camión). `DetalleCarga` y `CambioEstado` → `Orden` con `@ManyToOne`.
- `Camion` → `Cisterna` con `@OneToMany(cascade = ALL, orphanRemoval = true, fetch = EAGER)`:
  - `cascade = ALL`: al guardar el camión se guardan sus cisternas.
  - `orphanRemoval`: si una cisterna sale de la lista, se borra de la base (así se reemplazan al
    sincronizar).
  - `EAGER`: las cisternas son pocas y siempre se muestran con el camión, es el caso en que el profe
    dice que EAGER está bien.
- **Lo calculable no se guarda:** el total de litros del camión (`getTotalLitros()`) y la
  **conciliación** (clase `Conciliacion` en `model/dto`, se calcula en cada pedido).
- Pesos y valores como `Double` y no `double`: pueden ser `null` mientras no hay dato (la misma
  lección del `long` en clase).
- `@Enumerated(EnumType.STRING)`: el estado se guarda como texto (`"CERRADA"`), legible en la base.
- Query methods propios: `findByNumeroOrden`, `findByCodigoExterno`,
  `findFirstByOrdenIdOrderByFechaDesc` (el último detalle, para la frecuencia).
- `@Query` en `DetalleCargaRepository`: `SELECT AVG(d.temperatura) FROM DetalleCarga d WHERE
  d.orden.id = ?1` (JPQL, por eso no lleva `nativeQuery`; usa los nombres de las clases, no de las
  tablas).
- Hikari configurado igual que en clase (`minimum-idle=10`, `maximum-pool-size=20`).

## Para responder rápido

- *¿Por qué no guardan la conciliación?* Porque se calcula con datos que ya están guardados y es
  barato; si la guardáramos, podría quedar desactualizada.
- *¿Qué pasa si SAP manda un camión que ya existe con otra patente?* `CamionBusiness.sincronizar()`
  lo encuentra por `codigoExterno`, actualiza los datos y reemplaza las cisternas.

# 9. Estados de la orden y auditoría

## Lo que pide la consigna

- Estados **secuenciales y unidireccionales**: 1 pendiente de pesaje inicial, 2 con pesaje inicial,
  3 cerrada, 4 finalizada.
- Cada cambio de estado registrado con fecha y hora, usuario o proceso y observaciones. Recomienda un
  log de auditoría.

## En nuestro TP

- Enum `EstadoOrden` con los cuatro valores.
- `controlarEstado(orden, esperado)`: si la orden no está en el estado que la acción necesita, lanza
  `ConflictException` → **409**.
- `cambiarEstado(orden, nuevo, observacion)`: pone el estado, guarda y crea un `CambioEstado` con
  estado anterior, nuevo, fecha, usuario y observación. **Es el único lugar donde cambia el estado.**
- El usuario sale del token: `usuarioActual()` lee `SecurityContextHolder.getContext()
  .getAuthentication().getName()` (lo que el filtro JWT dejó en el contexto; misma idea que
  `getUserLogged()` de clase). Si no hay usuario, registra `"sistema"`.
- Consulta: `GET /api/v1/ordenes/{numeroOrden}/estados`.

## Para responder rápido

- *¿Cómo evitan que una orden vuelva atrás?* Cada acción exige un estado puntual y pasa al siguiente,
  y no hay otro código que cambie el estado.

# 10. Manejo de errores

## Lo que vimos en clase

- Árbol de excepciones de Java: `Throwable` → `Exception` (controlables, casos excepcionales del
  flujo) y `Error` (fin del camino).
- Pocas excepciones propias, construidas con el patrón **Builder** de Lombok:
  `BusinessException.builder().ex(e).build()`, siempre guardando la excepción original y mandándola al
  log con `log.error(e.getMessage(), e)`.
- `StandartResponse` estandariza los errores para el frontend: `message`, `code` y `devInfo` (traza,
  solo si `dev.info.enabled=true`). Se fabrica con un **Factory** (`IStandartResponseBusiness.build`)
  que lee la propiedad con `@Value("${dev.info.enabled:...}")`.
- En el controlador: `try { ... } catch (NotFoundException e) { 404 } catch (BusinessException e)
  { 500 }` devolviendo `ResponseEntity<?>`.

## En nuestro TP

| Excepción | Código | Ejemplo |
|-------------------|------|-------------------------------------------------------|
| `NotFoundException` | 404 | La orden no existe |
| `FoundException` | 302 | Ya existe una orden con ese número |
| `BadRequestException` | 400 | Peso 0, contraseña incorrecta, dato de carga descartado |
| `ConflictException` | 409 | Cerrar una orden que está en estado 1 |
| `BusinessException` | 500 | La base no responde |

En la VM `dev.info.enabled=false`: el cliente ve el mensaje y el código, pero no la traza.

# 11. Serialización y DTOs

## Lo que vimos en clase

- **Serializar:** objeto Java → JSON (o al revés, deserializar). Lo hace **Jackson** automáticamente
  con todos los atributos, salvo `@JsonIgnore`.
- Serializadores propios (`StdSerializer`): ocultar o renombrar atributos, crear atributos calculados,
  tener varias versiones (por ejemplo `?slim=v1`), según el rol. Ventajas: menos ancho de banda,
  menos latencia, más seguridad.
- Deserializadores propios (`StdDeserializer`): aceptar JSON con distintos nombres de campos; muy
  útiles en **integraciones B2B**, que "suelen ser un dolor de cabeza". La categoría se buscaba por
  **nombre**, porque un sistema externo no conoce nuestros IDs.
- `@JsonIgnore` es a JSON lo que `@Transient` es a la base: no serializar / no guardar.
- `spring.jackson.time-zone=America/Argentina/Buenos_Aires` para las fechas.

## En nuestro TP

- No hicimos serializadores propios: alcanzó con **DTOs** (`model/dto`) y `@JsonIgnore`. Es más simple
  de explicar y suficiente para el caso.
- `password` de la orden con `@JsonIgnore`: nunca aparece al consultar; solo se devuelve una vez en
  `PasswordResponse` del pesaje inicial.
- `orden` en `DetalleCarga` con `@JsonIgnore`: al listar detalles no se repite la orden en cada uno
  (y se evita un ciclo orden ↔ detalle).
- Las entidades externas se relacionan por `codigoExterno` (texto), no por nuestro `id`: la misma idea
  que el deserializador que buscaba la categoría por nombre.
- El punto 1 recibe la entidad `Orden` directamente como `@RequestBody`; el negocio limpia los campos
  internos (tara, password, fechas) por si vinieron.

# 12. Configuración externa y secretos

## Lo que vimos en clase

- `@Value("${propiedad:valorPorDefecto}")` inyecta valores de configuración; siempre poner un valor
  por defecto.
- Los valores críticos (contraseñas, tokens) no se escriben en el código: van en **variables de
  entorno** (`${SMTP_PASS:}`), en el `.env` del compose o en `environment:`.
- Los parámetros que pueden cambiar (destinatarios de mail, frecuencias de un scheduler) también van en
  configuración, no hardcodeados.

## En nuestro TP

| Propiedad | Para qué |
|-----------------------------------|------------------------------------------------------------|
| `carga.detalle.frecuencia.segundos=10` | Cada cuánto se guarda un detalle (`@Value` en `OrdenBusiness`) |
| `usuarios.password.inicial=${USUARIOS_PASSWORD:123456}` | Contraseña de los cinco usuarios; en la VM sale de `.env` |
| `spring.mail.username=${SMTP_USER:}` / `password=${SMTP_PASS:}` | Mail para la alarma del final |
| `dev.info.enabled` | Traza en los errores (true en dev, false en prod) |

**Punto a saber:** en el video de la consigna el profe dijo que la frecuencia "se va a tener que poder
definir en nuestra base de datos". Nosotros la tenemos en `application.properties`. Respuesta: es
configurable sin tocar código, pero cambiarla requiere reiniciar; pasarla a una tabla de configuración
(o por orden) es una mejora pensada para el final.

# 13. Seguridad: autenticación JWT y autorización por roles

Este es el tema que el profe dijo que más pregunta.

## 13.1. Autenticación (quién sos)

**Lo que vimos en clase**

- **JWT (JSON Web Token):** un JSON con **claims** (pares clave/valor: usuario, roles, email,
  versión), codificado en Base64 en tres partes separadas por puntos (encabezado, datos, firma) y
  **firmado** con una clave secreta que solo tiene el servidor.
- Los datos se pueden leer (jwt.io), pero no se pueden modificar: si alguien cambia algo, la firma no
  coincide. "Es como darle a alguien algo en una caja que no puede abrir; cuando me la devuelve, yo
  sí la abro."
- Gracias a eso el servidor **no consulta la base en cada request**: confía en lo que dice el token
  mientras no expire. Y cumple con **stateless**: el estado lo tiene el cliente, pero no lo puede
  alterar.
- Todo token tiene **fecha de expiración** (en el proyecto, 1 hora: `AuthConstants.EXPIRATION_TIME`).
- El token viaja en la cabecera `Authorization: Bearer <token>` (o por el parámetro `authtoken`).
  `Basic` es usuario:contraseña en Base64, inseguro.
- **Filtros:** antes de llegar al endpoint, el request pasa por una cadena de filtros; cada uno puede
  cortar y responder (por ejemplo, token vencido) o pasar al siguiente con `chain.doFilter`.

**Las piezas (todas del proyecto base, en `auth/`)**

| Pieza | Cuándo actúa | Qué hace |
|----------------------------|------------------|------------------------------------------------------------|
| `AuthRestController` (`POST /api/v1/login`) | En el login | Recibe `username` y `password`, llama al `AuthenticationManager` y, si está bien, arma el token con `JWT.create().withSubject(...).withClaim("roles", ...).withClaim("email", ...).withExpiresAt(...).sign(...)` |
| `CustomAuthenticationManager` | Una vez, en el login | Carga el usuario (por nombre o email), llama a `validate()` (cuenta expirada, bloqueada, deshabilitada), compara la contraseña con `passwordEncoder.matches()` y devuelve un `UsernamePasswordAuthenticationToken` con el usuario como **principal** y sus roles |
| `JWTAuthorizationFilter` | En **cada** request | Saca el token del header, lo verifica con la clave, arma el `Authentication` con usuario y roles y lo guarda con `SecurityContextHolder.getContext().setAuthentication(...)` |
| `SecurityConfiguration` | Al arrancar | Define el `PasswordEncoder` (**BCrypt**), el `AuthenticationManager`, qué rutas son públicas (`permitAll`), `anyRequest().authenticated()`, `STATELESS` y agrega el filtro |
| `User implements UserDetails` | - | El usuario que entiende Spring: `getAuthorities()` (los roles), `isEnabled()`, etc. `@Transient` en lo calculado |

- Las contraseñas se guardan como **hash BCrypt** (no reversible, con sal aleatoria): nunca en texto
  plano. Para comparar se usa `matches(textoPlano, hash)`.
- El login tiene que ser público: "si no, dejaste la llave adentro y cerraste la puerta".

**En nuestro TP**

- Mantuvimos todo el sistema de login del proyecto base.
- `DatosIniciales` (un `CommandLineRunner`) crea los roles y los usuarios `admin`, `sap`, `tms`,
  `carga`, `operador` al arrancar, y actualiza la contraseña si no coincide con
  `usuarios.password.inicial`.
- Ajustes nuestros:
  - `/error` con `permitAll`: si no, un 403 se mostraba como 401 (Spring redirigía a `/error` y esa
    ruta pedía autenticación).
  - Login sin `produces = TEXT_PLAIN`: un login incorrecto respondía 500 porque el error en JSON no se
    podía convertir a texto; ahora responde 401.
- Swagger (`/swagger-ui.html`, `/v3/api-docs/**`) es público, como en clase.

## 13.2. Autorización (qué podés hacer)

**Lo que vimos en clase**

- Sin autorización, cualquier usuario autenticado puede usar todo.
- `@EnableMethodSecurity(prePostEnabled = true)` habilita:
  - **`@PreAuthorize`** (antes de ejecutar): `hasRole('ROLE_ADMIN')`, `hasRole(...) or hasRole(...)`,
    comparaciones con parámetros (`#username == authentication.principal.username`).
  - **`@PostAuthorize`** (devuelve solo si se cumple sobre `returnObject`) y **`@PostFilter`** (filtra
    la lista devuelta usando `filterObject`).
  - `request.isUserInRole(...)` dentro del método.
- Si se cambian los roles en la base, el token viejo los sigue teniendo hasta que se vuelva a loguear.

**En nuestro TP**

Un usuario y un rol por sistema externo, con `@PreAuthorize("hasAnyRole('ADMIN','XXX')")` en cada
endpoint de `OrdenRestController` (Spring agrega el prefijo `ROLE_`):

| Rol | Puede |
|-------------|------------------------------------------------------------|
| `SAP` | Punto 1 (crear orden) |
| `TMS` | Puntos 2 y 5 (pesajes) |
| `CARGA` | Punto 3 (activar, detalle) y punto 4 (cerrar) |
| `OPERADOR` | Consultas (será el usuario del frontend) |
| `ADMIN` | Todo |

La conciliación la puede pedir cualquier usuario logueado.

## Para responder rápido

- *¿Qué pasa si `sap` registra un pesaje?* El `@PreAuthorize` no se cumple y Spring responde 403 sin
  ejecutar el método.
- *¿Por qué el backend no consulta la base en cada request para saber los roles?* Porque los roles
  están dentro del token, firmado: si el token es válido, los datos son confiables.
- *¿Qué hace el filtro si no viene token?* Pasa al siguiente filtro; al final, como la ruta pide
  autenticación, Spring responde 401.
- *¿Dónde se guarda el usuario autenticado durante el request?* En el `SecurityContextHolder`; de ahí
  lo leemos para la auditoría.

# 14. Logs

## Lo que vimos en clase

- No usar `System.out.println`: usar un logger (`@Slf4j` → variable `log`). "No escatimar el log,
  después se maneja con una perillita."
- Niveles: ERROR > WARN > INFO > DEBUG > TRACE; al elegir uno se ven ese y los superiores. Se configura
  por paquete o clase (`logging.level.ar.edu.iua.iw3=DEBUG`).
- En producción solo WARN y ERROR (el debug puede mostrar información útil para un atacante).

## En nuestro TP

- `logging.level.root=WARN`, nuestro paquete en `DEBUG`, el filtro JWT en `INFO`.
- Todos los `catch` del negocio hacen `log.error(e.getMessage(), e)` antes de lanzar la excepción
  propia. En la VM se ven con `docker logs --tail 100 -f backend`.

# 15. Documentación OpenAPI (Swagger)

## Lo que vimos en clase

- `springdoc-openapi`: documentación automática a partir del código, en `/swagger-ui.html`.
- `OpenApiConfig` con `@SecurityScheme` (Bearer JWT) y un `@Bean OpenAPI` con título, descripción y
  versión.
- En el controlador: `@Tag` (agrupa), `@Operation` (resumen), `@Parameter`, `@ApiResponses` con un
  `@ApiResponse` por código.
- Se puede probar todo: login, copiar el token, botón **Authorize**.

## En nuestro TP

- Todos los endpoints de `OrdenRestController` tienen `@Operation` y `@ApiResponses`, agrupados con
  `@Tag(name = "Órdenes")`. Está en `/swagger-ui.html` local y en la VM.

# 16. Deploy: Tomcat, nginx, HTTPS y el script

## Lo que vimos en clase

- **Tomcat:** `bin` (startup/shutdown), `conf` (`server.xml`, `catalina.properties`), `logs`
  (`catalina.out`), `webapps` (cada carpeta es una aplicación; `ROOT` atiende `/`).
- **Proxy inverso (nginx):** un solo puerto hacia afuera (443) y reglas que rutean `/` al backend y
  `/adminer` al adminer. Se encarga del certificado: así no hay que configurar SSL en cada aplicación.
- **Certificado con Let's Encrypt (certbot):** `certbot certonly --standalone -d <dominio>`. Desafío:
  certbot abre el puerto 80 y publica un token; Let's Encrypt se conecta al dominio, lo lee y, si
  coincide, confirma que controlás el dominio. Se usan `fullchain.pem` (certificado + cadena) y
  `privkey.pem` (clave privada, no compartirla). Dura 3 meses.
- **Script de deploy:** clonar el repo en `/tmp`, compilar el war con Maven en Docker (con `~/.m2`
  montado para no bajar las dependencias cada vez), parar el backend, reemplazar `webapps/ROOT`,
  descomprimir, levantar el backend y mirar el log. Rutas absolutas para que sea repetible.
- "En la informática no hay magia": comandos simples paso a paso, que una vez que funcionan, funcionan
  siempre. El flujo real: desarrollar → probar → subir al repo → del repo a producción con un script.

## En nuestro TP

- `infra/deploy.sh` (adaptado del profesor): verifica el perfil `mysqlprod`, clona `TiziTur/ingweb3`,
  compila con Maven en Docker, para el backend, reemplaza `ROOT` y lo vuelve a levantar.
- Cambios respecto al original: `cp` en vez de `mv` (Maven corre como root en el contenedor y
  `target` queda de root) y `sudo -v` al principio; se corre **en primer plano** porque pide la
  contraseña de sudo.
- Dominio: `https://iw3turina.duckdns.org` (DuckDNS asigna el dominio a la IP de la VM).
- La colección de Postman se corre contra la VM cambiando de entorno (`IW3-VM`).

# 17. Lo que viene para el final

Ya está preparado en el proyecto lo que el profe mostró en las últimas clases:

| Tema de clase | Para qué lo vamos a usar | Ya está en el repo |
|------------------------------------------|----------------------------------------------|---------------------------|
| **Mail** (`spring-boot-starter-mail`, `EmailBusiness.sendSimpleMessage`, SMTP con usuario y contraseña por variables de entorno) | Mandar la alarma de temperatura | `util/EmailBusiness`, `MailRestController`, propiedades `spring.mail.*` |
| **Eventos** (patrón Observer: un `ApplicationEvent`, un `ApplicationListener` y `publishEvent`; asíncronos para no frenar el flujo principal) | Al recibir un dato con temperatura alta, publicar un evento y que un listener cree la alarma y mande el mail | `events/AsyncSpringEventsConfig` |
| **Scheduling** (`@EnableScheduling`, `@Scheduled(fixedDelay)` o `cron`, `@Async`) | Tareas periódicas si hicieran falta | Se quitó el ejemplo; se agrega igual que en clase |
| **WebSocket** | Tiempo real en el frontend | `websocket/WebSocketConfig` |

Requisitos del final según la consigna: alarma de temperatura con umbral y destinatarios
configurables que no se reenvía hasta que un usuario la acepte, y un frontend con login, monitoreo en
tiempo real, filtros, ETA, tiempo transcurrido, gestión de alarmas y conciliación.

# 18. El circuito completo, punto por punto

Resumen para seguir la presentación con Postman. Detalle y JSON de ejemplo en `DOCUMENTACION.md`,
sección 6.

| Punto | Endpoint (bajo `/api/v1/ordenes`) y rol | Controles | Resultado |
|--------|----------------------------------|--------------------------|------------------------------|
| 1 | `POST /` (SAP) | `numeroOrden` y `preset > 0` (400); número repetido (302) | Sincroniza camión, chofer, cliente y producto por `codigoExterno`; estado 1; **201** + `Location` |
| 2 | `POST /{n}/pesaje-inicial` (TMS), body `{peso}` | Peso > 0 (400); estado 1 (409) | Guarda la tara, genera la password de 5 dígitos, estado 2; devuelve `{numeroOrden, password}` |
| 3a | `POST /{n}/activar` (CARGA), body `{password}` | Estado 2 (409); password correcta (400) | Devuelve `{numeroOrden, preset}`; no cambia el estado |
| 3b | `POST /{n}/detalle` (CARGA), body `{masaAcumulada, densidad, temperatura, caudal}` | Estado 2 (409); descarta caudal ≤ 0, masa ≤ 0 o menor a la anterior (400) | Actualiza **siempre** los últimos valores de la orden; guarda un detalle solo si pasaron 10 s desde el último |
| 4 | `POST /{n}/cerrar` (CARGA) | Estado 2 (409) | Estado 3; desde ahora el detalle responde 409 |
| 5 | `POST /{n}/pesaje-final` (TMS), body `{peso}` | Peso > 0 (400); estado 3 (409) | Guarda el pesaje final, estado 4, devuelve la conciliación |
| 5 | `GET /{n}/conciliacion` (cualquier usuario logueado) | Estado 4 (409) | La conciliación |

**Conciliación:**

| Campo | Cálculo |
|----------------------------|------------------------------------------------------------|
| `pesajeInicial` | La tara |
| `pesajeFinal` | El pesaje final |
| `productoCargado` | La última masa acumulada (caudalímetro **másico**: mide kg directamente) |
| `netoPorBalanza` | Pesaje final − tara |
| `diferenciaBalanzaCaudalimetro` | Neto por balanza − producto cargado (qué se hace con la diferencia lo deciden las personas) |
| Promedios | `AVG` de temperatura, densidad y caudal sobre los detalles guardados |

**Contraseña de activación:** `10000 + (int) (Math.random() * 90000)` → entre 10000 y 99999. La genera
el backend (en el video de la consigna el profe lo confirmó: "nuestro backend genera una password de
activación").

**Frecuencia:** si llegan datos cada 1 segundo y se guarda cada 10, en un minuto llegan 60 y se
guardan 6 (ejemplo de la consigna). La cabecera de la orden siempre tiene el último valor, "para que
esté muy accesible".

# 19. Decisiones y puntos que el profe podría señalar

Las decisiones están justificadas en `DOCUMENTACION.md`, sección 13. Estos son los puntos donde el
profe podría preguntar porque se apartan de lo que dijo en clase, con la respuesta preparada:

| Posible pregunta | Respuesta |
|------------------------------------------|------------------------------------------------------------|
| "La frecuencia tenía que estar en la base de datos" | Está en `application.properties`: configurable sin tocar código, pero requiere reiniciar. Pasarla a una tabla es una mejora para el final. |
| "`activar` y `cerrar` son verbos en la URL" | Son transiciones de estado que pide la consigna, no CRUD; la definición de endpoints es a criterio del grupo. Alternativa: `/cierre` o `PUT /estado` (ver 4.3). |
| "En producción `ddl-auto` debería ser `validate`" | Lo dejamos en `update` para que la VM cree las tablas sola; en un sistema real se crearían con un script y se usaría `validate`. |
| "La clave del JWT está en el código" | Es la del proyecto base (`AuthConstants.SECRET`). Lo correcto es sacarla a una variable de entorno, como hicimos con las contraseñas de usuarios y de mail. |
| "¿Por qué no hicieron serializadores propios?" | No hicieron falta: con DTOs y `@JsonIgnore` las respuestas tienen exactamente lo necesario, y es más simple de mantener. |
| "¿Por qué 302 si la orden ya existe?" | Es el mismo criterio del proyecto base para `FoundException` ("ya existe"). |
| "¿Por qué los datos descartados devuelven 400 y no 200?" | Para que el sistema de carga se entere de que ese dato no se usó y por qué (el mensaje dice el motivo). |
| "¿Por qué los roles ya en el parcial si eran del final?" | Son pocas líneas y evitan que un sistema use los endpoints de otro. |

# 20. Preguntas de práctica

Respuestas cortas, como las quiere el profe.

## Teoría

1. **¿Qué significa que HTTP es sin estado?** Que cada request-response es independiente; el servidor
   no recuerda el request anterior. La "sesión" se arma mandando el token en cada request.
2. **¿Qué es REST?** Un conjunto de restricciones para aplicaciones en red: cliente-servidor, sin
   estado, caché, contrato uniforme y sistema en capas.
3. **¿Qué es un recurso y una representación?** Recurso: cualquier fuente de información identificada
   por una URI (una orden). Representación: su estado en un formato (JSON).
4. **¿Diferencia entre imagen y contenedor?** La imagen es la plantilla (diseño); el contenedor es la
   imagen corriendo (ejecución). Como clase y objeto.
5. **¿Diferencia entre `ports` y `expose`?** `ports` abre el puerto en la máquina real (internet);
   `expose` solo lo comparte con los otros servicios del compose.
6. **¿Qué es un proxy inverso?** Un servidor HTTP que recibe todo por un solo puerto y rutea a los
   servicios de atrás según la URL; además maneja el certificado SSL.
7. **¿Cómo verifica Let's Encrypt que el dominio es tuyo?** Certbot publica un token en el puerto 80;
   Let's Encrypt entra al dominio, lo lee y si coincide emite el certificado.
8. **¿Diferencia entre jar y war?** El jar trae el servidor adentro y se corre con `java -jar`; el war no
   lo trae y se deposita en un Tomcat.
9. **¿Qué es un perfil?** Un conjunto de configuraciones que se activa con `spring.profiles.active`;
   `application-<perfil>.properties` sobrescribe al principal, como una herencia.
10. **¿Qué es la inversión de control?** Que no instanciamos nosotros: Spring crea los objetos y los
    inyecta donde ponemos `@Autowired`.
11. **¿Qué es JPA y qué es Hibernate?** JPA es la especificación de persistencia de Java; Hibernate es
    la implementación que traduce las entidades a tablas y escribe el SQL.
12. **¿Qué es un query method?** Un método del repositorio cuyo nombre (`findByNumeroOrden`) le dice a
    Spring qué consulta armar, sin escribir SQL.
13. **¿LAZY o EAGER?** LAZY carga los hijos cuando se piden (por defecto y lo más eficiente); EAGER los
    carga siempre, solo conviene si se usan siempre.
14. **¿Por qué la clave principal es un autonumérico?** Es la más eficiente para el índice; la clave del
    negocio (número de orden, código externo) queda como `unique`.
15. **¿Qué es un JWT y qué tiene adentro?** Un token firmado con tres partes en Base64 (encabezado,
    claims, firma). Adentro: usuario (subject), roles, email, versión y fecha de expiración.
16. **¿Por qué no se puede falsificar un JWT si se puede leer?** Porque está firmado con una clave que
    solo tiene el servidor; si se cambia algo, la firma no coincide.
17. **¿Qué es un filtro?** Un componente por el que pasa el request antes del endpoint; puede cortar y
    responder o pasar al siguiente.
18. **¿Por qué las contraseñas se guardan con BCrypt?** Es un hash no reversible y con sal: aunque
    roben la base, no se obtienen las contraseñas.
19. **¿Qué es `@PreAuthorize`?** Una anotación que evalúa una condición (por ejemplo el rol) antes de
    ejecutar el método; si no se cumple, no se ejecuta.
20. **¿Qué es serializar?** Convertir un objeto Java a JSON para mandarlo por la red (y deserializar, lo
    contrario).
21. **¿Por qué no guardar lo que se puede calcular?** Porque habría que mantenerlo actualizado cada vez
    que cambian los datos de los que depende.
22. **¿Para qué sirve un pool de conexiones?** Para no abrir y cerrar una conexión a la base en cada
    consulta: se abren al arrancar y se reutilizan.
23. **¿Qué es B2B?** La comunicación automática entre sistemas, sin un humano en el medio.

## Sobre el TP

24. **¿Qué pasa si llega un dato para una orden cerrada?** `controlarEstado` exige estado 2; la orden
    está en 3, lanza `ConflictException` y responde 409.
25. **¿Qué datos de carga se descartan?** Caudal ≤ 0, masa ≤ 0 o masa menor a la anterior (la masa es
    acumulativa). Responden 400.
26. **Si llegan datos cada segundo, ¿cuántos se guardan?** La cabecera se actualiza con todos; en
    `detalles_carga` se guarda uno cada 10 segundos (`correspondeGuardarDetalle`).
27. **¿Cómo se calculan los promedios?** Con `@Query` JPQL `AVG(...)` en `DetalleCargaRepository`,
    sobre los detalles guardados.
28. **¿Quién genera la contraseña y cómo?** El backend, en el pesaje inicial:
    `10000 + (int)(Math.random() * 90000)`.
29. **¿Por qué la contraseña no aparece al consultar la orden?** Por `@JsonIgnore`; se entrega una
    sola vez.
30. **¿Cómo se registra quién cambió el estado?** `cambiarEstado` crea un `CambioEstado` con el
    usuario del token (`SecurityContextHolder`).
31. **¿Para qué sirve el `codigoExterno`?** Para identificar las entidades con el código del sistema
    externo (SAP) y sincronizarlas; es texto para no depender de su formato.
32. **¿Qué significa sincronizar?** Si la entidad no existe se crea; si existe, se actualiza con los
    datos nuevos.
33. **¿Por qué `Double` y no `double`?** Porque `Double` puede ser `null` y así se distingue "todavía no
    hay dato" de "el dato es 0".
34. **¿Dónde se ve qué rutas son públicas?** En `SecurityConfiguration`: login, `/error`, Swagger y
    `/demo/**`; el resto pide autenticación.
35. **¿Cómo cambian la contraseña de los usuarios en la VM?** Con la variable `USUARIOS_PASSWORD` en
    `~/infra/.env`; al reiniciar, `DatosIniciales` la actualiza.
36. **¿Cómo se despliega una versión nueva?** Commit y push a `main`, y en la VM `~/infra/deploy.sh`.
37. **¿Cómo sabe el backend de la VM a qué base conectarse?** Por el perfil `mysqlprod`, que define
    `catalina.properties`; su URL apunta al host `mysql` (el servicio del compose).
38. **¿Qué devuelve la conciliación si la orden está en estado 3?** 409: solo está disponible en
    estado 4.
39. **¿Qué diferencia hay entre el punto 3a y 3b?** 3a (`activar`) valida la contraseña y devuelve el
    preset; 3b (`detalle`) recibe los datos en tiempo real.
40. **¿Por qué las cisternas usan `cascade` y `orphanRemoval`?** Para que se guarden con el camión y se
    borren las que ya no vienen al sincronizar.

# 21. Guion sugerido para la presentación (15 a 20 minutos)

| Minutos | Quién | Qué mostrar |
|--------|-------------|------------------------------------------------------------|
| 0-3 | Integrante 1 | El caso (SAP, TMS, carga), los cuatro estados y la arquitectura (capas y la VM con nginx y Docker) |
| 3-8 | Integrante 2 | Login con `sap`, punto 1 (201 y `Location`), punto 2 (password), punto 3 activación y detalle (incluido un dato descartado) |
| 8-12 | Integrante 3 | Punto 4 (cierre y dato rechazado con 409), punto 5 (conciliación), consulta de estados (auditoría) |
| 12-15 | Integrante 4 | Errores esperados (401, 403, 404, 409), Swagger y la corrida completa del Runner en la VM |
| 15-20 | Todos | Preguntas del profesor |

Antes del martes:

- Correr la colección completa en local y en la VM (con la contraseña cargada en el entorno `IW3-VM`).
- Tener el proyecto abierto en el IDE en cada computadora, con la app corriendo.
- Que cada uno practique encontrar en menos de 30 segundos todo lo de la sección 2.
