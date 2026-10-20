# Instrucciones para IAs que trabajen en este repo

Este archivo es para cualquier asistente de IA (Claude, ChatGPT, Copilot, Cursor, Gemini, etc.)
que un integrante del grupo use para seguir el proyecto. Leelo completo antes de cambiar algo.

## 1. Contexto

- Trabajo integrador de **Ingeniería Web 3** (IUA, 2026). Grupo de 4 integrantes.
- Backend Spring Boot 4 / Java 21 / MySQL que administra **órdenes de carga de gas líquido**.
- Partimos del proyecto base del profesor (`github.com/magm3333/iw3_2026`, carpeta `iw3`),
  quitándole los ejemplos de clase (perfiles cli1/cli2, Product, Category, demos).
- **La defensa es oral y el profesor pregunta por el código.** Cada integrante tiene que poder
  explicar qué hace cada línea y por qué está así. Por eso la regla más importante es:

> **Simple y explicable antes que ingenioso.** Si hay dos formas de hacer algo, se elige la que
> se entiende más rápido leyendo el código, aunque sea un poco más larga.

## 2. Antes de hacer un cambio

1. Leé `docs/DOCUMENTACION.md` (cómo funciona todo y por qué) y `docs/PENDIENTES.md` (qué falta).
2. Mirá las últimas entradas de `docs/BITACORA.md` para saber en qué quedó el trabajo.
3. Si el cambio contradice una decisión de `docs/DOCUMENTACION.md` (sección "Decisiones"),
   **no la cambies por tu cuenta**: avisale al humano y que el grupo decida.

## 3. Reglas del código

- **Seguir los patrones del proyecto base**, que son los que se vieron en clase:
  - Entidad JPA en `model/` + repositorio en `model/persistence/`.
  - Negocio con interfaz + implementación en `business/` (`IOrdenBusiness` / `OrdenBusiness`).
  - El negocio lanza excepciones (`NotFoundException`, `FoundException`, `BadRequestException`,
    `ConflictException`, `BusinessException`) y el controlador las traduce a códigos HTTP
    con `try/catch`, devolviendo `StandartResponse` para los errores.
  - Clases de entrada/salida que no son entidades van en `model/dto/`.
  - Lombok para getters/setters/constructores.
- **No agregar librerías, frameworks ni patrones nuevos** (MapStruct, Spring Events complejos,
  programación reactiva, etc.) sin que el grupo lo apruebe.
- **No renombrar** el paquete `business.execption` (viene así, con ese error de tipeo, del proyecto
  del profesor; renombrarlo rompe todo y no aporta nada).
- **Los cambios de estado de una orden** se hacen solamente con `cambiarEstado()` de `OrdenBusiness`,
  después de `controlarEstado()`. Nunca hacer `orden.setEstado(...)` en otro lado: se perdería el
  registro de auditoría y el control de que los estados solo avanzan.
- **No cambiar rutas de endpoints, nombres de campos JSON ni roles** sin actualizar a la vez la
  colección de Postman, el README y `docs/DOCUMENTACION.md`. Otros integrantes y la colección
  dependen de esos nombres.
- **Comentarios en español**, cortos, explicando el *por qué* cuando no es obvio.
- **Nada de secretos en el repo**: contraseñas de mail, tokens, etc. van por variables de entorno
  (`SMTP_USER`, `SMTP_PASS`, `USUARIOS_PASSWORD`). Las contraseñas de MySQL que ya están son las de
  desarrollo y la de la infraestructura que dio el profesor.

## 4. Después de hacer un cambio (obligatorio)

1. **Compilar**: `cd iw3 && ./mvnw -DskipTests package` (o `mvn` si está instalado).
2. **Probar**: levantar la app y correr la colección de Postman completa con el Runner.
   Si agregaste un endpoint, agregá su request (con test del código de respuesta) a la colección.
3. **Documentar**:
   - Agregar una entrada en `docs/BITACORA.md` (fecha, quién, qué se hizo, archivos tocados).
   - Actualizar `docs/DOCUMENTACION.md` si cambió cómo funciona algo o se tomó una decisión.
   - Actualizar `docs/PENDIENTES.md` (tachar lo hecho, agregar lo nuevo).
   - Actualizar el README si cambió cómo se usa o se corre.
4. **Regenerar el Word** a partir de `docs/DOCUMENTACION.md`:

   ```bash
   pandoc docs/DOCUMENTACION.md -o docs/Documentacion_TP_Integrador.docx --toc --no-highlight --reference-doc=docs/word/referencia.docx
   ```

   `docs/word/referencia.docx` tiene los estilos simples del Word (títulos negros, tablas con bordes
   finos); no lo borres. Si no tenés `pandoc`, avisale al humano que hay que regenerarlo.
5. **Commit** con un mensaje en español que diga qué se hizo (por ejemplo:
   `Punto 3: validación de densidad entre 0 y 1`).

## 5. Trabajo en grupo con git

- Antes de empezar: `git pull` para tener lo último.
- Commits chicos y frecuentes, cada uno con algo que compile y funcione.
- Si dos personas tienen que tocar el mismo archivo (sobre todo `OrdenBusiness.java`), coordinarlo
  antes para evitar conflictos.
- Nunca hacer `git push --force` sobre `main`.

## 6. Cómo correr el proyecto

Ver el README (sección "Entorno de desarrollo"). Resumen: MySQL en el puerto 33306 con usuario
`iw3`/`iw3pass`, y `./mvnw spring-boot:run` dentro de `iw3/`. Los usuarios se crean solos
(clase `auth/DatosIniciales.java`), todos con contraseña `123456` en desarrollo. En la VM la
contraseña es otra y viene de la variable `USUARIOS_PASSWORD`: **nunca la escribas en el repo**.

## 7. Mapa rápido del código

```
iw3/src/main/java/ar/edu/iua/iw3/
  Iw3Application.java          Arranque (zona horaria y log del perfil)
  auth/                        Usuarios, roles, login JWT (del proyecto base)
    DatosIniciales.java        Crea roles y usuarios al arrancar
  business/                    Lógica de negocio
    OrdenBusiness.java         Puntos 1 a 5, estados, auditoría, conciliación
    CamionBusiness.java ...    Sincronización de entidades externas por codigoExterno
    execption/                 Excepciones del negocio
  controllers/
    OrdenRestController.java   Endpoints de los puntos 1 a 5
    Constants.java             Rutas de la API
  model/                       Entidades JPA (Orden, Camion, Cisterna, Chofer, Cliente,
                               Producto, DetalleCarga, CambioEstado, EstadoOrden)
    dto/                       Cuerpos de request/response que no son entidades
    persistence/               Repositorios JPA
  security/SecurityConfiguration.java   Qué rutas son públicas, JWT, CORS
  config/OpenApiConfig.java    Swagger
  util/                        Respuesta de error estándar y mail (del proyecto base)
  websocket/                   Preparado para el final (tiempo real)
```
