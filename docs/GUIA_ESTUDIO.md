---
title: "Guía de estudio - Trabajo Integrador IW3"
subtitle: "Todo lo necesario para la defensa: el sistema, Spring, cada archivo del proyecto, la teoría de la materia y preguntas"
date: "Defensa: martes 10/11/2026"
lang: es
toc-title: "Índice"
---

# 1. Cómo usar esta guía

Esta guía está pensada para que sea lo único que tengas que leer. Tiene:

1. **El sistema en cinco minutos** (sección 2): qué hace, los estados y el circuito. Es lo que se
   muestra en la presentación.
2. **Spring desde cero** (sección 3): todo lo de Spring que usa el proyecto, explicado con nuestro
   código.
3. **Recorrido archivo por archivo** (sección 4): qué hace cada archivo, sus líneas importantes y por
   qué está hecho así. Es lo que sirve para el "buscame tal parte y explicámela".
4. **Un request de punta a punta** (sección 5): el camino completo de una petición, desde Postman
   hasta la base y de vuelta.
5. **La teoría de la materia** (sección 6): HTTP, REST, Docker, infraestructura, SSL y deploy,
   cada tema con su aplicación en el TP.
6. **Decisiones y puntos débiles** (sección 7) y **preguntas de práctica** (sección 8).
7. **Guion de la presentación** (sección 9).

Las rutas de archivos que aparecen sin carpeta están bajo
`iw3/src/main/java/ar/edu/iua/iw3/`.

## 1.1. Cómo es la evaluación (lo que dijo el profesor en las clases)

| Lo que dijo | Qué hacer |
|-----------------------------------------------|------------------------------------------------------------|
| "Tenemos un grupo de cuatro, no hay problema con eso" | El grupo está aprobado. |
| Presentación de 15 a 20 minutos por grupo, mostrando el sistema **con Postman**. "Tiene que estar todo listo y funcionando" | Correr la colección antes (local y VM). |
| Después hace "una o dos preguntas puntuales a cada una de las personas", sin importar quién hizo qué | Todos tienen que saber todo. |
| No repregunta; quiere respuestas concisas. "La persona que no me contesta, no aprueba" | Responder en 2 o 3 oraciones, sin vueltas. |
| "Cada uno tiene que tener su código en su computadora; le voy a decir **buscame tal parte y explicámela**. Si tardaste más de 30 segundos en encontrarlo y explicármelo, se acabó" | Tener el proyecto abierto en el IDE y practicar con la sección 4. Atajos de Eclipse: **Ctrl+Shift+T** abre una clase por nombre; **Ctrl+O** salta a un método de la clase abierta; **Ctrl+Shift+R** abre cualquier archivo (por ejemplo `application.properties`). |
| "La autenticación me interesa mucho que la sepan; suelo hacer bastantes preguntas de esto" | Estudiar muy bien 3.8, 4.7 y las preguntas de seguridad. |
| Swagger y una colección de Postman con tests del circuito completo son "un valor agregado muy grande" | Lo tenemos: mostrarlo. |
| La definición de los endpoints "queda a criterio de los grupos" | Poder justificar nuestras rutas (sección 7). |

## 1.2. Plan de estudio

| Día | Qué leer | Práctica |
|-----------|-----------------------------|------------------------------------------------------------|
| Viernes | Secciones 2 y 3 | Abrir en Eclipse cada clase que se nombra. |
| Sábado | Sección 4 completa | Seguir el circuito con Postman y mirar en la base qué tablas cambian. |
| Domingo | Secciones 5, 6 y 7 | Probar un 401, un 403 y un 409. Leer `deploy.sh`. |
| Lunes | Secciones 8 y 9 | Simulacro: uno dice "buscame X" y el otro tiene 30 segundos. Ensayar la presentación. |

# 2. El sistema en cinco minutos

## 2.1. Qué es

Un **backend** (sin interfaz gráfica) que administra **órdenes de carga de gas líquido** en una
planta. No lo usa una persona directamente: lo usan otros sistemas por HTTP con JSON (comunicación
**B2B**, *business to business*):

| Sistema externo | Qué hace | Usuario en nuestro backend |
|-----------------|----------------------------------------------|-------------------|
| **SAP** | Sistema administrativo: crea la orden con camión, chofer, cliente, producto y cantidad a cargar | `sap` |
| **TMS** (*Terminal Manager System*) | La balanza: pesa el camión vacío (tara) y lleno | `tms` |
| **Sistema de carga** | El equipo que carga el gas: usa una contraseña para arrancar y manda datos en tiempo real desde el **caudalímetro másico** (mide kg directamente) | `carga` |
| **Operador** | Consulta órdenes (será el usuario del frontend en el final) | `operador` |

## 2.2. Los cuatro estados

Los estados son **secuenciales y unidireccionales** (solo avanzan):

| Nº | Estado (`EstadoOrden`) | Cuándo | Qué se puede hacer |
|------|------------------------------|-------------------------------|---------------------------|
| 1 | `PENDIENTE_PESAJE_INICIAL` | SAP mandó la orden | Registrar la tara |
| 2 | `CON_PESAJE_INICIAL` | Se registró la tara y se generó la contraseña | Activar, mandar datos de carga, cerrar |
| 3 | `CERRADA` | Terminó la carga | Registrar el pesaje final |
| 4 | `FINALIZADA` | Se registró el pesaje final | Pedir la conciliación |

Cada cambio de estado queda registrado (auditoría) con fecha, usuario y observación.

## 2.3. El circuito

Todas las rutas empiezan con `/api/v1/ordenes`; la orden se identifica por su `numeroOrden`.

| Punto | Endpoint y rol | Controles | Qué hace |
|---------|------------------------------|---------------------------|------------------------------|
| 1 | `POST /` (SAP) | Falta número o preset ≤ 0 → 400; número repetido → 302 | Crea o actualiza camión, chofer, cliente y producto por `codigoExterno`; orden en estado 1. Responde **201** con `Location` |
| 2 | `POST /{n}/pesaje-inicial` (TMS) `{"peso": 15000}` | Peso ≤ 0 → 400; no está en 1 → 409 | Guarda la tara, genera una contraseña de 5 dígitos, pasa a 2. Devuelve `{numeroOrden, password}` |
| 3a | `POST /{n}/activar` (CARGA) `{"password": 84921}` | No está en 2 → 409; contraseña mal → 400 | Devuelve `{numeroOrden, preset}` para arrancar la bomba. No cambia el estado |
| 3b | `POST /{n}/detalle` (CARGA) `{masaAcumulada, densidad, temperatura, caudal}` | No está en 2 → 409; caudal ≤ 0, masa ≤ 0 o masa menor a la anterior → 400 (dato descartado) | Actualiza **siempre** los últimos valores de la orden; guarda un registro de detalle solo cada 10 segundos |
| 4 | `POST /{n}/cerrar` (CARGA) | No está en 2 → 409 | Pasa a 3. Desde ahí, mandar datos responde 409 |
| 5 | `POST /{n}/pesaje-final` (TMS) `{"peso": 18100}` | Peso ≤ 0 → 400; no está en 3 → 409 | Guarda el pesaje final, pasa a 4 y devuelve la conciliación |
| 5 | `GET /{n}/conciliacion` (cualquiera logueado) | No está en 4 → 409 | Devuelve la conciliación |

Consultas (OPERADOR): `GET /`, `GET /{n}`, `GET /{n}/detalles`, `GET /{n}/estados`. El rol `ADMIN`
puede todo.

**La conciliación** compara lo que midió la balanza con lo que midió el caudalímetro:

| Campo | Cálculo | Ejemplo |
|-------|---------|---------|
| `pesajeInicial` | La tara | 15.000 |
| `pesajeFinal` | El pesaje final | 18.100 |
| `productoCargado` | La última masa acumulada recibida | 3.000 |
| `netoPorBalanza` | Final − inicial | 3.100 |
| `diferenciaBalanzaCaudalimetro` | Neto − producto cargado (qué hacer con la diferencia lo decide una persona) | 100 |
| Promedios | `AVG` de temperatura, densidad y caudal de los detalles guardados | |

# 3. Spring desde cero

## 3.1. Qué es Spring y qué es Spring Boot

- **Spring** es un *framework* de Java (tiene unos 25 años y es el más usado para backends). Está
  dividido en módulos chicos: web, datos, seguridad, mail, etc.
- **Spring Boot** es la forma rápida de armar un proyecto Spring: trae configuraciones por defecto,
  un **Tomcat embebido** (el servidor web) y dependencias "curadas" que funcionan bien entre sí. El
  proyecto se genera en **start.spring.io**.
- Lo más importante de Spring es la **inversión de control (IoC)**: nosotros no hacemos `new` de
  nuestras clases de negocio, repositorios o controladores. Spring los crea al arrancar, los guarda
  en su **contenedor** y los entrega donde los pedimos. A esos objetos se los llama **beans**.
- **Inyección de dependencias:** cuando una clase necesita otra, la pide con `@Autowired` y Spring
  se la "inyecta". Ejemplo, en `OrdenRestController`:

  ```java
  @Autowired
  private IOrdenBusiness ordenBusiness;
  ```

  Pedimos la **interfaz**; Spring busca quién la implementa (la clase con `@Service`, o sea
  `OrdenBusiness`) y pone esa instancia. Por defecto hay **una sola instancia** de cada bean
  (*singleton*), compartida por todos los requests.

## 3.2. Cómo arranca la aplicación

1. `Iw3Application.main()` llama a `SpringApplication.run(...)`.
2. `@SpringBootApplication` le dice a Spring que **escanee** el paquete `ar.edu.iua.iw3` y todos sus
   subpaquetes buscando clases anotadas (`@Service`, `@RestController`, `@Configuration`, etc.) y que
   aplique la autoconfiguración.
3. Spring lee `application.properties` (y el del perfil activo), se conecta a MySQL, Hibernate crea o
   actualiza las tablas, se crean todos los beans y se levanta el servidor en el puerto 8080.
4. Cuando todo terminó de arrancar, se ejecutan las clases que implementan **`CommandLineRunner`**:
   `Iw3Application.run()` (configura la zona horaria y muestra el perfil en el log) y
   `DatosIniciales.run()` (crea roles y usuarios).

En la VM no se usa `main`: el `.war` lo arranca el Tomcat. Por eso `Iw3Application` **extiende
`SpringBootServletInitializer`**, que es lo que permite que la misma aplicación funcione como `.jar`
(con su propio Tomcat) y como `.war` (dentro de un Tomcat externo).

## 3.3. Las anotaciones que usamos

Una anotación (`@Algo`) es una forma **declarativa** de pedir comportamiento: no programamos cómo,
decimos qué queremos y el framework lo hace.

**Candidatos de instanciación (beans)** — le dicen a Spring "creá un objeto de esta clase":

| Anotación | Dónde | Qué significa |
|-----------|-------|---------------|
| `@SpringBootApplication` | `Iw3Application` | Clase principal: escaneo de componentes y autoconfiguración |
| `@RestController` | `OrdenRestController`, `AuthRestController`, `MailRestController` | Controlador web: sus métodos atienden requests y lo que devuelven se convierte a JSON |
| `@Service` | `OrdenBusiness`, `CamionBusiness`, ..., `StandartResponseBusiness` | Implementación de un servicio de negocio |
| `@Repository` | Interfaces de `model/persistence` | Acceso a datos (Spring genera la implementación) |
| `@Component` | `DatosIniciales`, `UserEventListener` | Bean genérico |
| `@Configuration` | `SecurityConfiguration`, `OpenApiConfig`, `AsyncSpringEventsConfig` | Clase de configuración: se procesa primero al arrancar |
| `@Bean` | Métodos de `SecurityConfiguration` | El objeto que devuelve el método pasa a ser un bean (por ejemplo el `PasswordEncoder`) |

