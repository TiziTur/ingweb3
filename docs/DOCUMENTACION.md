---
title: "Trabajo Integrador - Ingeniería Web 3"
subtitle: "Sistema de administración de órdenes de carga de gas líquido - Documentación del desarrollo"
date: "Última actualización: 20/10/2026"
lang: es
toc-title: "Índice"
---

# 1. Introducción

Este documento explica qué se hizo en el trabajo integrador, cómo funciona cada parte y por qué se
decidió hacerlo así. Está pensado para que cualquier integrante del grupo pueda entender el sistema
completo y defenderlo en la presentación.

El sistema es un backend que administra órdenes de carga de gas líquido. Una orden recorre cuatro
estados:

1. Llega desde un sistema externo (SAP) con los datos del camión, chofer, cliente, producto y la
   cantidad a cargar (preset).
2. La balanza (TMS) registra el peso del camión vacío (tara) y el backend genera una contraseña de
   activación.
3. El sistema de carga usa esa contraseña para obtener el preset, carga el gas y envía datos en tiempo
   real (masa, densidad, temperatura y caudal). Al terminar, cierra la orden.
4. La balanza registra el peso final y el backend calcula la conciliación entre lo que midió la
   balanza y lo que midió el caudalímetro.

## 1.1. Alcance actual

| Requisito | Estado |
|------------------------------------------------------------|---------|
| Puntos 1 a 5 implementados como API, sin interfaz gráfica | Hecho |
| Documentación OpenAPI (Swagger) | Hecho |
| Colección de Postman para probar todo, con tests del circuito completo | Hecho |
| Frecuencia de almacenamiento del detalle (opcional) | Hecho |
| Promedios en la conciliación (opcional) | Hecho |
| Roles de usuario (requisito del final) | Hecho |
| Log de auditoría de estados (recomendado en la consigna) | Hecho |
| Alarma de temperatura por mail (final) | Pendiente |
| Frontend (final) | Pendiente |

# 2. Tecnologías y organización

## 2.1. Tecnologías

- **Java 21** y **Spring Boot 4** (el mismo proyecto base que se usó en clase).
- **Spring Data JPA / Hibernate** para guardar los datos en **MySQL 8**. Las tablas se crean solas
  a partir de las clases (`spring.jpa.hibernate.ddl-auto=update`).
- **Spring Security** con **JWT** para el login y los roles (del proyecto base).
- **Lombok** para no escribir getters, setters y constructores a mano.
- **springdoc-openapi** para la documentación Swagger.
- **Postman** para las pruebas.
- En la VM, la aplicación corre como `.war` dentro de un **Tomcat** en Docker, detrás de **nginx** con
  HTTPS (infraestructura armada en el práctico anterior).

## 2.2. Capas

El código sigue las mismas capas que el proyecto del profesor:

| Capa | Paquete | Responsabilidad |
|------------|-------------------|------------------------------------------------------------|
| Controlador | `controllers` | Recibe la petición HTTP, llama al negocio y traduce el resultado o la excepción a un código HTTP. No tiene lógica. |
| Negocio | `business` | Las reglas: validaciones, estados, cálculos. Se define con una interfaz (`IOrdenBusiness`) y una implementación (`OrdenBusiness`). |
| Persistencia | `model.persistence` | Repositorios JPA: interfaces que Spring implementa solo. |
| Modelo | `model` | Entidades JPA: cada clase es una tabla. |
| DTO | `model.dto` | Clases para el cuerpo de las peticiones y respuestas que no son entidades. |

El flujo de una petición es: **controlador → negocio → repositorio → base de datos**, y la respuesta
vuelve por el mismo camino.

## 2.3. Carpetas del repositorio

| Carpeta | Contenido |
|------------------------|---------------------------------------------------|
| `iw3/` | Proyecto Spring Boot |
| `infra/deploy.sh` | Script de deploy para la VM |
| `postman/` | Colección y entornos de Postman |
| `docs/` | Esta documentación, la bitácora y los pendientes |
| `AGENTS.md`, `CLAUDE.md` | Reglas para las herramientas de IA que use el grupo |

# 3. Punto de partida

Se tomó el proyecto del profesor (`github.com/magm3333/iw3_2026`, commit "Backend completo"), como él
recomendó, y se quitó todo lo que era ejemplo de clase:

| Se quitó | Por qué |
|------------------------------------------------------------|------------------------------------------------------------|
| Paquetes `integration/cli1` y `integration/cli2`, perfiles `cli1` y `cli2` | El profesor indicó dejar solo `dev` y `prod` |
| Clases `config/profile/*ScanConfig` | Solo servían para separar las entidades de cli1 y cli2. Sin ellas, Spring escanea todo el paquete, que es lo que necesitamos |
| `Product`, `Category` y sus controladores y negocio | Eran el ejemplo de la materia, no forman parte del caso |
| `AuthtestRestController`, chat y gráfico por WebSocket, `Scheduler` de ejemplo | Demos de clase |

Se mantuvo:

- Login con JWT, usuarios y roles (paquete `auth`).
- Respuesta de error estándar (`StandartResponse`) y las excepciones del negocio.
- Envío de mails (`EmailBusiness`), configuración de eventos asíncronos y de WebSocket: se van a usar
  en el final para la alarma y el tiempo real.
- Swagger (`OpenApiConfig`).

# 4. Modelo de datos

## 4.1. Entidades que vienen de SAP

Todas tienen un `id` interno (lo genera la base) y un **`codigoExterno`** único, que es el
identificador con el que se relacionan con el sistema externo. Es un texto (`String`), como pide la
consigna, para no depender del formato del otro sistema.

| Entidad | Tabla | Campos | Opcionales |
|----------|-----------|------------------------------------------------------|-------------|
| `Camion` | `camiones` | `codigoExterno`, `patente`, `descripcion`, `cisternas` | `descripcion` |
| `Cisterna` | `cisternas` | `numero`, `capacidadLitros` | - |
| `Chofer` | `choferes` | `codigoExterno`, `nombre`, `apellido`, `documento` | `documento` |
| `Cliente` | `clientes` | `codigoExterno`, `razonSocial`, `contacto` | `contacto` |
| `Producto` | `productos` | `codigoExterno`, `nombre`, `descripcion` | `descripcion` |

**Cisternado.** Un camión tiene una lista de cisternas (relación uno a muchos). Cada cisterna guarda su
número y su capacidad. El total de litros no se guarda: se calcula sumando las cisternas
(`getTotalLitros()` en `Camion`), así nunca puede quedar desactualizado.

La relación usa `cascade = ALL` y `orphanRemoval = true`: al guardar el camión se guardan sus
cisternas, y si una cisterna se saca de la lista, se borra de la base.

## 4.2. Orden

`Orden` (tabla `ordenes`) es el centro del sistema.

| Grupo | Campos |
|---------------------------|------------------------------------------------------------|
| Identificación | `id` (interno), `numeroOrden` (único; es el código que conocen los otros sistemas) |
| Entidades asociadas | `camion`, `chofer`, `cliente`, `producto` (relaciones muchos a uno) |
| Datos de SAP | `fechaPrevistaCarga` (turno), `preset` (kg a cargar) |
| Estado | `estado` (ver sección 5) |
| Pesajes | `tara` (camión vacío), `pesajeFinal` (camión cargado) |
| Activación | `password` (5 dígitos) |
| Fechas | `fechaRecepcion`, `fechaPesajeInicial`, `fechaInicioCarga`, `fechaFinCarga`, `fechaPesajeFinal` |
| Últimos valores de la carga | `ultimaMasaAcumulada`, `ultimaDensidad`, `ultimaTemperatura`, `ultimoCaudal`, `fechaUltimoDato` |

Detalles a tener en cuenta:

- Los números se guardan como `Double` (con mayúscula) y no `double`, para que puedan valer `null`
  mientras todavía no hay dato (por ejemplo, la tara antes del punto 2).
- La `password` tiene `@JsonIgnore`: nunca aparece cuando se consulta una orden. Solo se entrega una
  vez, en la respuesta del pesaje inicial.
- `getNumeroEstado()` devuelve el número de estado (1 a 4) para mostrarlo junto al nombre.

## 4.3. DetalleCarga

`DetalleCarga` (tabla `detalles_carga`) guarda el histórico de la carga: `masaAcumulada`, `densidad`,
`temperatura`, `caudal` y `fecha`, más la orden a la que pertenece. No se guardan todos los datos que
llegan, sino según la frecuencia configurada (sección 6.4).

El campo `orden` tiene `@JsonIgnore` para que al listar detalles no se repita la orden completa en
cada uno.

## 4.4. CambioEstado (auditoría)

