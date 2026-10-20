# Trabajo Integrador - Ingeniería Web 3 (IUA, 2026)

Backend para administrar órdenes de carga de gas líquido: recibe la orden desde un sistema
externo (SAP), registra los pesajes de la balanza (TMS), recibe los datos de la carga en tiempo
real y calcula la conciliación final.

> **Si sos una IA trabajando en este repo:** antes de tocar nada leé [AGENTS.md](AGENTS.md).

## Estado actual

| Parte | Estado |
|---|---|
| Puntos 1 a 5 (API sin interfaz) | Hecho |
| Roles por sistema (SAP, TMS, CARGA, OPERADOR, ADMIN) | Hecho |
| Auditoría de cambios de estado | Hecho |
| Frecuencia de guardado del detalle y promedios | Hecho |
| Documentación OpenAPI (Swagger) | Hecho |
| Colección de Postman con el circuito completo | Hecho |
| Alarma de temperatura por mail (final) | Pendiente |
| Frontend (final) | Pendiente |

El detalle de lo que falta está en [docs/PENDIENTES.md](docs/PENDIENTES.md).

## Documentación

| Archivo | Para qué sirve |
|---|---|
| [docs/DOCUMENTACION.md](docs/DOCUMENTACION.md) | Documento completo: qué hace cada parte y por qué se hizo así. Es la fuente del Word. |
| [docs/Documentacion_TP_Integrador.docx](docs/Documentacion_TP_Integrador.docx) | El mismo documento en Word. |
| [docs/BITACORA.md](docs/BITACORA.md) | Registro cronológico de lo que se fue haciendo. |
| [docs/PENDIENTES.md](docs/PENDIENTES.md) | Lo que falta hacer y las dudas para consultar al profesor. |
| [AGENTS.md](AGENTS.md) | Reglas para las IAs que trabajen en el proyecto. |

## Estructura del repo

```
iw3/                 Proyecto Spring Boot (backend)
infra/deploy.sh      Script de deploy para la VM
postman/             Colección y entornos de Postman
docs/                Documentación
```

## Entorno de desarrollo (cada integrante)

Hace falta:

- **Java 21** y un IDE (Eclipse/STS, IntelliJ o VS Code).
- **MySQL 8** escuchando en el puerto **33306**, con un usuario `iw3` / contraseña `iw3pass`.
  La base `iw3_2026` y las tablas se crean solas al arrancar.
  La forma más simple es con Docker:

  ```bash
  docker run -d --name mysql-iw3 -p 33306:3306 \
    -e MYSQL_ROOT_PASSWORD=root -e MYSQL_USER=iw3 -e MYSQL_PASSWORD=iw3pass \
    -e MYSQL_DATABASE=iw3_2026 mysql:8.0.43
  ```

- **Postman**.

## Cómo correrlo

Desde la carpeta `iw3/`:

```bash
./mvnw spring-boot:run          # Linux / Mac
mvnw.cmd spring-boot:run        # Windows
```

O desde el IDE, ejecutando `Iw3Application`. Por defecto arranca con el perfil `mysqldev`.

- API: `http://localhost:8080/api/v1/...`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Usuarios

Se crean solos la primera vez que arranca (clase `DatosIniciales`). La contraseña de todos es
`123456` en la PC. En la VM es otra, definida en la variable de entorno `USUARIOS_PASSWORD`
(archivo `~/infra/.env` de la VM, que no está en el repo); pedísela al grupo.

| Usuario | Rol | Qué puede hacer |
|---|---|---|
| `admin` | ROLE_ADMIN | Todo |
| `sap` | ROLE_SAP | Punto 1: enviar órdenes |
| `tms` | ROLE_TMS | Puntos 2 y 5: pesajes |
| `carga` | ROLE_CARGA | Puntos 3 y 4: activar, enviar datos, cerrar |
| `operador` | ROLE_OPERADOR | Consultar órdenes, detalles, estados y conciliación |

Login: `POST /api/v1/login` con `username` y `password` (form-urlencoded). Devuelve el token JWT,
que se manda en el header `Authorization: Bearer <token>`.

## Endpoints

Todos empiezan con `/api/v1/ordenes`. La orden se identifica por su `numeroOrden`.

| Punto | Método y ruta | Rol | Estado |
|---|---|---|---|
| 1 | `POST /ordenes` | SAP | → 1 |
| 2 | `POST /ordenes/{numeroOrden}/pesaje-inicial` | TMS | 1 → 2 |
| 3 | `POST /ordenes/{numeroOrden}/activar` | CARGA | 2 |
| 3 | `POST /ordenes/{numeroOrden}/detalle` | CARGA | 2 |
| 4 | `POST /ordenes/{numeroOrden}/cerrar` | CARGA | 2 → 3 |
| 5 | `POST /ordenes/{numeroOrden}/pesaje-final` | TMS | 3 → 4 |
| 5 | `GET /ordenes/{numeroOrden}/conciliacion` | cualquiera logueado | 4 |
| - | `GET /ordenes`, `GET /ordenes/{numeroOrden}` | OPERADOR | - |
| - | `GET /ordenes/{numeroOrden}/detalles`, `GET /ordenes/{numeroOrden}/estados` | OPERADOR | - |

`ADMIN` puede usar todos. Los ejemplos de cada JSON están en la colección de Postman y en
[docs/DOCUMENTACION.md](docs/DOCUMENTACION.md).

## Probar con Postman

1. Importar `postman/IW3-Integrador.postman_collection.json` y `postman/IW3-Local.postman_environment.json`.
2. Elegir el entorno **IW3 - Local**. Para probar la VM se usa **IW3 - VM** (importar también
   `postman/IW3-VM.postman_environment.json`) y hay que completar `passwordUsuarios` con la contraseña
   de la VM en la pestaña Environments, columna "Current value".
3. Correr la colección completa con el **Runner** (en orden). Primero hace login con todos los usuarios,
   después recorre una orden completa y al final prueba los errores esperados.

## Deploy en la VM

1. Copiar `infra/deploy.sh` a `~/infra/deploy.sh` en la VM y darle permisos (`chmod +x`).
2. Ejecutar `~/infra/deploy.sh` **en primer plano** (sin `&`): al principio pide la contraseña de sudo de la VM.

El script clona este repo, compila el `.war` y reemplaza la aplicación en Tomcat. Más detalle en
[docs/DOCUMENTACION.md](docs/DOCUMENTACION.md), sección "Deploy".