**Inyección y configuración:**

| Anotación | Ejemplo | Qué hace |
|-----------|---------|----------|
| `@Autowired` | `@Autowired private OrdenRepository ordenDAO;` | Spring inyecta el bean |
| `@Value` | `@Value("${carga.detalle.frecuencia.segundos:10}")` | Inyecta un valor de configuración; lo que va después de `:` es el valor por defecto |

**Web (Spring MVC):**

| Anotación | Ejemplo | Qué hace |
|-----------|---------|----------|
| `@RequestMapping` | `@RequestMapping(Constants.URL_ORDENES)` en la clase | Prefijo de todas las rutas del controlador (`/api/v1/ordenes`) |
| `@GetMapping`, `@PostMapping` | `@PostMapping(value = "/{numeroOrden}/cerrar")` | El método atiende ese verbo HTTP y esa ruta |
| `@PathVariable` | `@PathVariable Long numeroOrden` | Toma un pedazo variable de la URL (`/ordenes/1001/...` → 1001) |
| `@RequestBody` | `@RequestBody PesajeRequest pesaje` | Convierte el JSON del body en un objeto Java (deserializa) |
| `@RequestParam` | `@RequestParam String username` (login) | Toma un parámetro del formulario o del *query string* |
| `produces` / `consumes` | `consumes = MediaType.APPLICATION_JSON_VALUE` | Qué formato devuelve / acepta |

**Persistencia (JPA):** `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`,
`@OneToMany`, `@ManyToMany`, `@JoinColumn`, `@JoinTable`, `@Enumerated`, `@OrderBy`, `@Transient`,
`@Query`. Se explican en 3.6.

**Seguridad:** `@EnableWebSecurity`, `@EnableMethodSecurity`, `@PreAuthorize`. Se explican en 3.8.

**Swagger:** `@Tag`, `@Operation`, `@ApiResponses`, `@ApiResponse`, `@Schema`. Ver 4.10.

**JSON (Jackson):** `@JsonIgnore` (ese atributo no aparece en el JSON).

**Lombok** (genera código al compilar, para no escribir lo repetitivo):

| Anotación | Genera |
|-----------|--------|
| `@Getter`, `@Setter` | Todos los getters y setters |
| `@NoArgsConstructor` | Constructor vacío (JPA lo necesita para crear los objetos al leer de la base) |
| `@AllArgsConstructor` | Constructor con todos los atributos |
| `@Builder` | Patrón *Builder*: `BadRequestException.builder().message("...").build()` |
| `@Slf4j` | Una variable `log` para escribir en el log |

## 3.4. Maven y el `pom.xml`

- **Maven** maneja las **dependencias** (las librerías, incluidas las que dependen de otras) y la
  compilación. Las librerías se bajan una sola vez a `~/.m2/repository` y se comparten entre proyectos.
- Un artefacto se identifica con **groupId + artifactId + version**. El nuestro: `ar.edu.iua` /
  `iw3` / `0.0.1-SNAPSHOT`. El padre `spring-boot-starter-parent 4.1.1` fija las versiones probadas
  de todo lo de Spring.
- Dependencias que usamos:

| Dependencia | Para qué |
|-------------|----------|
| `spring-boot-starter-webmvc` | Controladores REST, Tomcat, Jackson (JSON) |
| `spring-boot-starter-data-jpa` | JPA + Hibernate + pool de conexiones Hikari |
| `mysql-connector-j` | Driver de MySQL |
| `spring-boot-starter-security` | Login, filtros, roles |
| `java-jwt` (com.auth0) | Crear y verificar los tokens JWT |
| `lombok` | Getters, setters, constructores, builder, log |
| `commons-lang3` | `ExceptionUtils` para la traza en `StandartResponse` |
| `springdoc-openapi-starter-webmvc-ui` | Swagger |
| `spring-boot-starter-mail` | Mails (para la alarma del final) |
| `spring-websocket`, `spring-messaging` | WebSocket (tiempo real del final) |
| `spring-boot-devtools` | Reinicio automático al guardar (solo desarrollo) |
| `spring-boot-starter-tomcat` | El Tomcat; su *scope* cambia según el perfil de empaquetado |

- **Perfiles de Maven** (`<profiles>` en el pom):
  - De **empaquetado**: `war-build` (por defecto: `ROOT.war`, Tomcat `provided` porque lo pone el
    servidor) y `jar-build` (`-Dbuild=jar`: `iw3.jar` con el Tomcat adentro).
  - De **ejecución**: `mysqldev` y `mysqlprod` (definen `spring.profiles.active`).
- Comandos: `./mvnw spring-boot:run` (correr) y `./mvnw clean package -Dmaven.test.skip=true`
  (empaquetar; `clean` borra lo compilado antes; los tests se saltean porque necesitan una base).

## 3.5. Configuración y perfiles

- `src/main/resources/application.properties` es la configuración principal de Spring (formato
  `clave=valor`).
- **Perfiles de ejecución:** `application-<perfil>.properties`. Funcionan como **herencia**: el
  archivo del perfil sobrescribe las claves que repite y hereda el resto del principal. El perfil se
  elige con `spring.profiles.active`.
- **Variables de entorno:** `${USUARIOS_PASSWORD:123456}` significa "usá la variable de entorno
  `USUARIOS_PASSWORD`; si no existe, `123456`". Así los secretos no quedan en el repo.

## 3.6. JPA e Hibernate (la base de datos)

- **ORM** (*Object Relational Mapping*): trabajamos con objetos y la herramienta los guarda en tablas.
  **JPA** es la especificación estándar de Java; **Hibernate** es la implementación que viene con
  Spring y escribe el SQL.
- **Entidad** = clase con `@Entity` = una tabla. Lo mínimo es `@Entity` y un `@Id`.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: el id es autonumérico (lo genera MySQL).
- `@Column(nullable = false, unique = true, length = 50)`: restricciones de la columna. `unique`
  crea un índice único (clave secundaria).
- **Relaciones:**
  - `@ManyToOne` + `@JoinColumn(name = "id_camion")`: muchas órdenes apuntan a un camión; en la tabla
    `ordenes` se crea la clave foránea `id_camion`.
  - `@OneToMany`: un camión tiene muchas cisternas.
  - `@ManyToMany` + `@JoinTable`: usuarios y roles (tabla intermedia `userroles`).
  - `cascade = ALL`: lo que se hace con el padre (guardar, borrar) se hace con los hijos.
    `orphanRemoval = true`: un hijo que sale de la lista se borra de la base.
  - `fetch = LAZY` (carga los hijos cuando se piden; por defecto en colecciones) o `EAGER` (los carga
    siempre).
- `@Enumerated(EnumType.STRING)`: guarda el nombre del enum (`"CERRADA"`) en vez del número.
- `@Transient`: ese método o atributo no se guarda en la base.
- `spring.jpa.hibernate.ddl-auto=update`: al arrancar, Hibernate compara las clases con las tablas y
  crea o agrega lo que falta, sin borrar datos. En producción lo ideal es `validate` o `none`.
- **Repositorio:** una interfaz que extiende `JpaRepository<Entidad, TipoDelId>`. Spring la
  implementa sola y trae `findAll`, `findById`, `save` (inserta o actualiza), `deleteById`, etc. Es el
  **DAO** (*Data Access Object*); por eso en el código las variables se llaman `ordenDAO`, `detalleDAO`.
- **Query methods:** métodos cuyo **nombre** describe la consulta y Spring la arma:

| Método | SQL que genera (aproximado) |
|--------|-----------------------------|
| `findByNumeroOrden(Long n)` | `... WHERE numero_orden = ?` |
| `findByCodigoExterno(String c)` | `... WHERE codigo_externo = ?` |
| `findByOrdenIdOrderByFechaAsc(Long id)` | `... WHERE id_orden = ? ORDER BY fecha ASC` |
| `findFirstByOrdenIdOrderByFechaDesc(Long id)` | El más nuevo: `... ORDER BY fecha DESC LIMIT 1` |
| `findOneByUsernameOrEmail(String u, String e)` | `... WHERE username = ? OR email = ?` |

- **`@Query`:** consulta escrita a mano. En JPQL (sobre clases y atributos, no tablas):
  `SELECT AVG(d.temperatura) FROM DetalleCarga d WHERE d.orden.id = ?1` (`?1` es el primer
  parámetro). Para SQL nativo se agrega `nativeQuery = true`.
- `Optional<T>`: lo que devuelven las búsquedas por clave; puede tener el objeto o estar vacío
  (`isEmpty()`), sin usar `null`.
- **Pool de conexiones (Hikari):** abre conexiones a MySQL al arrancar y las reutiliza (mínimo 10,
  máximo 20 en nuestra configuración), porque abrir una conexión es lento y costoso.

## 3.7. Spring MVC: controladores y JSON

- Un `@RestController` recibe el request, llama al negocio y devuelve un **`ResponseEntity`**: el
  cuerpo más el código HTTP (`new ResponseEntity<>(objeto, HttpStatus.OK)`). Se declara
  `ResponseEntity<?>` porque un mismo método puede devolver el objeto bueno o un `StandartResponse`
  de error.
- **Jackson** convierte automáticamente objetos Java a JSON (**serializar**) y JSON a objetos
  (**deserializar**), usando los getters y setters. `@JsonIgnore` saca un atributo del JSON.
- **DTO** (*Data Transfer Object*): clase simple para el cuerpo de un request o response que no es
  una tabla (`PesajeRequest`, `Conciliacion`...).

## 3.8. Spring Security, JWT y roles

**Autenticación** (quién sos):

1. El cliente hace `POST /api/v1/login` con `username` y `password`.
2. El `AuthenticationManager` (nuestro `CustomAuthenticationManager`) busca el usuario, controla que
   la cuenta esté habilitada y compara la contraseña con el **hash BCrypt** guardado.
3. Si está bien, el controlador arma un **JWT** (*JSON Web Token*): un JSON con *claims* (usuario,
   roles, email, versión, vencimiento), codificado en Base64 en tres partes separadas por puntos
   (encabezado.datos.firma) y **firmado** con una clave secreta que solo tiene el servidor. Vence en
   **1 hora**.
4. El cliente manda el token en cada request: `Authorization: Bearer <token>`.
5. Un **filtro** (`JWTAuthorizationFilter`), que se ejecuta antes de llegar al controlador, verifica
   la firma y la fecha, arma el usuario con sus roles y lo guarda en el **`SecurityContextHolder`**
   (el "contexto de seguridad" de ese request).

Por qué funciona: el token se puede **leer** (los datos están en Base64, no cifrados), pero no se
puede **modificar**, porque la firma dejaría de coincidir. Entonces el servidor confía en lo que dice
sin ir a la base en cada request. Esto cumple la restricción **stateless** de REST: el servidor no
guarda sesiones; el "estado" lo tiene el cliente en el token, pero no lo puede alterar.

**Autorización** (qué podés hacer): `@EnableMethodSecurity` habilita `@PreAuthorize` en los métodos.
`@PreAuthorize("hasAnyRole('ADMIN','TMS')")` se evalúa **antes** de ejecutar el método; si el
usuario no tiene el rol, Spring responde **403** sin ejecutarlo. Spring agrega el prefijo `ROLE_`
(en la base el rol se llama `ROLE_TMS`).

