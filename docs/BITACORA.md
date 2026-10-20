# Bitácora

Registro cronológico de lo que se hizo. La entrada más nueva va **arriba**.
Formato: fecha, quién (persona o "IA a pedido de X"), qué se hizo, archivos principales y cómo se probó.

---

## 2026-10-20 - Contraseña de usuarios por variable de entorno y login 401 (IA a pedido de Tiziano)

**Qué se hizo**

- `DatosIniciales`: si un usuario ya existe y su contraseña no coincide con
  `usuarios.password.inicial`, se la actualiza al arrancar. Así se puede cambiar la contraseña de la
  VM con la variable de entorno `USUARIOS_PASSWORD` sin tocar la base.
- `AuthRestController`: se quitó `produces = TEXT_PLAIN` del login. Con eso, una contraseña
  incorrecta respondía 500 (el error en JSON no se podía convertir a texto); ahora responde 401.
- Postman: request nuevo "Login con contraseña incorrecta -> 401". En el entorno `IW3-VM` se vació
  `passwordUsuarios` para no subir la contraseña de la VM al repo.
- Documentación: README, AGENTS.md, DOCUMENTACION.md (8.3, 8.4, 9.2, 11, 13) y Word regenerado.

**Cómo se probó:** en local, con `USUARIOS_PASSWORD=PruebaCambio99` se actualizaron los cinco
usuarios (login con la nueva: 200, con `123456`: rechazado); sin la variable volvieron a `123456`.
Colección de Postman: 30 requests, 34 tests, 0 fallas.

**Para aplicarlo en la VM:** agregar `USUARIOS_PASSWORD=...` a `~/infra/.env`, agregar
`- USUARIOS_PASSWORD=${USUARIOS_PASSWORD}` en `environment` del servicio `backend` de
`~/infra/docker-compose.yml`, y correr `~/infra/deploy.sh`.

**Aplicado en la VM (20/10/2026 18:04):** se agregó la variable a `~/infra/.env` y a
`~/infra/docker-compose.yml` (servicio `backend`) y se redesplegó. El log mostró
"Actualizando contraseña del usuario ..." para los cinco usuarios y `Perfil activo mysqlprod`.
La contraseña nueva la tiene el grupo por privado; no está en el repo.

---

## 2026-10-20 - Primer deploy en la VM (Tiziano, con ayuda de IA)

**Qué se hizo:** se reemplazó `~/infra/deploy.sh` de la VM por `infra/deploy.sh` del repo (el del
profesor quedó guardado como `~/infra/deploy.sh.profesor`) y se ejecutó. Compiló (`BUILD SUCCESS`),
reemplazó la aplicación en Tomcat y el backend arrancó con perfil `mysqlprod`, creando los cinco
usuarios en la base de la VM.

**Cómo se probó:** login con `admin` y `GET /api/v1/ordenes` por HTTPS
(`https://iw3turina.duckdns.org`) devolvió 200. Después se corrió la colección completa de Postman con el entorno
"IW3 - VM": 33 tests, 0 errores.

**Ojo:** desde este deploy, `https://<dominio>/api/v1/products` (URL del TP2) ya no existe.

---

## 2026-10-20 - Deploy: pedir la contraseña de sudo al principio (IA a pedido de Tiziano)

**Qué pasó:** al correr `deploy.sh` en segundo plano (`nohup ./deploy.sh &`) el proceso quedó
`Stopped` justo después de "Clonando": el `sudo rm -rf` pedía la contraseña y un proceso en segundo
plano no puede leerla de la terminal.

**Qué se hizo:** `infra/deploy.sh` ahora ejecuta `sudo -v` al principio (pide la contraseña una sola
vez) y se documentó que hay que correrlo en primer plano. Archivos: `infra/deploy.sh`, `README.md`,
`docs/DOCUMENTACION.md`, Word regenerado.

---

## 2026-10-20 - Arranque del proyecto (IA a pedido de Tiziano)

**Qué se hizo**

- Se copió el proyecto base del profesor (`magm3333/iw3_2026`, commit "Backend completo") en `iw3/`.
- Se quitaron los ejemplos de clase, como pidió el profesor: paquetes `integration/cli1` y
  `integration/cli2`, perfiles `cli1` y `cli2` (en `pom.xml` y `.properties`), las clases
  `config/profile/*ScanConfig`, `Product`, `Category`, `AuthtestRestController`, el chat y el
  gráfico de WebSocket y el `Scheduler` de ejemplo. Quedan los perfiles `mysqldev` y `mysqlprod`.
- `application.properties`: perfil por defecto `mysqldev`, contraseña de mail por variable de
  entorno (`SMTP_USER` / `SMTP_PASS`) y dos propiedades nuevas
  (`carga.detalle.frecuencia.segundos` y `usuarios.password.inicial`).
- Modelo: `Camion`, `Cisterna`, `Chofer`, `Cliente`, `Producto`, `Orden`, `EstadoOrden`,
  `DetalleCarga`, `CambioEstado`, y sus repositorios.
- Negocio: un `*Business` por entidad externa (sincroniza por `codigoExterno`) y `OrdenBusiness`
  con los puntos 1 a 5, el control de estados y la auditoría.
- Excepciones nuevas: `BadRequestException` (400) y `ConflictException` (409).
- `OrdenRestController` con todos los endpoints, anotaciones de Swagger y `@PreAuthorize` por rol.
- `DatosIniciales`: crea roles y usuarios (`admin`, `sap`, `tms`, `carga`, `operador`) al arrancar.
- `SecurityConfiguration`: se permitió `/error` para que un 403 no se muestre como 401.
- `infra/deploy.sh` adaptado al repo del grupo (clona `TiziTur/ingweb3` y copia el `.war` con `cp`).
- Colección de Postman (`postman/`) con login, circuito completo, errores esperados y consultas.
- Documentación: README, AGENTS.md, CLAUDE.md, `docs/DOCUMENTACION.md`, `docs/PENDIENTES.md`,
  esta bitácora y el Word generado.

**Cómo se probó**

- Compilación como `.jar` y como `.war` (el `.war` es el que usa el deploy).
- App levantada con perfil `mysqldev` contra MySQL 8 local.
- Colección de Postman corrida con newman: 29 requests, 33 tests, 0 fallas.
- Prueba manual de la frecuencia de guardado: 4 datos válidos en 11 segundos → se guardaron 2
  detalles (el primero y el que llegó pasados los 10 segundos). Un dato con masa menor a la
  anterior devolvió 400.
- Prueba de sincronización: una segunda orden con el mismo `codigoExterno` de camión y otra
  patente actualizó el camión y reemplazó sus cisternas.
- Swagger UI accesible en `/swagger-ui.html`.

**Pendiente de esta etapa:** probar el deploy en la VM.