`CambioEstado` (tabla `cambios_estado`) registra cada cambio de estado de una orden: estado anterior,
estado nuevo, fecha, usuario que hizo la acción y observación. Es el "log de auditoría" que recomienda
la consigna. El usuario sale del token JWT de quien llamó al endpoint (por ejemplo `sap`, `tms`,
`carga`).

## 4.5. Relaciones

| Desde | Hacia | Tipo | Columna |
|--------------|-----------------------------------------|------------|-----------------------------------------------------|
| `Orden` | `Camion`, `Chofer`, `Cliente`, `Producto` | Muchos a uno | `id_camion`, `id_chofer`, `id_cliente`, `id_producto` |
| `Camion` | `Cisterna` | Uno a muchos | `id_camion` en `cisternas` |
| `DetalleCarga` | `Orden` | Muchos a uno | `id_orden` |
| `CambioEstado` | `Orden` | Muchos a uno | `id_orden` |

# 5. Estados de la orden

Los estados están en el enum `EstadoOrden`:

| Nº | Valor | Significado | Lo genera |
|----|--------------------------|--------------------------------------------------|---------|
| 1 | `PENDIENTE_PESAJE_INICIAL` | La orden llegó desde SAP | Punto 1 |
| 2 | `CON_PESAJE_INICIAL` | Tiene tara y contraseña; en este estado se carga | Punto 2 |
| 3 | `CERRADA` | Se terminó la carga, no se aceptan más datos | Punto 4 |
| 4 | `FINALIZADA` | Tiene pesaje final; se puede pedir la conciliación | Punto 5 |

En la base se guarda el nombre (`"CERRADA"`) y no el número, para que se entienda al mirar la tabla.

**Cómo se controla que los estados solo avancen.** En `OrdenBusiness` hay dos métodos que usan todos
los puntos:

- `controlarEstado(orden, esperado)`: si la orden no está en el estado que necesita la acción, lanza
  `ConflictException` y el controlador responde **409**. Por ejemplo, no se puede cerrar una orden
  que está en estado 1.
- `cambiarEstado(orden, nuevo, observacion)`: pone el estado nuevo, guarda la orden y registra el
  cambio en `CambioEstado`.

Como cada acción exige un estado puntual y siempre pasa al siguiente, una orden nunca puede volver
atrás. El estado nunca se cambia en otro lugar del código.

# 6. Funcionamiento de cada punto

Todas las rutas empiezan con `/api/v1/ordenes` y la orden se identifica por su `numeroOrden`
(el número que conocen los sistemas externos), no por el `id` interno.

## 6.1. Punto 1: recepción de la orden (SAP)

`POST /api/v1/ordenes` - rol `SAP` (o `ADMIN`)

```json
{
  "numeroOrden": 1001,
  "fechaPrevistaCarga": "2026-11-10T10:00:00-03:00",
  "preset": 3000,
  "camion": {
    "codigoExterno": "CAM-001",
    "patente": "AB123CD",
    "descripcion": "Camión cisterna",
    "cisternas": [
      { "numero": 1, "capacidadLitros": 10000 },
      { "numero": 2, "capacidadLitros": 5000 }
    ]
  },
  "chofer": {
    "codigoExterno": "CHO-001",
    "nombre": "Juan",
    "apellido": "Pérez",
    "documento": "30111222"
  },
  "cliente": {
    "codigoExterno": "CLI-001",
    "razonSocial": "Gas del Centro SA",
    "contacto": "compras@gasdelcentro.com"
  },
  "producto": {
    "codigoExterno": "PRO-001",
    "nombre": "Propano",
    "descripcion": "Gas licuado de petróleo"
  }
}
```

Qué hace `recibirOrden()`:

1. Controla que vengan `numeroOrden` y un `preset` mayor a 0. Si no, **400**.
2. Controla que no exista otra orden con el mismo número. Si existe, **302** (mismo criterio que el
   proyecto base para "ya existe").
3. **Sincroniza** camión, chofer, cliente y producto: cada `*Business` busca por `codigoExterno`; si
   existe, actualiza sus datos con lo que mandó SAP, y si no, lo crea. En el camión, las cisternas se
   reemplazan por las nuevas.
4. Limpia los datos internos (tara, password, fechas, últimos valores) por si vinieron en el JSON:
   esos datos no los define SAP.
5. Pone la fecha de recepción, el estado 1, guarda y registra el cambio de estado.