**401 vs 403:** 401 = no autenticado (sin token, token vencido o login incorrecto). 403 =
autenticado pero sin el rol.

**Contraseñas:** se guardan con **BCrypt**, un *hash* (no reversible) lento a propósito y con "sal"
aleatoria (dos contraseñas iguales dan hashes distintos). Para comparar se usa
`passwordEncoder.matches(textoPlano, hash)`.

## 3.9. Logs

- Con `@Slf4j` cada clase tiene `log`. Niveles de mayor a menor: ERROR, WARN, INFO, DEBUG, TRACE; al
  elegir un nivel se ven ese y los más graves.
- En `application.properties`: `logging.level.root=WARN` (todo lo de librerías solo advertencias) y
  `logging.level.ar.edu.iua.iw3=DEBUG` (nuestro código con detalle).
- Regla del proyecto: en cada `catch` técnico, `log.error(e.getMessage(), e)` antes de lanzar la
  excepción propia, para no perder la causa.

# 4. Recorrido archivo por archivo

## 4.1. Mapa rápido

| Si te piden... | Abrí |
|------------------------------------------|------------------------------------------------------------|
| Una entidad (tabla) | `model/Orden.java`, `Camion`, `Cisterna`, `Chofer`, `Cliente`, `Producto`, `DetalleCarga`, `CambioEstado` |
| Los estados | `model/EstadoOrden.java` |
| Un repositorio o una consulta | `model/persistence/` (los promedios en `DetalleCargaRepository`) |
| Los JSON de entrada y salida | `model/dto/` |
| La lógica de un punto | `business/OrdenBusiness.java`: `recibirOrden`, `registrarPesajeInicial`, `activarCarga`, `recibirDatoCarga`, `cerrarOrden`, `registrarPesajeFinal`, `getConciliacion` |
| El control de estados | `OrdenBusiness.controlarEstado()` |
| El cambio de estado y la auditoría | `OrdenBusiness.cambiarEstado()`, `registrarCambioEstado()`, `usuarioActual()` |
| La frecuencia del detalle | `OrdenBusiness.correspondeGuardarDetalle()` + `carga.detalle.frecuencia.segundos` |
| La contraseña de activación | `OrdenBusiness.generarPassword()` |
| Los cálculos de la conciliación | `OrdenBusiness.armarConciliacion()` |
| La sincronización por código externo | `business/CamionBusiness.sincronizar()` (igual en Chofer, Cliente, Producto) |
| Las excepciones | `business/execption/` |
| Los endpoints, códigos HTTP y roles | `controllers/OrdenRestController.java` |
| Las rutas | `controllers/Constants.java` |
| El login y la creación del token | `auth/controller/AuthRestController.java` |
| La validación de usuario y contraseña | `auth/custom/CustomAuthenticationManager.java` |
| El filtro que valida el token | `auth/filters/JWTAuthorizationFilter.java` |
| La clave y la duración del token | `auth/filters/AuthConstants.java` |
| Rutas públicas, BCrypt, stateless | `security/SecurityConfiguration.java` |
| Los usuarios y roles iniciales | `auth/DatosIniciales.java` |
| La respuesta de error | `util/StandartResponse.java` + `util/StandartResponseBusiness.java` |
| Swagger | `config/OpenApiConfig.java` + anotaciones en `OrdenRestController` |
| La configuración | `src/main/resources/application.properties` y los dos de perfil |
| Jar/war y perfiles de Maven | `iw3/pom.xml` |
| El arranque | `Iw3Application.java` |
| El deploy | `infra/deploy.sh` |
| Las pruebas | `postman/IW3-Integrador.postman_collection.json` |

## 4.2. `Iw3Application.java` (arranque)

```java
@SpringBootApplication
@Slf4j
public class Iw3Application extends SpringBootServletInitializer implements CommandLineRunner {
    public static void main(String[] args) { SpringApplication.run(Iw3Application.class, args); }
    @Value("${spring.profiles.active}") private String profile;
    @Value("${spring.jackson.time-zone:-}") private String backendTimezone;
    public void run(String... args) { ... TimeZone.setDefault(...); log.info("- Perfil activo {}", profile); }
}
```

- `@SpringBootApplication`: escanea `ar.edu.iua.iw3` y subpaquetes.
- `extends SpringBootServletInitializer`: necesario para que el `.war` arranque dentro del Tomcat de
  la VM. **Por qué:** en clase, sin esto el war no levantaba en el Tomcat externo.
- `CommandLineRunner.run()`: se ejecuta cuando Spring terminó de arrancar. Fija la zona horaria
  (`America/Argentina/Buenos_Aires`, de `spring.jackson.time-zone`) y muestra el perfil activo en el
  log; así en `docker logs backend` se ve si arrancó con `mysqlprod`.

## 4.3. El modelo (`model/`)

### `Orden.java` (tabla `ordenes`)

Es el centro del sistema. Campos y anotaciones importantes:

| Código | Por qué |
|--------|---------|
| `@Id @GeneratedValue(strategy = IDENTITY) private Long id;` | Clave principal autonumérica interna. El profe recomienda siempre un entero largo autonumérico como clave principal. |
| `@Column(nullable = false, unique = true) private Long numeroOrden;` | El número que conocen los sistemas externos. `unique` = clave secundaria. Las rutas usan este número, no el `id`. |
| `@ManyToOne @JoinColumn(name = "id_camion", nullable = false) private Camion camion;` (igual chofer, cliente, producto) | Muchas órdenes pueden usar el mismo camión. Crea la clave foránea `id_camion`. |
| `@Enumerated(EnumType.STRING) private EstadoOrden estado;` | Se guarda `"CERRADA"` en vez de 2: se lee en la base sin buscar qué número es qué. |
| `private Date fechaPrevistaCarga; private Double preset;` | Datos de SAP: turno y kg a cargar. |
| `private Double tara; private Double pesajeFinal;` | Pesajes. `Double` (objeto) y no `double` (primitivo) para que puedan ser `null` mientras no se pesó. |
| `@JsonIgnore private Integer password;` | La contraseña nunca aparece al consultar la orden; solo se devuelve una vez en el pesaje inicial. |
| `fechaRecepcion`, `fechaPesajeInicial`, `fechaInicioCarga`, `fechaFinCarga`, `fechaPesajeFinal` | Una fecha por evento, como pide la consigna (auditoría). |
| `ultimaMasaAcumulada`, `ultimaDensidad`, `ultimaTemperatura`, `ultimoCaudal`, `fechaUltimoDato` | Los últimos valores de la carga en la cabecera, "para que estén muy accesibles" (consigna); se pisan con cada dato válido. |
| `public int getNumeroEstado()` | Devuelve 1 a 4; aparece en el JSON como `numeroEstado` porque Jackson usa los getters. |

Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`.

### `EstadoOrden.java` (enum)

```java
PENDIENTE_PESAJE_INICIAL, CON_PESAJE_INICIAL, CERRADA, FINALIZADA;
public int getNumero() { return this.ordinal() + 1; }
```

El orden en que están escritos es el orden del proceso; `ordinal()` es la posición (0 a 3), por eso
se suma 1.

### Entidades externas: `Camion`, `Cisterna`, `Chofer`, `Cliente`, `Producto`

- Todas tienen `id` autonumérico y **`@Column(unique = true) String codigoExterno`**: el código con
  el que las identifica SAP. Es `String` porque la consigna lo pide "para mayor flexibilidad y evitar
  dependencias rígidas entre sistemas". Es el patrón de integración que mostró el profe en clase
  (un código externo `unique` en la entidad).
- Campos (los opcionales según la consigna van sin `nullable = false`):

| Entidad | Tabla | Campos |
|---------|-------|--------|
| `Camion` | `camiones` | `codigoExterno`, `patente`, `descripcion` (opcional), `cisternas` |
| `Cisterna` | `cisternas` | `numero`, `capacidadLitros` (no tiene código externo: solo existe dentro de un camión) |
| `Chofer` | `choferes` | `codigoExterno`, `nombre`, `apellido`, `documento` (opcional) |
| `Cliente` | `clientes` | `codigoExterno`, `razonSocial`, `contacto` (opcional) |
| `Producto` | `productos` | `codigoExterno`, `nombre`, `descripcion` (opcional) |

- En `Camion`:

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
@JoinColumn(name = "id_camion")
@OrderBy("numero")
private List<Cisterna> cisternas = new ArrayList<>();

public double getTotalLitros() { ... suma las capacidades ... }
```

  - `cascade = ALL`: al guardar el camión se guardan sus cisternas.
  - `orphanRemoval = true`: si una cisterna sale de la lista, se borra de la base (así funciona el
    reemplazo al sincronizar).
  - `EAGER`: las cisternas son pocas y siempre se muestran con el camión. El profe dijo que EAGER
    solo conviene cuando los hijos se usan siempre: es este caso.
  - `@JoinColumn(name = "id_camion")`: la clave foránea queda en la tabla `cisternas`.
  - `getTotalLitros()`: el total **no se guarda**, se calcula. El profe: "todo lo que sea calculable
    no hay que almacenarlo", porque si se guarda hay que mantenerlo actualizado.

### `DetalleCarga.java` (tabla `detalles_carga`)

Histórico de la carga: `masaAcumulada`, `densidad`, `temperatura`, `caudal`, `fecha` y la orden
(`@ManyToOne @JoinColumn(name = "id_orden")`). La orden tiene `@JsonIgnore` para que al listar
detalles no se repita la orden completa en cada uno (y no haya un ciclo al convertir a JSON). Acá los
números son `double` (primitivos) porque un detalle solo se crea con los cuatro valores presentes.

### `CambioEstado.java` (tabla `cambios_estado`)

El log de auditoría: `orden`, `estadoAnterior` (null al crear la orden), `estadoNuevo`, `fecha`,
`usuario` y `observacion`. Los estados con `@Enumerated(STRING)`.

## 4.4. Los DTO (`model/dto/`)

| Clase | JSON | Dónde se usa |
|-------|------|--------------|
| `PesajeRequest` | `{"peso": 15000}` | Body de los puntos 2 y 5 |
| `ActivacionRequest` | `{"password": 84921}` | Body del punto 3a |
| `DatoCargaRequest` | `{"masaAcumulada", "densidad", "temperatura", "caudal"}` | Body del punto 3b. Todos `Double` para poder detectar si falta alguno (`null`) |
| `PasswordResponse` | `{"numeroOrden", "password"}` | Respuesta del punto 2 |
| `PresetResponse` | `{"numeroOrden", "preset"}` | Respuesta del punto 3a |
| `Conciliacion` | Los 9 campos de la conciliación | Respuesta del punto 5. No es una tabla: se calcula |

**Por qué DTOs:** el body de un pesaje no es una orden; con una clase chica el JSON es exactamente
lo que hace falta. El punto 1, en cambio, recibe la entidad `Orden` directamente porque su JSON tiene
la misma forma que la orden.

## 4.5. Los repositorios (`model/persistence/`)

Todos son interfaces con `@Repository` que extienden `JpaRepository<Entidad, Long>`:

| Repositorio | Métodos propios |
|-------------|-----------------|
| `OrdenRepository` | `Optional<Orden> findByNumeroOrden(Long numeroOrden)` |
| `CamionRepository`, `ChoferRepository`, `ClienteRepository`, `ProductoRepository` | `Optional<X> findByCodigoExterno(String codigoExterno)` |
| `DetalleCargaRepository` | `findByOrdenIdOrderByFechaAsc`, `findFirstByOrdenIdOrderByFechaDesc` (el último detalle), y tres `@Query` con `AVG` |
| `CambioEstadoRepository` | `findByOrdenIdOrderByFechaAsc` |

```java
@Query("SELECT AVG(d.temperatura) FROM DetalleCarga d WHERE d.orden.id = ?1")
Double promedioTemperatura(Long idOrden);
```

Es **JPQL**: usa el nombre de la clase (`DetalleCarga`) y de los atributos (`d.orden.id`), no de las
tablas. Devuelve `Double` porque si no hay detalles el promedio es `null`. **Por qué en la base:** el
motor calcula el promedio sin traer todos los detalles a Java.

## 4.6. El negocio (`business/`)

### Interfaces e implementaciones

Cada negocio es una interfaz (`IOrdenBusiness`, `ICamionBusiness`...) y una clase con `@Service`
que la implementa. Los demás siempre piden la interfaz con `@Autowired`. **Por qué:** es el patrón de
la materia; la interfaz dice qué hace el negocio (con qué excepciones) sin mostrar el cómo, y la
implementación se puede cambiar sin tocar a quien la usa.

### Las excepciones (`business/execption/`)

| Excepción | Código HTTP | Origen |
|-----------|------|--------|
| `BusinessException` | 500 | Proyecto base: error técnico inesperado |
| `NotFoundException` | 404 | Proyecto base: lo buscado no existe |
| `FoundException` | 302 | Proyecto base: lo que se quiere crear ya existe |
| `BadRequestException` | 400 | Nuestra: datos inválidos |
| `ConflictException` | 409 | Nuestra: la orden no está en el estado que la acción necesita |

Todas extienden `Exception`, tienen los tres constructores (mensaje + causa, mensaje, causa) con
`@Builder` de Lombok, y se crean así: `ConflictException.builder().message("...").build()`. El paquete
se llama `execption` (con el error de tipeo) porque así viene del proyecto del profesor; renombrarlo
no aportaba nada.

**Patrón de manejo de errores** (en todos los negocios):

```java
try {
    return ordenDAO.findAll();
} catch (Exception e) {
    log.error(e.getMessage(), e);              // no se pierde la causa
    throw BusinessException.builder().ex(e).build();  // se envuelve en nuestra excepción
}
```

Así el sistema tiene pocas excepciones propias en lugar de una por cada librería (lo que recomendó
el profe).

### `CamionBusiness` (y `ChoferBusiness`, `ClienteBusiness`, `ProductoBusiness`)

Métodos: `list()`, `load(codigoExterno)` y **`sincronizar(recibido)`**:

```java
if (recibido == null || recibido.getCodigoExterno() == null || ...isBlank())
    throw BadRequestException...;                       // sin código no se puede sincronizar
Optional<Camion> existente = camionDAO.findByCodigoExterno(recibido.getCodigoExterno());
if (existente.isPresent()) {                             // ya existe: se actualiza
    Camion actual = existente.get();
    actual.setPatente(recibido.getPatente());
    actual.setDescripcion(recibido.getDescripcion());
    actual.getCisternas().clear();                       // se reemplazan las cisternas
    recibido.getCisternas().forEach(c -> c.setId(null));
    actual.getCisternas().addAll(recibido.getCisternas());
    return camionDAO.save(actual);
}
recibido.setId(null);                                    // es nuevo: el id lo pone la base
return camionDAO.save(recibido);
```

**Por qué:** la consigna dice que los datos de las entidades externas "deben sincronizarse": si no
existen se crean y si existen se actualizan. Se busca por `codigoExterno` porque SAP no conoce
nuestros `id`. `clear()` + `orphanRemoval` borra las cisternas viejas. Se pone `id = null` para que
`save` siempre inserte lo nuevo y nadie pueda pisar un registro mandando un id.

### `OrdenBusiness` (el archivo más importante)

Atributos inyectados: los cuatro repositorios (`ordenDAO`, `detalleDAO`, `cambioEstadoDAO`), los
cuatro negocios externos y

```java
@Value("${carga.detalle.frecuencia.segundos:10}")
private long frecuenciaDetalleSegundos;
```

**`load(numeroOrden)`**: busca con `findByNumeroOrden`; si el `Optional` está vacío lanza
`NotFoundException` ("No se encuentra la orden numero=..."). Todos los puntos empiezan con `load`.

**Punto 1 - `recibirOrden(orden)`:**

1. `numeroOrden` obligatorio y `preset > 0` → si no, `BadRequestException`.
2. Controla que no exista:
   ```java
   try {
       load(orden.getNumeroOrden());
       throw FoundException...("Ya existe la orden ...");
   } catch (NotFoundException e) { /* no existe: seguimos */ }
   ```
   Si `load` encuentra la orden, se lanza `FoundException`; si lanza `NotFoundException`, es lo
   esperado. Es la misma lógica que el `add` de `ProductBusiness` en clase.
3. Sincroniza camión, chofer, cliente y producto (cada `sincronizar` devuelve la entidad guardada,
   con su `id`, y se la asigna a la orden).
4. Pone en `null` todos los datos internos (id, tara, password, fechas, últimos valores). **Por qué:**
   como el body se recibe como `Orden`, alguien podría mandar una tara o un estado; esos datos no los
   define SAP.
5. `fechaRecepcion = new Date()`, estado 1, guarda y registra el cambio de estado (anterior `null`,
   "Orden recibida").

**Punto 2 - `registrarPesajeInicial(numeroOrden, tara)`:** peso > 0, `load`,
`controlarEstado(orden, PENDIENTE_PESAJE_INICIAL)`, guarda tara y fecha, genera la contraseña y
`cambiarEstado(orden, CON_PESAJE_INICIAL, "Tara registrada: ...")`.

```java
private int generarPassword() {
    return 10000 + (int) (Math.random() * 90000);
}
```

`Math.random()` da un número entre 0 y 1 (sin el 1); × 90000 da entre 0 y 89999; + 10000 queda entre
**10000 y 99999**: siempre 5 dígitos. La genera el backend porque lo dice el diagrama de la consigna
y el profe lo confirmó en el video ("nuestro backend genera una password de activación").

**Punto 3a - `activarCarga(numeroOrden, password)`:** `load`, `controlarEstado(CON_PESAJE_INICIAL)`,
compara con `password.equals(orden.getPassword())` (si no coincide, `BadRequestException`) y devuelve
la orden; el controlador arma `PresetResponse`. **No cambia el estado**: la orden sigue en 2 mientras
carga.

**Punto 3b - `recibirDatoCarga(numeroOrden, dato)`:**

1. `load` y `controlarEstado(CON_PESAJE_INICIAL)`. Si la orden está cerrada, esto da 409: **así se
   bloquea la recepción después del punto 4**.
2. Descarta (400) si falta algún valor, si `caudal <= 0` (bomba apagada), si `masaAcumulada <= 0` o si
   la masa es menor a la anterior (`orden.getUltimaMasaAcumulada()`), porque la masa es acumulativa.
3. Dato válido: si `fechaInicioCarga` es null la pone (primer dato), pone `fechaFinCarga = ahora`
   (siempre el último dato válido; al cerrar queda la del último), actualiza los últimos valores y
   guarda la orden. **Siempre**, aunque no se guarde detalle.
4. Si `correspondeGuardarDetalle(...)`, crea y guarda un `DetalleCarga`.

```java
private boolean correspondeGuardarDetalle(Orden orden, Date ahora) {
    Optional<DetalleCarga> ultimo = detalleDAO.findFirstByOrdenIdOrderByFechaDesc(orden.getId());
    if (ultimo.isEmpty()) return true;                       // el primero siempre se guarda
    long segundos = (ahora.getTime() - ultimo.get().getFecha().getTime()) / 1000;
    return segundos >= frecuenciaDetalleSegundos;
}
```

`getTime()` da milisegundos; dividido 1000 son segundos. **Por qué así:** la consigna dice "cada 10
segundos" (frecuencia de tiempo), entonces se mide el tiempo desde el último guardado. Ejemplo: datos
cada 1 s durante un minuto → llegan 60, se guardan 6. Con frecuencia 0 se guardan todos.

**Punto 4 - `cerrarOrden(numeroOrden)`:** `load`, `controlarEstado(CON_PESAJE_INICIAL)`,
`cambiarEstado(CERRADA, "Orden cerrada para carga")`.

**Punto 5 - `registrarPesajeFinal(numeroOrden, pesajeFinal)`:** peso > 0, `load`,
`controlarEstado(CERRADA)`, guarda pesaje final y fecha, `cambiarEstado(FINALIZADA, ...)` y devuelve
`armarConciliacion(orden)`. **`getConciliacion(numeroOrden)`:** `load`, `controlarEstado(FINALIZADA)`
y `armarConciliacion`.

```java
double productoCargado = orden.getUltimaMasaAcumulada() == null ? 0 : orden.getUltimaMasaAcumulada();
double neto = orden.getPesajeFinal() - orden.getTara();
c.setNetoPorBalanza(neto);
c.setDiferenciaBalanzaCaudalimetro(neto - productoCargado);
c.setPromedioTemperatura(detalleDAO.promedioTemperatura(orden.getId()));  // y densidad, caudal
```

Si nunca llegó un dato, el producto cargado es 0 (operador ternario `? :`). La conciliación **no se
guarda**: se calcula cada vez con datos que ya están en la base, así nunca queda desactualizada.

**Métodos de apoyo:**

```java
private void controlarEstado(Orden orden, EstadoOrden esperado) throws ConflictException {
    if (orden.getEstado() != esperado)
        throw ConflictException.builder().message("La orden ... está en estado ... y para esta acción debe estar en ...").build();
}

private void cambiarEstado(Orden orden, EstadoOrden nuevo, String observacion) {
    EstadoOrden anterior = orden.getEstado();
    orden.setEstado(nuevo);
    guardar(orden);
    registrarCambioEstado(orden, anterior, nuevo, observacion);
}

private String usuarioActual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) return "sistema";
    return auth.getName();
}
```

- Los enums se comparan con `!=` porque cada valor existe una sola vez en memoria.
- **Cómo se garantiza que los estados solo avanzan:** cada acción exige un estado puntual
  (`controlarEstado`) y pasa al siguiente (`cambiarEstado`), y no hay otro código que cambie el estado
  (salvo el estado inicial al crear la orden).
- **De dónde sale el usuario de la auditoría:** el filtro JWT dejó el usuario en el
  `SecurityContextHolder`; `getName()` devuelve el *subject* del token (`sap`, `tms`, `carga`...). Si
  no hay usuario (no debería pasar, porque todo requiere login) se registra `"sistema"`.

## 4.7. Los controladores (`controllers/`)

### `Constants.java`

```java
URL_API = "/api";  URL_API_VERSION = "/v1";  URL_BASE = URL_API + URL_API_VERSION;
URL_ORDENES = URL_BASE + "/ordenes";  URL_LOGIN = URL_BASE + "/login";  URL_MAIL = URL_BASE + "/mail";
```

Clase `final` con constantes: si cambia la versión de la API, se cambia en un solo lugar. Versionar
la API desde el principio es la recomendación de la clase de REST.

### `BaseRestController.java`

Padre de todos los controladores. Tiene `getUserLogged()`, que saca el usuario del
`SecurityContextHolder` (`(User) auth.getPrincipal()`). Viene del proyecto base.

### `OrdenRestController.java`

```java
@RestController
@RequestMapping(Constants.URL_ORDENES)
@Tag(name = "Órdenes", ...)
public class OrdenRestController extends BaseRestController {
    @Autowired private IOrdenBusiness ordenBusiness;
    @Autowired private IStandartResponseBusiness response;
```