Respuesta: **201** con el header `location: /api/v1/ordenes/1001`.

## 6.2. Punto 2: pesaje inicial (TMS)

`POST /api/v1/ordenes/{numeroOrden}/pesaje-inicial` - rol `TMS`

```json
{ "peso": 15000 }
```

Qué hace `registrarPesajeInicial()`:

1. El peso tiene que ser mayor a 0 (si no, **400**) y la orden tiene que estar en estado 1 (si no, **409**).
2. Guarda la tara y la fecha de pesaje inicial.
3. Genera la contraseña de activación: un número entero de 5 dígitos, entre 10000 y 99999
   (`10000 + (int) (Math.random() * 90000)`).
4. Pasa la orden a estado 2.

Respuesta **200**:

```json
{ "numeroOrden": 1001, "password": 84921 }
```

Se decidió que el backend genere la contraseña y se la devuelva al TMS, porque el diagrama de la
consigna la ubica en el backend ("se genera la password de activación").

## 6.3. Punto 3: activación de la carga

`POST /api/v1/ordenes/{numeroOrden}/activar` - rol `CARGA`

```json
{ "password": 84921 }
```

`activarCarga()` controla que la orden exista (si no, **404**), que esté en estado 2 (si no, **409**)
y que la contraseña sea la correcta (si no, **400**). Si todo está bien, devuelve el preset para que el
sistema de carga arranque la bomba:

```json
{ "numeroOrden": 1001, "preset": 3000 }
```

La orden no cambia de estado: sigue en estado 2 mientras se carga.

## 6.4. Punto 3: datos de carga en tiempo real

`POST /api/v1/ordenes/{numeroOrden}/detalle` - rol `CARGA`

```json
{ "masaAcumulada": 1500, "densidad": 0.55, "temperatura": 18.5, "caudal": 3000 }
```

Qué hace `recibirDatoCarga()`:

1. La orden tiene que estar en estado 2. Si ya está cerrada, responde **409**: así se bloquea la
   recepción de datos después del punto 4.
2. **Descarta** el dato (responde **400** con el motivo) si:
   - falta alguno de los cuatro valores,
   - el caudal es menor o igual a 0,
   - la masa acumulada es menor o igual a 0,
   - la masa acumulada es menor que la anterior.
3. Si el dato es válido, actualiza **siempre** la cabecera de la orden con los últimos valores. El
   primer dato válido fija `fechaInicioCarga`, y cada dato válido actualiza `fechaFinCarga` (así, al
   cerrar, queda la fecha del último).
4. Guarda un registro de **detalle** solo si corresponde según la frecuencia.

**Frecuencia de almacenamiento.** La propiedad `carga.detalle.frecuencia.segundos` (por defecto 10)
indica cada cuánto se guarda un detalle. El método `correspondeGuardarDetalle()` busca el último
detalle guardado de la orden: si no hay ninguno, o si pasaron al menos esos segundos, se guarda.
Ejemplo de la consigna: si llegan datos cada 1 segundo y se guarda cada 10, en un minuto llegan 60 y
se guardan 6. Con el valor 0 se guardan todos.

## 6.5. Punto 4: cierre de la orden

`POST /api/v1/ordenes/{numeroOrden}/cerrar` - rol `CARGA`

`cerrarOrden()` controla que la orden esté en estado 2 y la pasa a estado 3. Desde ese momento
cualquier dato de detalle responde 409. Respuesta **200**.

## 6.6. Punto 5: pesaje final y conciliación

`POST /api/v1/ordenes/{numeroOrden}/pesaje-final` - rol `TMS`

```json
{ "peso": 18100 }
```

`registrarPesajeFinal()` controla que la orden esté en estado 3, guarda el pesaje final y su fecha,
pasa a estado 4 y devuelve la conciliación.

`GET /api/v1/ordenes/{numeroOrden}/conciliacion` - cualquier usuario logueado

Devuelve la conciliación en cualquier momento, pero solo si la orden está en estado 4 (si no, **409**).

```json
{
  "numeroOrden": 1001,
  "pesajeInicial": 15000.0,
  "pesajeFinal": 18100.0,
  "productoCargado": 3000.0,
  "netoPorBalanza": 3100.0,
  "diferenciaBalanzaCaudalimetro": 100.0,
  "promedioTemperatura": 18.5,
  "promedioDensidad": 0.55,
  "promedioCaudal": 3000.0
}
```