Cada método tiene la misma forma. Ejemplo, punto 2:

```java
@PreAuthorize("hasAnyRole('ADMIN','TMS')")
@PostMapping(value = "/{numeroOrden}/pesaje-inicial", consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<?> pesajeInicial(@PathVariable Long numeroOrden, @RequestBody PesajeRequest pesaje) {
    try {
        Orden orden = ordenBusiness.registrarPesajeInicial(numeroOrden, pesaje.getPeso());
        return new ResponseEntity<>(new PasswordResponse(orden.getNumeroOrden(), orden.getPassword()), HttpStatus.OK);
    } catch (NotFoundException e)   { return error(HttpStatus.NOT_FOUND, e);
    } catch (ConflictException e)   { return error(HttpStatus.CONFLICT, e);
    } catch (BadRequestException e) { return error(HttpStatus.BAD_REQUEST, e);
    } catch (BusinessException e)   { return error(HttpStatus.INTERNAL_SERVER_ERROR, e); }
}

private ResponseEntity<?> error(HttpStatus status, Exception e) {
    return new ResponseEntity<>(response.build(status, e, e.getMessage()), status);
}
```

- El controlador **no tiene lógica**: llama al negocio y traduce cada excepción a un código.
- `error(...)` es un método nuestro para no repetir la construcción del `StandartResponse` en cada
  `catch`.
- Punto 1: devuelve **201 Created** con el header `location: /api/v1/ordenes/{numero}` y sin cuerpo.
  **Por qué:** es lo que enseñó el profe (hypermedia): no mandar el objeto completo
  "compulsivamente"; si el cliente lo quiere, usa el `Location`.
- Puntos 3b y 4 devuelven 200 sin cuerpo.
- Conciliación: `@PreAuthorize("isAuthenticated()")`, cualquier usuario logueado.
- Roles por endpoint: SAP → punto 1; TMS → 2 y 5; CARGA → 3a, 3b y 4; OPERADOR → consultas; ADMIN
  todo.

## 4.8. La seguridad (`security/` y `auth/`)

### `security/SecurityConfiguration.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    @Bean PasswordEncoder bCryptPasswordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean WebMvcConfigurer corsConfigurer() { ... allowedOrigins("*") ... }
    @Bean AuthenticationManager authenticationManager() {
        return new CustomAuthenticationManager(bCryptPasswordEncoder(), userBusiness);
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, Constants.URL_LOGIN).permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**" ...).permitAll()
            .requestMatchers("/demo/**").permitAll()
            .anyRequest().authenticated());
        http.httpBasic(...).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilter(new JWTAuthorizationFilter(authenticationManager()));
        return http.build();
    }
}
```

| Línea | Para qué |
|-------|----------|
| `@EnableMethodSecurity(prePostEnabled = true)` | Habilita `@PreAuthorize` en los controladores |
| `BCryptPasswordEncoder` como `@Bean` | Cualquier clase que pida un `PasswordEncoder` recibe BCrypt; se cambia en un solo lugar |
| `corsConfigurer` | CORS: permite que un frontend de otro origen (otro dominio o puerto) llame a la API |
| `csrf(disable)` | CSRF es un ataque que aprovecha las cookies de sesión del navegador; como no usamos cookies sino un token en el header, no aplica |
| `permitAll` en el login | "Si no, dejaste la llave adentro y cerraste la puerta" (profe): sin login abierto nadie podría loguearse |
| `permitAll` en `/error` | **Cambio nuestro.** Cuando un usuario sin rol llamaba a un endpoint, Spring redirigía a `/error`, que también pedía login, y la respuesta terminaba siendo 401 en vez de 403 |
| `permitAll` en Swagger | La documentación es pública, como en clase |
| `anyRequest().authenticated()` | Todo lo demás requiere token |
| `STATELESS` | Spring no crea sesiones en el servidor: cumple la restricción sin estado de REST |
| `addFilter(new JWTAuthorizationFilter(...))` | Agrega nuestro filtro a la cadena |

### `auth/filters/AuthConstants.java`

```java
EXPIRATION_TIME = 60 * 60 * 1000;   // 1 hora en milisegundos
SECRET = "...";                     // clave para firmar los tokens
AUTH_HEADER_NAME = "Authorization"; AUTH_PARAM_NAME = "authtoken"; TOKEN_PREFIX = "Bearer ";
```

### `auth/controller/AuthRestController.java` (el login)

```java
@PostMapping(value = Constants.URL_LOGIN)
public ResponseEntity<?> loginExternalOnlyToken(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {
    try {
        auth = authManager.authenticate(((CustomAuthenticationManager) authManager).authWrap(username, password));
    } catch (AuthenticationServiceException e0) { ... 500 ... }
      catch (AuthenticationException e)         { ... 401 ... }
    User user = (User) auth.getPrincipal();
    String token = JWT.create().withSubject(user.getUsername())
        .withClaim("internalId", user.getIdUser())
        .withClaim("roles", new ArrayList<String>(user.getAuthoritiesStr()))
        .withClaim("email", user.getEmail())
        .withClaim("version", "1.0.0")
        .withExpiresAt(new Date(System.currentTimeMillis() + AuthConstants.EXPIRATION_TIME))
        .sign(Algorithm.HMAC512(AuthConstants.SECRET.getBytes()));
    applicationEventPublisher.publishEvent(new UserEvent(user, request, UserEvent.TypeEvent.LOGIN));
    return new ResponseEntity<String>(token, HttpStatus.OK);
}
```

- Recibe usuario y contraseña como parámetros de formulario (`form-data` en Postman).
- `authWrap` arma un objeto `Authentication` con solo usuario y contraseña (una **clase adaptadora**:
  implementa la interfaz pero solo los métodos que interesan).
- `getPrincipal()` devuelve el usuario completo que cargó el `CustomAuthenticationManager`.
- El token se firma con **HMAC512** y la clave secreta. `withSubject` (el usuario) y `withExpiresAt`
  son los datos obligatorios; los claims (roles, email, versión) son los que agregamos.
- Publica un **evento** de login (`UserEvent`), que escucha `UserEventListener` y lo escribe en el log
  (patrón Observer, visto en clase; el login no sabe quién escucha).
- **Cambio nuestro:** se quitó `produces = TEXT_PLAIN`. Con eso, un login incorrecto daba 500
  (el error en JSON no se podía convertir a texto); ahora da 401.
- `GET /demo/encodepass?password=...` devuelve el hash BCrypt de un texto (utilidad del proyecto base).

### `auth/custom/CustomAuthenticationManager.java`

```java
public Authentication authenticate(Authentication authentication) {
    String username = authentication.getName();
    String password = authentication.getCredentials().toString();
    user = userBusiness.load(username);           // NotFound -> BadCredentialsException (401)
    String validation = user.validate();          // cuenta vencida, bloqueada, deshabilitada
    if (... ) throw new AccountExpiredException / CredentialsExpiredException / DisabledException / LockedException;
    if (!pEncoder.matches(password, user.getPassword())) throw new BadCredentialsException("Invalid password");
    return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
}
```

- Se ejecuta **una sola vez, en el login**.
- Usa las excepciones de Spring Security porque es parte del circuito de Spring.
- El resultado: un `Authentication` con el usuario como **principal**, credenciales `null` (no se
  guarda la contraseña) y los roles.

### `auth/filters/JWTAuthorizationFilter.java`

```java
protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
    String header = req.getHeader("Authorization");
    String param = req.getParameter("authtoken");
    boolean byHeader = header empieza con "Bearer ";
    boolean byParam = param tiene al menos 10 caracteres;
    if (!byHeader && !byParam) { chain.doFilter(req, res); return; }   // no hay token: sigue
    UsernamePasswordAuthenticationToken authentication = getAuthentication(req, byHeader);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    chain.doFilter(req, res);
}
// getAuthentication: JWT.require(Algorithm.HMAC512(SECRET)).build().verify(token)
//   -> lee subject, roles, email, internalId y arma un User + UsernamePasswordAuthenticationToken.
//   Si la firma no coincide o el token venció, verify lanza excepción y devuelve null.
```

- Se ejecuta **en cada request**.
- Si no viene token **no rechaza**, pasa al siguiente filtro: puede haber otro método de
  autenticación (está habilitado `httpBasic`). Si al final nadie autenticó y la ruta lo requiere,
  Spring responde 401.
- **No consulta la base**: arma el usuario con lo que dice el token.
- El token puede venir por header (prioridad) o por el parámetro `?authtoken=` (para herramientas a
  las que les cuesta mandar headers).

### `auth/User.java`, `Role.java`, `UserBusiness`, `UserRepository`

- `User implements UserDetails` (la interfaz que entiende Spring): `username`, `password` (hash),
  `email` (unique), los cuatro booleanos de la cuenta (`enabled`, `accountNonLocked`...) y
  `@ManyToMany(fetch = EAGER) Set<Role> roles` con la tabla `userroles`.
- `validate()` devuelve `"OK"` o el motivo (expirada, bloqueada, deshabilitada).
- `getAuthorities()` (con `@Transient`) convierte los roles en `SimpleGrantedAuthority` usando
  *streams*: `roles.stream().map(r -> new SimpleGrantedAuthority(r.getName())).collect(...)`.
- `Set` porque un usuario no puede tener el mismo rol dos veces.
- `UserRepository.findOneByUsernameOrEmail`: se puede loguear con el usuario o el email.

### `auth/DatosIniciales.java`

`@Component` + `CommandLineRunner`: al arrancar crea los roles `ROLE_ADMIN`, `ROLE_SAP`, `ROLE_TMS`,
`ROLE_CARGA`, `ROLE_OPERADOR` y los usuarios `admin`, `sap`, `tms`, `carga`, `operador` si no
existen. La contraseña sale de `@Value("${usuarios.password.inicial}")`. Si el usuario ya existe y
`!passwordEncoder.matches(passwordInicial, u.getPassword())`, le actualiza la contraseña.

**Por qué:** cualquiera levanta el proyecto con la base vacía y ya tiene usuarios para probar; y en
la VM la contraseña se cambia con la variable de entorno `USUARIOS_PASSWORD` sin tocar código ni la
base.

## 4.9. Utilidades (`util/`)

- **`StandartResponse`**: el cuerpo de todos los errores. Atributos `message`, `ex` y `httpStatus`
  (los dos últimos con `@JsonIgnore`) y `devInfoEnabled`. El JSON queda:
  `{"message": "...", "code": 409, "devInfo": "traza o null"}`. `getCode()` y `getDevInfo()` son
  getters calculados: Jackson los muestra como atributos. `devInfo` usa `ExceptionUtils` de
  commons-lang3 para la traza.
- **`StandartResponseBusiness`** (`@Service`): fábrica (patrón *Factory*) que arma el
  `StandartResponse` leyendo `@Value("${dev.info.enabled:false}")`. En desarrollo `true` (se ve la
  traza), en la VM `false` (no se muestra información interna).
- `EmailBusiness`, `JsonUtiles`: del proyecto base. `EmailBusiness` se va a usar para la alarma del
  final; `MailRestController` tiene un endpoint de prueba solo para ADMIN.

## 4.10. Swagger (`config/OpenApiConfig.java`)

`@Configuration` con `@SecurityScheme` (autenticación Bearer JWT, para que Swagger tenga el botón
**Authorize**) y un `@Bean OpenAPI` con título, descripción y versión. En `OrdenRestController`: `@Tag`
agrupa los endpoints bajo "Órdenes", `@Operation` describe cada uno y `@ApiResponses` lista los
códigos posibles con `@Schema(implementation = ...)` para mostrar el formato. Se ve en
`/swagger-ui.html`.

## 4.11. Preparado para el final

- `events/AsyncSpringEventsConfig`: configura los eventos de Spring como **asíncronos** (en otro hilo,
  para no frenar el request).
- `auth/events/UserEvent` y `UserEventListener`: el evento de login (Observer).
- `websocket/WebSocketConfig` y `static/ui/js/` (sockjs, stomp): para el tiempo real del frontend.
- Idea para la alarma de temperatura: en `recibirDatoCarga`, si la temperatura supera el umbral,
  publicar un evento; un listener asíncrono crea la alarma y manda el mail con `EmailBusiness`.

## 4.12. Configuración (`src/main/resources/`)

**`application.properties`** (lo importante):

```properties
spring.profiles.active=mysqldev                  # perfil por defecto (PC)
dev.info.enabled=true                            # traza en los errores
spring.jackson.time-zone=America/Argentina/Buenos_Aires
spring.datasource.url=jdbc:mysql://localhost:33306/iw3_2026?createDatabaseIfNotExist=true&...
spring.datasource.username=iw3 / password=iw3pass
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect   # dialecto: SQL óptimo para MySQL
spring.jpa.hibernate.ddl-auto=update
logging.level.root=WARN / logging.level.ar.edu.iua.iw3=DEBUG
spring.datasource.hikari.minimum-idle=10 / maximum-pool-size=20
spring.mail.username=${SMTP_USER:} / spring.mail.password=${SMTP_PASS:}
carga.detalle.frecuencia.segundos=10
usuarios.password.inicial=${USUARIOS_PASSWORD:123456}
```

- La URL JDBC: protocolo `jdbc`, subprotocolo `mysql`, host, puerto `33306` (el MySQL de Docker en la
  PC), base `iw3_2026`; `createDatabaseIfNotExist=true` crea la base si no existe.
- `carga.detalle.frecuencia.segundos` y `usuarios.password.inicial` son **propiedades nuestras**.

**`application-mysqldev.properties`**: `dev.info.enabled=true` y la misma URL local.

**`application-mysqlprod.properties`** (VM): `dev.info.enabled=false`, URL
`jdbc:mysql://mysql:3306/iw3_prod_2026` (**`mysql` es el nombre del servicio en el docker-compose**,
se resuelve por la red interna de Docker), usuario y contraseña de la infraestructura del profesor,
`show-sql=false`, `ddl-auto=update`.

En la VM el perfil lo activa el Tomcat: `~/infra/tomcat/conf/catalina.properties` tiene
`spring.profiles.active=mysqlprod`.

## 4.13. Fuera del código

- **`infra/deploy.sh`** (ver 6.6).
- **`postman/`**: la colección y dos entornos (`IW3-Local` con `http://localhost:8080`, `IW3-VM` con
  `https://iw3turina.duckdns.org`). Carpetas:
  - **0 - Login**: los cinco usuarios; cada login guarda su token en una variable (`tokenSap`,
    `tokenTms`...) con un script de Tests.
  - **1 - Circuito completo**: una orden de punta a punta, con un dato descartado (caudal 0) y uno
    rechazado por orden cerrada, más el historial de estados y los detalles.
  - **2 - Errores esperados**: 302, 404, 409, 400, 403 y dos 401.
  - **3 - Consultas**.
  - El punto 1 genera un número de orden nuevo en cada corrida y el punto 2 guarda la contraseña en
    una variable, por eso se puede correr con el **Runner** cuantas veces se quiera. Resultado: 30
    requests, 34 tests, 0 fallas (local y VM).
- **`docs/`**: `DOCUMENTACION.md` (y su Word), `BITACORA.md`, `PENDIENTES.md`, esta guía.
- **`AGENTS.md` / `CLAUDE.md`**: reglas para las herramientas de IA que use el grupo.

# 5. Un request de punta a punta

Ejemplo: el TMS registra la tara de la orden 1001 en la VM.

```
POST https://iw3turina.duckdns.org/api/v1/ordenes/1001/pesaje-inicial
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{"peso": 15000}
```

1. **DNS:** DuckDNS traduce `iw3turina.duckdns.org` a la IP pública de la VM del grupo.
2. **nginx** (contenedor, puerto 443): descifra el HTTPS con el certificado de Let's Encrypt y, como
   la ruta empieza con `/`, reenvía el request a `http://backend:8080` por la red interna de Docker.
3. **Tomcat** (contenedor `backend`): recibe el request y se lo pasa a nuestra aplicación
   (`webapps/ROOT`).
4. **Cadena de filtros de Spring Security:** `JWTAuthorizationFilter` lee el header, verifica la
   firma y el vencimiento del token, arma el usuario `tms` con el rol `ROLE_TMS` y lo guarda en el
   `SecurityContextHolder`. La ruta no es pública, pero el usuario está autenticado: sigue.
5. **Spring MVC** busca el método: `@RequestMapping("/api/v1/ordenes")` +
   `@PostMapping("/{numeroOrden}/pesaje-inicial")` → `OrdenRestController.pesajeInicial`.
6. **`@PreAuthorize("hasAnyRole('ADMIN','TMS')")`**: `tms` tiene `ROLE_TMS`, se permite. (Si fuera
   `sap`, acá se cortaría con **403**.)
7. **Jackson** convierte el body en un `PesajeRequest` (`@RequestBody`) y Spring convierte `1001` en
   `Long numeroOrden` (`@PathVariable`).
8. **Controlador** → `ordenBusiness.registrarPesajeInicial(1001, 15000.0)`.
9. **Negocio:** valida peso > 0 → `load(1001)` (`OrdenRepository.findByNumeroOrden` → `SELECT` en
   MySQL) → `controlarEstado(orden, PENDIENTE_PESAJE_INICIAL)` → setea tara, fecha y contraseña →
   `cambiarEstado` (`UPDATE ordenes` + `INSERT INTO cambios_estado` con usuario `tms`).
10. **Hibernate** genera el SQL y lo ejecuta con una conexión del pool Hikari contra el contenedor
    `mysql`.
11. **Respuesta:** el controlador devuelve
    `new ResponseEntity<>(new PasswordResponse(1001, 84921), HttpStatus.OK)`; Jackson lo convierte
    en `{"numeroOrden":1001,"password":84921}` y vuelve por Tomcat y nginx al cliente.

Si en el paso 9 la orden estaba en otro estado, `controlarEstado` lanza `ConflictException`, el
`catch` del controlador la convierte en 409 y el cuerpo es un `StandartResponse` con el mensaje.

# 6. La teoría de la materia aplicada al TP

## 6.1. HTTP

**Clase:**

- **Cliente-servidor:** el servidor es pasivo (abre un puerto y espera); el cliente es activo (envía un
  *request* y recibe un *response*). Programar un backend es programar el servidor.
- **HTTP** es un protocolo de aplicación, de texto plano, **sin estado**: cada request-response es
  independiente. Se extiende con **cabeceras** (`nombre: valor`).
- Request: `MÉTODO PATH VERSIÓN`, cabeceras, una **línea en blanco obligatoria** y el body opcional.
  Response: `VERSIÓN CÓDIGO DESCRIPCIÓN`, cabeceras, línea en blanco, body.
- Métodos: GET (obtener), POST (crear/acción), PUT (modificar), DELETE (borrar).
- Códigos: 2xx éxito (200 OK, 201 Created), 3xx redirección (302 Found + `Location`), 4xx error del
  cliente (400, 401, 403, 404), 5xx error del servidor (500).
- `Content-Type` dice el formato del body (tipo MIME: `application/json`).
- **HTTPS** = HTTP sobre TLS (cifrado) sobre TCP sobre IP.
- Formatos para B2B: XML (muchos metadatos), **JSON** (el estándar actual), YAML (configuración).
- Puertos conocidos: 80 HTTP, 443 HTTPS, 22 SSH, 25 SMTP; abrir uno menor a 1024 requiere sudo.

**En el TP:** comunicación B2B por HTTP + JSON. Códigos usados: 200, 201 (+ `Location`), 302, 400,
401, 403, 404, 409, 500.

## 6.2. REST

**Clase:** REST es un conjunto de **restricciones**, no una tecnología:

| Restricción | Idea | En el TP |
|-------------|------|----------|
| Cliente-servidor | Responsabilidades separadas; frontend y backend evolucionan por separado | Backend sin interfaz; el frontend del final consumirá la misma API |
| **Sin estado** | Cada request trae todo lo necesario; el estado lo guarda el cliente (si lo guardara el servidor, la memoria crecería con cada cliente) | Token JWT en cada request, `STATELESS` |
| Caché | Cada respuesta indica si se puede cachear | Lo resuelve el framework |
| Contrato uniforme | La API es un contrato que respetan todos; servicios genéricos, no pensados para un cliente en particular | Endpoints documentados en Swagger |
| Sistema en capas | Puede haber intermediarios transparentes (proxy, caché) | nginx adelante del Tomcat |

- **Recurso** (la orden), identificado por una **URI**; **representación** (su estado en JSON).
- Buenas prácticas de URIs: **sustantivos** (el verbo lo da el método HTTP), plural, minúsculas,
  guion medio, **versión** (`/api/v1`), el formato en la cabecera `Accept` y no en la URL.
- **Hypermedia types:** combinar código y cabeceras para dar significado (201 + `Location`).

**En el TP:** `/api/v1/ordenes` (plural, versionado), `pesaje-inicial` con guion medio,
`/ordenes/{numeroOrden}/...` (subrecursos de una orden).

## 6.3. Docker

**Clase:**

- **Imagen** = plantilla (se define con un `Dockerfile` que empieza con `FROM`); **contenedor** = la
  imagen corriendo. Como clase y objeto.
- Diferencia con una VM: la VM tiene un sistema operativo completo; Docker comparte el kernel del
  host, es mucho más liviano.
- Comandos: `docker run` (crear + iniciar), `start`/`stop`/`restart`, `ps` (contenedores andando; son
  procesos), `exec -it <c> bash`, `logs --tail 100 -f` ("la herramienta más útil": los errores del
  backend están en el log).
- Parámetros de `run`: `-d` (segundo plano), `--name`, `--restart always`, `-e VAR=valor` (variables
  de entorno), `-p host:contenedor` (**izquierda el host, derecha el contenedor**), `-v` (volúmenes).
- **Volúmenes con nombre:** los datos sobreviven aunque se borre o actualice el contenedor (la base
  de MySQL).
- **Docker Compose:** un YAML con `services:` para manejar todos los contenedores juntos. El nombre del
  servicio es el nombre de host en la red interna. `ports` abre el puerto en la máquina (internet);
  `expose` solo lo comparte con los otros servicios. Los secretos van en un `.env` y se usan como
  `${VARIABLE}`.

**En el TP:** en la PC, MySQL corre con `docker run ... -p 33306:3306 ... mysql:8.0.43` (README). En
la VM, el compose del profesor (sección 6.5). `deploy.sh` compila con Maven dentro de un contenedor
temporal (`docker run --rm`).

## 6.4. Empaquetado: jar y war

**Clase:** **jar** autocontenido = aplicación + librerías + Tomcat embebido (`java -jar iw3.jar`);
**war** = todo menos el servidor; se deposita en la carpeta `webapps` de un Tomcat (`ROOT` atiende
`/`). Tomcat: `bin` (arranque), `conf` (`server.xml`, `catalina.properties`), `logs`
(`catalina.out`), `webapps`.