| Campo | Cálculo |
|-------------------------------|------------------------------------------------------------|
| `productoCargado` | Última masa acumulada recibida |
| `netoPorBalanza` | Pesaje final - tara |
| `diferenciaBalanzaCaudalimetro` | Neto por balanza - producto cargado |
| Promedios | `AVG` de los detalles guardados, con una consulta en `DetalleCargaRepository` |

La conciliación no se guarda en la base: se calcula cada vez que se pide (clase `Conciliacion` en
`model.dto`). Así no puede quedar desactualizada.

## 6.7. Consultas

| Ruta | Devuelve |
|--------------------------------------------|-------------------------------------------------------|
| `GET /api/v1/ordenes` | Todas las órdenes |
| `GET /api/v1/ordenes/{numeroOrden}` | Una orden con sus entidades asociadas y últimos valores |
| `GET /api/v1/ordenes/{numeroOrden}/detalles` | Los detalles guardados |
| `GET /api/v1/ordenes/{numeroOrden}/estados` | El historial de cambios de estado |

Las cuatro son para el rol `OPERADOR` (y `ADMIN`); el frontend del final las va a usar.

# 7. Manejo de errores

El negocio lanza excepciones y el controlador las traduce a códigos HTTP con `try/catch`, igual que
en el proyecto base. El cuerpo de la respuesta de error es un `StandartResponse` con `message`, `code`
y, en desarrollo, `devInfo` con la traza (en producción no se muestra: `dev.info.enabled=false`).

| Excepción | Código | Cuándo |
|---------------------|------|------------------------------------------------------------|
| `NotFoundException` | 404 | La orden no existe |
| `FoundException` | 302 | Ya existe una orden con ese número |
| `BadRequestException` | 400 | Faltan datos, peso inválido, contraseña incorrecta, dato de carga descartado |
| `ConflictException` | 409 | La orden no está en el estado que requiere la acción |
| `BusinessException` | 500 | Error inesperado (por ejemplo, la base no responde) |

`BadRequestException` y `ConflictException` son nuevas y copian el formato de las del proyecto base.
El paquete se llama `business.execption` (con ese error de tipeo) porque así viene del proyecto del
profesor; no se renombró para no tocar código que ya funcionaba.

Además, Spring Security responde **401** si no se manda token (o es inválido) y **403** si el usuario
no tiene el rol necesario.

# 8. Seguridad

## 8.1. Login con JWT (del proyecto base)

1. El cliente hace `POST /api/v1/login` con `username` y `password` (formulario).
2. `CustomAuthenticationManager` busca el usuario y compara la contraseña con BCrypt.
3. Si es correcta, `AuthRestController` arma un token JWT firmado que incluye el usuario y sus roles,
   válido por 1 hora.
4. En cada petición, el cliente manda `Authorization: Bearer <token>`. El filtro
   `JWTAuthorizationFilter` verifica la firma y carga el usuario con sus roles.

## 8.2. Roles

Cada sistema externo tiene su usuario y su rol, para que solo pueda usar los endpoints que le
corresponden. Esto es requisito del final, pero se hizo desde el principio porque son pocas líneas.

| Rol | Usuario | Endpoints |
|---------------|----------|-------------------------------------------------------|
| `ROLE_SAP` | `sap` | Punto 1 |
| `ROLE_TMS` | `tms` | Puntos 2 y 5 (pesajes) |
| `ROLE_CARGA` | `carga` | Puntos 3 y 4 (activar, detalle, cerrar) |
| `ROLE_OPERADOR` | `operador` | Consultas y conciliación (será el usuario del frontend) |
| `ROLE_ADMIN` | `admin` | Todo |

En el controlador cada método tiene `@PreAuthorize`, por ejemplo
`@PreAuthorize("hasAnyRole('ADMIN','TMS')")`. Spring agrega el prefijo `ROLE_` al comparar.

## 8.3. Usuarios iniciales

La clase `auth/DatosIniciales` se ejecuta al arrancar (implementa `CommandLineRunner`) y crea los
roles y los cinco usuarios si todavía no existen. Así cualquiera puede levantar el proyecto con la
base vacía. La contraseña sale de `usuarios.password.inicial` (en desarrollo `123456`; en la VM se
cambia con la variable de entorno `USUARIOS_PASSWORD`).

## 8.4. Ajuste en SecurityConfiguration