**En el TP:** en la VM se usa el **war** (`ROOT.war`) dentro del Tomcat del contenedor `backend`.
Por eso `Iw3Application extends SpringBootServletInitializer`.

## 6.5. La infraestructura de la VM

```
Internet --443--> [nginx] --/--> [backend: Tomcat 8080] ----> [mysql 3306]
                          \--/adminer--> [adminer 8080] -------^
```

| Servicio | Imagen / función | Puertos | Volúmenes |
|----------|------------------|---------|-----------|
| `nginx` | **Proxy inverso** y HTTPS | `443:443` (el único abierto a internet) | configuración y certificados |
| `backend` | Tomcat 10 + Java 21 (imagen del profesor) | `expose: 8080` | `webapps`, `catalina.properties` (perfil `mysqlprod`), `setenv.sh` (memoria, zona horaria) |
| `mysql` | MySQL 8 | ninguno hacia afuera | volumen con nombre para la base |
| `adminer` | Administrador web de MySQL | `expose: 8080` | |

- **Proxy inverso:** recibe todo por un único puerto y rutea según la URL a los servicios de atrás
  (`/` → backend, `/adminer` → adminer). Además maneja el certificado, así no hay que configurar SSL en
  cada aplicación.
- MySQL no está expuesto: solo lo ven los otros contenedores por la red interna (más seguro).
- `.env` con las contraseñas (incluida `USUARIOS_PASSWORD`, que el compose pasa al backend).
- `restart: always`: si la VM se reinicia, los contenedores vuelven a levantarse solos.
- `depends_on: mysql`: el backend arranca después de la base.

## 6.6. HTTPS y deploy

**Certificado (Let's Encrypt con certbot):** `sudo certbot certonly --standalone -d <dominio>`.
Certbot abre el puerto 80 y publica un token; Let's Encrypt se conecta al dominio, lo lee y, si
coincide, confirma que controlás el dominio y emite el certificado. Se usan `fullchain.pem`
(certificado + cadena de certificados que lo firman) y `privkey.pem` (clave privada: no compartir).
Dura 3 meses. Cifrado asimétrico: lo cifrado con la clave pública solo se descifra con la privada.

**`infra/deploy.sh`** (se copia a `~/infra/deploy.sh` en la VM):

1. Verifica que la infraestructura tenga el perfil `mysqlprod` (si no, la app se conectaría a la base
   equivocada).
2. `sudo -v`: pide la contraseña de sudo una vez al principio. Por eso se corre **en primer plano**
   (en segundo plano sudo no puede pedirla y el proceso queda "Stopped").
3. Clona la rama `main` del repo `TiziTur/ingweb3` en `/tmp/ingweb3`.
4. Compila con Maven **en un contenedor** (`maven:3.9.11-amazoncorretto-21`), con `~/.m2` montado
   para no bajar las dependencias cada vez: `mvn clean package -Dmaven.test.skip=true -Dbuild=war`.
5. Cambia permisos y dueño del `ROOT.war` (lo creó root dentro del contenedor).
6. `docker compose stop backend`, borra `webapps/ROOT`, copia el war como `ROOT.zip`, lo descomprime
   y lo borra (el Tomcat del profesor no despliega solo los war: es costoso en producción).
7. `docker compose up -d backend` y se mira el arranque con `docker logs -f backend`.

Cambios respecto al script del profesor: nuestro repo, y `cp` en vez de `mv` (con `mv` fallaba por
permisos porque `target` queda de root). Flujo de trabajo: **desarrollar → probar → commit y push →
`deploy.sh` en la VM**.

# 7. Decisiones y puntos débiles

## 7.1. Por qué se hizo así

| Decisión | Alternativa | Por qué |
|----------|-------------|---------|
| Partir del proyecto del profesor | Empezar de cero | Lo recomendó él y es el código visto en clase |
| Quitar `cli1`, `cli2`, `Product`, `Category` y sus `ScanConfig` | Dejarlos | Eran ejemplos de clase; el profesor pidió dejar solo dev y prod |
| La orden se identifica por `numeroOrden` en las rutas | Usar el `id` | Los sistemas externos solo conocen el número |
| `codigoExterno` `String` y `unique` en las entidades externas | Usar nuestro id | Lo pide la consigna; SAP no conoce nuestros ids |
| Un `*Business` por entidad externa con `sincronizar()` | Todo en `OrdenBusiness` | Sigue el patrón de clase (un negocio por entidad) |
| Sincronizar actualiza si existe | Ignorar los datos nuevos | La consigna dice que los datos "deben sincronizarse" |
| Punto 1 recibe la entidad `Orden` | Un DTO o un deserializador | Menos clases; el negocio limpia los campos internos |
| La contraseña la genera el backend | La genera el TMS | Diagrama de la consigna; el profe lo confirmó en el video |
| Estado como enum guardado como texto | Un número | Se lee mejor en el código y en la base |
| Excepciones nuevas para 400 y 409 | Usar 500 o 404 para todo | Cada error con su código |
| Datos descartados responden 400 | Responder 200 e ignorarlos | El sistema de carga se entera de que el dato no se usó y por qué |
| Conciliación y total de litros calculados | Guardarlos | "Lo calculable no se almacena": no pueden quedar desactualizados |
| Frecuencia medida por tiempo desde el último detalle | Contar datos recibidos | La consigna habla de "cada 10 segundos" |
| Roles por sistema ya en el parcial | Dejarlos para el final | Son pocas líneas y evitan que un sistema use endpoints de otro |
| Usuarios creados al arrancar | Insertarlos con SQL | Cualquiera levanta el proyecto con la base vacía |
| Contraseña de usuarios por variable de entorno | Escribirla en el código | No queda en el repo (es público) |
| DTOs y `@JsonIgnore` | Serializadores propios | Alcanzan y son más simples de explicar |

## 7.2. Lo que el profe podría señalar (con la respuesta)

| Posible pregunta | Respuesta |
|------------------|-----------|
| "La frecuencia tenía que poder definirse en la base de datos" (lo dijo en el video de la consigna) | Hoy está en `application.properties`: es configurable sin tocar código, pero requiere reiniciar. Pasarla a una tabla de configuración es la mejora pensada para el final. |
| "`activar` y `cerrar` son verbos en la URL" | No son operaciones CRUD sino transiciones de estado que pide la consigna; la definición de endpoints es a criterio del grupo. La alternativa más REST sería `/cierre` o `PUT /ordenes/{n}/estado`. |
| "En producción `ddl-auto` debería ser `validate`" | Lo dejamos en `update` para que la VM cree las tablas sola; en un sistema real se crearían con un script y se usaría `validate`. |
| "La clave del JWT está en el código" | Viene del proyecto base (`AuthConstants.SECRET`). Lo correcto es sacarla a una variable de entorno, como hicimos con las contraseñas de usuarios y de mail. |
| "¿Por qué 302 si la orden ya existe?" | Es el criterio del proyecto base para `FoundException` ("encontré algo que no esperaba"). |
| "¿Por qué el body del punto 1 es la entidad y no un DTO?" | Porque el JSON de SAP tiene la forma de la orden; para que no se cuelen datos internos, `recibirOrden` los pone en `null`. |
| "¿Qué pasa si dos datos de carga llegan al mismo tiempo?" | No lo contemplamos: los datos del caudalímetro llegan en secuencia (uno por segundo). Se podría agregar control de concurrencia con `@Version` (bloqueo optimista). |
| "¿Valida la densidad entre 0 y 1?" | No: la consigna solo pide descartar caudal ≤ 0, masa ≤ 0 y masa decreciente; la densidad es "anecdótica". |

# 8. Preguntas de práctica

Respuestas cortas, como las quiere el profesor. Donde dice **Dónde**, es lo que hay que abrir en
Eclipse.

## 8.1. "Buscame y explicame"

1. **¿Dónde se controla que la orden esté en el estado correcto?** Dónde: `OrdenBusiness.controlarEstado`.
   Compara el estado de la orden con el esperado; si no coincide lanza `ConflictException` y el
   controlador responde 409.
2. **¿Dónde se cambia el estado?** Dónde: `OrdenBusiness.cambiarEstado`. Pone el estado nuevo, guarda
   la orden y llama a `registrarCambioEstado`, que crea un `CambioEstado` con fecha, usuario y
   observación.
3. **¿De dónde sale el usuario de la auditoría?** Dónde: `OrdenBusiness.usuarioActual`. Del
   `SecurityContextHolder`, donde el filtro JWT dejó el usuario del token.
4. **¿Dónde se genera la contraseña?** Dónde: `OrdenBusiness.generarPassword`.
   `10000 + (int)(Math.random() * 90000)`: siempre entre 10000 y 99999.
5. **¿Dónde se descartan los datos de carga?** Dónde: `OrdenBusiness.recibirDatoCarga`. Los `if` que
   lanzan `BadRequestException`: faltan valores, caudal ≤ 0, masa ≤ 0, masa menor a la anterior.
6. **¿Dónde se decide si se guarda un detalle?** Dónde: `OrdenBusiness.correspondeGuardarDetalle`.
   Busca el último detalle (`findFirstByOrdenIdOrderByFechaDesc`); si no hay o pasaron al menos 10
   segundos, se guarda.
7. **¿Dónde se configuran esos 10 segundos?** Dónde: `application.properties`,
   `carga.detalle.frecuencia.segundos=10`, inyectado con `@Value` en `OrdenBusiness`.
8. **¿Dónde se calcula la conciliación?** Dónde: `OrdenBusiness.armarConciliacion`. Neto = final −
   tara; diferencia = neto − última masa; promedios con las `@Query` de `DetalleCargaRepository`.
9. **¿Dónde están los promedios?** Dónde: `DetalleCargaRepository`, `@Query("SELECT AVG(...) FROM
   DetalleCarga d WHERE d.orden.id = ?1")`.
10. **¿Dónde se sincroniza el camión?** Dónde: `CamionBusiness.sincronizar`. Busca por
    `codigoExterno`; si existe actualiza patente, descripción y cisternas; si no, lo crea.
11. **¿Dónde está la relación camión-cisternas?** Dónde: `Camion`, `@OneToMany(cascade = ALL,
    orphanRemoval = true, fetch = EAGER)` con `@JoinColumn(name = "id_camion")`.
12. **¿Dónde se ve que la contraseña no sale en el JSON?** Dónde: `Orden`, `@JsonIgnore` sobre
    `password`.
13. **¿Dónde se define qué rol usa cada endpoint?** Dónde: `OrdenRestController`, `@PreAuthorize` en
    cada método.
14. **¿Dónde se traduce una excepción a código HTTP?** Dónde: los `catch` de cada método de
    `OrdenRestController` y el método `error(...)`.
15. **¿Dónde se arma el 201 con `Location`?** Dónde: `OrdenRestController.recibirOrden`, con
    `HttpHeaders` y `HttpStatus.CREATED`.
16. **¿Dónde se crea el token?** Dónde: `AuthRestController.loginExternalOnlyToken`, con
    `JWT.create()...sign(...)`.
17. **¿Dónde se valida el token en cada request?** Dónde: `JWTAuthorizationFilter.doFilterInternal`
    y `getAuthentication` (`JWT.require(...).verify(token)`).
18. **¿Dónde se valida la contraseña en el login?** Dónde: `CustomAuthenticationManager.authenticate`,
    `pEncoder.matches(password, user.getPassword())`.
19. **¿Dónde se dice qué rutas son públicas?** Dónde: `SecurityConfiguration.filterChain`
    (`permitAll` en login, `/error`, Swagger, `/demo/**`; el resto `authenticated()`).
20. **¿Dónde se crean los usuarios?** Dónde: `DatosIniciales.run`.
21. **¿Dónde se elige la base de datos?** Dónde: `application.properties` y
    `application-mysqlprod.properties` (`spring.datasource.url`).
22. **¿Dónde se define la duración del token?** Dónde: `AuthConstants.EXPIRATION_TIME` (1 hora).
23. **¿Dónde está la versión de la API?** Dónde: `Constants` (`URL_API_VERSION = "/v1"`).
24. **¿Dónde se decide si se empaqueta como jar o war?** Dónde: `pom.xml`, perfiles `war-build` (por
    defecto) y `jar-build`.

## 8.2. Spring

25. **¿Qué es la inversión de control?** Que no instanciamos nosotros: Spring crea los objetos (beans)
    al arrancar y los inyecta donde ponemos `@Autowired`.
26. **¿Qué es un bean?** Un objeto creado y administrado por Spring (clases con `@Service`,
    `@RestController`, `@Repository`, `@Component`, o lo que devuelve un método `@Bean`).
27. **¿Por qué se inyecta `IOrdenBusiness` y no `OrdenBusiness`?** Para depender de la interfaz:
    Spring pone la implementación (`@Service`) y se puede cambiar sin tocar el controlador.
28. **¿Qué hace `@SpringBootApplication`?** Marca la clase principal y hace que Spring escanee el
    paquete buscando componentes y aplique la configuración automática.
29. **¿Qué es `CommandLineRunner`?** Una interfaz cuyo método `run` se ejecuta cuando Spring terminó
    de arrancar. La usan `Iw3Application` y `DatosIniciales`.
30. **¿Para qué `SpringBootServletInitializer`?** Para que la aplicación funcione como `.war` dentro
    de un Tomcat externo (la VM).
31. **¿Qué hace `@Value`?** Inyecta un valor de configuración; `${clave:defecto}` usa el valor por
    defecto si la clave no existe.
32. **¿Qué es un perfil?** Una configuración que se activa con `spring.profiles.active`;
    `application-<perfil>.properties` sobrescribe al principal como una herencia.
33. **¿Qué hace `@RestController`?** Marca la clase como controlador web; lo que devuelven sus
    métodos se convierte a JSON.
34. **¿Diferencia entre `@PathVariable`, `@RequestBody` y `@RequestParam`?** La primera toma un pedazo
    de la URL; la segunda, el JSON del body; la tercera, un parámetro de formulario o del *query string*.
35. **¿Qué es `ResponseEntity`?** El objeto que devuelve el controlador: cuerpo más código HTTP (y
    cabeceras si hacen falta).
36. **¿Qué hace Jackson?** Convierte objetos Java a JSON y JSON a objetos, usando getters y setters.
37. **¿Qué es Lombok?** Una librería que genera código repetitivo al compilar (getters, setters,
    constructores, builder, log) a partir de anotaciones.
38. **¿Qué es el patrón Builder?** Crear un objeto encadenando métodos y terminando con `build()`:
    `ConflictException.builder().message("...").build()`.
39. **¿Qué es Maven?** El manejador de dependencias y compilación del proyecto; se configura en el
    `pom.xml`.

## 8.3. Base de datos y JPA

40. **¿Qué es JPA y qué es Hibernate?** JPA es la especificación de persistencia de Java; Hibernate es
    la implementación que convierte objetos en tablas y escribe el SQL.
41. **¿Qué necesita una clase para ser tabla?** `@Entity` y un atributo con `@Id`.
42. **¿Qué es un repositorio?** Una interfaz que extiende `JpaRepository`; Spring la implementa y trae
    `findAll`, `save`, `findById`, etc. Es el DAO.
43. **¿Qué es un query method?** Un método del repositorio cuyo nombre describe la consulta
    (`findByNumeroOrden`) y Spring la genera.
44. **¿Qué es JPQL?** El lenguaje de consultas de JPA: como SQL pero sobre clases y atributos, no
    tablas.
45. **¿Qué hace `ddl-auto=update`?** Hibernate crea o modifica las tablas para que coincidan con las
    clases, sin borrar datos.
46. **¿Por qué `Double` y no `double` en la orden?** `Double` puede ser `null`: distingue "todavía no
    hay dato" de "el dato es 0".
47. **¿Qué es `@ManyToOne`?** Muchas filas de esta tabla apuntan a una de otra (muchas órdenes, un
    camión); crea la clave foránea.
48. **¿Qué hacen `cascade` y `orphanRemoval`?** `cascade` aplica a los hijos lo que se hace con el
    padre; `orphanRemoval` borra el hijo que sale de la lista.
49. **¿LAZY o EAGER?** LAZY carga los hijos cuando se piden (más eficiente); EAGER siempre (solo si se
    usan siempre, como las cisternas del camión).
50. **¿Por qué la clave principal es un autonumérico?** Es la más eficiente para el índice; la clave
    del negocio (`numeroOrden`, `codigoExterno`) queda como `unique`.
51. **¿Qué es el pool de conexiones?** Hikari abre conexiones a la base al arrancar y las reutiliza,
    porque abrir una conexión es lento.
52. **¿Por qué no se guarda la conciliación?** Se calcula con datos ya guardados; si se guardara,
    podría quedar desactualizada.

## 8.4. Seguridad

53. **¿Qué es un JWT?** Un token con tres partes en Base64 (encabezado, datos, firma). Lleva el
    usuario, los roles, el email y el vencimiento, firmado con una clave del servidor.
54. **¿Se puede leer un JWT? ¿Se puede falsificar?** Se puede leer (Base64 no es cifrado), pero no
    modificar: la firma dejaría de coincidir.
55. **¿Por qué el servidor no consulta la base en cada request?** Porque el usuario y los roles están
    en el token firmado; si la firma es válida, los datos son confiables.
56. **¿Cuánto dura el token y qué pasa cuando vence?** Una hora; después `verify` falla, el filtro no
    autentica y la respuesta es 401. Hay que volver a loguearse.
57. **¿Qué es un filtro?** Un componente por el que pasa el request antes de llegar al controlador;
    puede cortar o pasar al siguiente con `chain.doFilter`.
58. **¿Qué hace el filtro si no viene token?** Pasa al siguiente filtro; como la ruta requiere
    autenticación, Spring termina respondiendo 401.
59. **¿Diferencia entre 401 y 403?** 401: no sé quién sos. 403: sé quién sos pero tu rol no puede.
60. **¿Qué pasa si `sap` intenta registrar un pesaje?** `@PreAuthorize("hasAnyRole('ADMIN','TMS')")`
    no se cumple: 403 sin ejecutar el método.
61. **¿Cómo se guardan las contraseñas?** Con hash BCrypt (no reversible y con sal); se compara con
    `matches`.
62. **¿Por qué el login es público?** Porque si pidiera token, nadie podría conseguir uno.
63. **¿Qué significa `STATELESS`?** Que Spring no guarda sesiones en el servidor; cada request se
    autentica con su token (restricción sin estado de REST).
64. **¿Por qué está desactivado CSRF?** CSRF aprovecha las cookies de sesión del navegador; nuestra API
    no usa cookies sino un token en el header.
65. **¿Por qué permitieron `/error`?** Sin eso, un 403 se mostraba como 401: Spring redirigía a
    `/error` y esa ruta también pedía login.
66. **¿Cómo cambian la contraseña de los usuarios en la VM?** Con `USUARIOS_PASSWORD` en
    `~/infra/.env`; al reiniciar, `DatosIniciales` la actualiza.

## 8.5. HTTP, REST, Docker y deploy

67. **¿Qué es REST?** Un conjunto de restricciones: cliente-servidor, sin estado, caché, contrato
    uniforme y sistema en capas.
68. **¿Por qué 201 al crear la orden?** Significa "Created", es más preciso que 200; va con `Location`
    para que el cliente encuentre la orden si la necesita.
69. **¿Qué es un recurso?** Algo identificable por una URI (una orden); su representación es el JSON.
70. **¿Diferencia entre imagen y contenedor?** La imagen es la plantilla; el contenedor es la imagen
    ejecutándose.
71. **¿Por qué MySQL no tiene puerto abierto en la VM?** Para no exponer la base a internet; solo la ven
    los otros contenedores.
72. **¿Cómo sabe el backend de la VM a qué base conectarse?** Por el perfil `mysqlprod` (definido en
    `catalina.properties`); su URL usa el host `mysql`, el nombre del servicio del compose.
73. **¿Qué es un proxy inverso?** Un servidor HTTP que recibe todo por un solo puerto y rutea según la
    URL; además maneja el HTTPS.
74. **¿Cómo verifica Let's Encrypt que el dominio es tuyo?** Certbot publica un token en el puerto 80
    del dominio; Let's Encrypt lo lee y, si coincide, emite el certificado.
75. **¿Diferencia entre jar y war?** El jar trae el Tomcat adentro y se corre con `java -jar`; el war
    no, se deposita en un Tomcat.
76. **¿Cómo se despliega una versión nueva?** Commit y push a `main`, y en la VM `~/infra/deploy.sh`.
77. **¿Por qué `deploy.sh` compila dentro de Docker?** Para no instalar Maven en la VM y fijar la
    versión; `~/.m2` montado evita bajar las dependencias cada vez.

## 8.6. Del caso

78. **¿Qué pasa si llega un dato para una orden cerrada?** 409, porque `controlarEstado` exige estado 2.
79. **¿Por qué se descarta un dato con masa menor a la anterior?** La masa es acumulada: solo puede
    crecer; si baja, es un error de medición.
80. **¿Por qué la cabecera se actualiza con todos los datos y el detalle no?** La consigna pide tener
    siempre el último valor accesible, pero guardar todo el histórico ocuparía demasiado; se guarda uno
    cada 10 s.
81. **¿Qué es la tara?** El peso del camión vacío (punto 2). El neto por balanza es final − tara.
82. **¿Qué mide el caudalímetro másico?** Masa (kg) directamente; la última masa acumulada es el
    producto cargado.
83. **¿Qué hace la orden en el punto 3a?** Nada cambia: valida la contraseña y devuelve el preset para
    que el sistema de carga arranque la bomba.
84. **¿Qué significa sincronizar?** Si la entidad no existe se crea; si existe, se actualiza con los
    datos nuevos.
85. **¿Para qué el `codigoExterno`?** Para identificar camión, chofer, cliente y producto con el código
    del sistema externo; es texto para no depender de su formato.

# 9. La presentación

## 9.1. Guion (15 a 20 minutos)

| Minutos | Quién | Qué mostrar |
|---------|-------|-------------|
| 0-3 | Integrante 1 | El caso (SAP, TMS, carga), los cuatro estados y la arquitectura (capas y la VM con nginx y Docker) |
| 3-8 | Integrante 2 | Login con `sap`; punto 1 (201 y `Location`); punto 2 (la contraseña); punto 3: activación y datos, incluido uno descartado |
| 8-12 | Integrante 3 | Punto 4 y dato rechazado con 409; punto 5 y la conciliación; historial de estados (auditoría) |
| 12-15 | Integrante 4 | Errores esperados (401, 403, 404, 409), Swagger y la corrida completa del Runner en la VM |
| 15-20 | Todos | Preguntas |

## 9.2. Antes del martes

- Correr la colección completa en local y en la VM (con la contraseña cargada en el entorno `IW3-VM`).
- Tener el proyecto abierto en Eclipse en cada computadora (y el MySQL de Docker andando si se quiere
  correr la app).
- Practicar la sección 8.1 con el tiempo: encontrar y explicar en menos de 30 segundos.