Se agregó `.requestMatchers("/error").permitAll()`. Sin esto, cuando un usuario sin permiso llamaba a
un endpoint, Spring redirigía internamente a `/error`, esa ruta también pedía autenticación, y la
respuesta terminaba siendo 401 en lugar de 403.

# 9. Configuración

## 9.1. Perfiles

| Perfil | Dónde se usa | Base de datos |
|-----------|---------------------------------------------------------|----------------------------------------|
| `mysqldev` | En la PC de cada integrante (es el que queda por defecto) | `localhost:33306`, base `iw3_2026` |
| `mysqlprod` | En la VM | contenedor `mysql`, base `iw3_prod_2026` |

En `application.properties` está `spring.profiles.active=mysqldev`. En la VM, la infraestructura de
Tomcat define `spring.profiles.active=mysqlprod` (en `~/infra/tomcat/conf/catalina.properties`), y ese
valor tiene prioridad sobre el del archivo. El script de deploy verifica que esté definido antes de
compilar.

## 9.2. Propiedades propias

| Propiedad | Valor por defecto | Para qué |
|-----------------------------------|--------------------------------|-----------------------------------------|
| `carga.detalle.frecuencia.segundos` | 10 | Cada cuánto se guarda un detalle de carga |
| `usuarios.password.inicial` | `123456` (o `USUARIOS_PASSWORD`) | Contraseña de los usuarios iniciales |
| `spring.mail.username` / `password` | `SMTP_USER` / `SMTP_PASS` | Cuenta de mail para la alarma (final) |

Las contraseñas de mail no están en el repo: se pasan por variables de entorno.

# 10. Documentación OpenAPI (Swagger)

Con la aplicación corriendo, la documentación está en `/swagger-ui.html` (y en JSON en
`/v3/api-docs`). Cada endpoint de `OrdenRestController` tiene:

- `@Operation`: resumen y descripción de qué hace.
- `@ApiResponses`: los códigos que puede devolver y qué significa cada uno.
- `@Tag` en la clase: agrupa todo bajo "Órdenes".

Para probar desde Swagger: hacer login en `/api/v1/login`, copiar el token y pegarlo en el botón
**Authorize**.

# 11. Pruebas con Postman

La colección está en `postman/IW3-Integrador.postman_collection.json`, con dos entornos:
`IW3-Local` (`http://localhost:8080`) e `IW3-VM` (la URL con HTTPS del dominio).

| Carpeta | Contenido |
|---------------------|------------------------------------------------------------|
| 0 - Login | Login de los cinco usuarios. Cada uno guarda su token en una variable (`tokenSap`, `tokenTms`...) con un script de "Tests" |
| 1 - Circuito completo | Una orden desde que llega de SAP hasta la conciliación, incluyendo un dato descartado y un dato rechazado por orden cerrada |
| 2 - Errores esperados | 302, 404, 409, 400, 403 y 401 |
| 3 - Consultas | Listado y detalle de órdenes |

Cada request tiene un test que verifica el código de respuesta (y algunos, valores concretos como el
preset o el neto por balanza). El request del punto 1 genera un número de orden nuevo en cada
ejecución, y el del punto 2 guarda la contraseña generada en una variable. Por eso la colección
completa se puede correr con el **Runner** las veces que se quiera.

Resultado de la última corrida: 29 requests, 33 tests, 0 fallas.

# 12. Deploy en la VM

El script `infra/deploy.sh` se copia a `~/infra/deploy.sh` en la VM. Hace lo mismo que el script del
profesor:

1. Verifica que la infraestructura defina el perfil `mysqlprod`.
2. Clona la rama `main` del repo del grupo en `/tmp/ingweb3`.
3. Compila el `.war` con Maven dentro de un contenedor Docker (no hace falta Maven en la VM).
4. Detiene el backend, reemplaza el contenido de `~/infra/tomcat/webapps/ROOT` y lo vuelve a levantar.

Diferencias con el script original: clona nuestro repo y copia el `.war` con `cp` en lugar de `mv`.
Con `mv` fallaba por permisos: Maven corre como `root` dentro del contenedor y la carpeta `target`
queda de `root`.

# 13. Decisiones tomadas

| Decisión | Alternativa descartada | Motivo |
|---------------------------------------------------------|---------------------------------|------------------------------------------------------------|
| Partir del proyecto del profesor | Proyecto nuevo desde cero | Lo recomendó el profesor y es el código que ya conocemos de clase |
| Identificar la orden por `numeroOrden` en las rutas | Usar el `id` interno | Los sistemas externos solo conocen el número de orden |
| Recibir el punto 1 con la entidad `Orden` directamente | Un DTO aparte o un deserializador | Menos clases; el negocio limpia los campos internos. Es lo que hace el proyecto base con `Product` |
| Un `*Business` por entidad externa con `sincronizar()` | Una sola clase para todas | Sigue el patrón de clase (una clase de negocio por entidad) |
| Actualizar las entidades externas si ya existen | Ignorar los datos nuevos | La consigna dice que los datos "deben sincronizarse" |
| La password la genera el backend | La genera el TMS | El diagrama de la consigna la ubica en el backend. Pendiente de confirmar |
| Estado como enum guardado como texto | Un número entero | Se lee mejor en el código y en la base |
| Excepciones nuevas para 400 y 409 | Usar 500 o 404 para todo | Cada error tiene un código que dice qué pasó |
| Datos descartados responden 400 | Responder 200 e ignorarlos | El sistema de carga se entera de que el dato no se usó |
| Roles por sistema desde el parcial | Dejarlos para el final | Son pocas líneas y evitan que un sistema use endpoints de otro |
| Usuarios creados al arrancar | Insertarlos a mano con SQL | Cualquiera levanta el proyecto con la base vacía y ya puede probar |
| Conciliación calculada, no guardada | Guardarla en una tabla | No puede quedar desactualizada |
| Total de litros calculado, no guardado | Guardarlo en el camión | Ídem |
| Frecuencia de detalle por tiempo desde el último guardado | Contar datos recibidos | Es lo que describe la consigna ("cada 10 segundos") |

# 14. Preguntas para practicar la defensa

**¿Qué pasa si llega un dato de carga para una orden cerrada?**
`recibirDatoCarga()` llama a `controlarEstado(orden, CON_PESAJE_INICIAL)`. Como la orden está en
`CERRADA`, lanza `ConflictException` y el controlador responde 409. Nada se guarda.

**¿Cómo se evita que una orden vuelva a un estado anterior?**
Cada acción exige un estado puntual (`controlarEstado`) y pasa al siguiente (`cambiarEstado`). No hay
ningún método que cambie el estado de otra forma.

**¿Dónde se ve quién cambió el estado?**
En la tabla `cambios_estado` o en `GET /ordenes/{numeroOrden}/estados`. El usuario sale del token JWT
con `SecurityContextHolder`.

**Si llegan datos cada segundo, ¿cuántos se guardan?**
La cabecera de la orden se actualiza con todos los datos válidos. En `detalles_carga` se guarda uno
cada `carga.detalle.frecuencia.segundos` (10 por defecto).

**¿Cómo se calcula el promedio de temperatura?**
Con una consulta JPQL en `DetalleCargaRepository`:
`SELECT AVG(d.temperatura) FROM DetalleCarga d WHERE d.orden.id = ?1`. Solo usa los detalles guardados.

**¿Qué pasa si SAP manda un camión que ya existe con otra patente?**
`CamionBusiness.sincronizar()` lo encuentra por `codigoExterno`, actualiza la patente y la descripción,
y reemplaza las cisternas.

**¿Por qué la password no aparece al consultar la orden?**
Porque el campo tiene `@JsonIgnore`. Solo se devuelve una vez, en la respuesta del pesaje inicial.

**¿Qué pasa si el usuario `sap` intenta registrar un pesaje?**
El endpoint tiene `@PreAuthorize("hasAnyRole('ADMIN','TMS')")`; `sap` no tiene ese rol, así que Spring
responde 403 sin llegar a ejecutar el método.

**¿Por qué los pesos son `Double` y no `double`?**
Porque `Double` puede ser `null`, y así se distingue "todavía no hay dato" de "el dato es 0".

**¿Cómo se genera la password?**
`10000 + (int) (Math.random() * 90000)`: `Math.random()` da un número entre 0 y 1 (sin incluir el 1),
multiplicado por 90000 da entre 0 y 89999, y sumando 10000 queda entre 10000 y 99999.

# 15. Registro de cambios

El detalle de cada cambio está en `docs/BITACORA.md`. Resumen:

| Fecha | Cambio |
|----------|------------------------------------------------------------|
| 20/10/2026 | Proyecto base limpio, modelo de datos, puntos 1 a 5, roles, auditoría, Swagger, Postman, deploy y documentación |
