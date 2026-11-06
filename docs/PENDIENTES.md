# Pendientes

Lista de lo que falta. Al terminar algo, marcarlo con `[x]` y anotarlo en `BITACORA.md`.

## Para el segundo parcial (entrega 10/11/2026)

- [x] Proyecto base limpio (sin cli1, cli2, Product ni demos)
- [x] Modelo de datos y repositorios
- [x] Puntos 1 a 5 funcionando
- [x] Roles y usuarios por sistema
- [x] Auditoría de cambios de estado
- [x] Frecuencia de guardado del detalle (opcional en la consigna)
- [x] Promedios en la conciliación (opcional en la consigna)
- [x] Documentación OpenAPI (Swagger)
- [x] Colección de Postman con el circuito completo y tests
- [ ] Cada integrante: clonar el repo, levantar MySQL y correr la colección en su PC
- [x] Deploy en la VM con `infra/deploy.sh`
- [x] Correr la colección de Postman con el entorno "IW3 - VM"
- [x] Contraseña de los usuarios por variable de entorno (`USUARIOS_PASSWORD`)
- [x] Cargar `USUARIOS_PASSWORD` en la VM y redesplegar (ver BITACORA)
- [ ] Repasar entre todos el código completo (ver "Preguntas para practicar" en DOCUMENTACION.md)
- [x] Guía de estudio con la teoría de las clases aplicada al TP (`docs/GUIA_ESTUDIO.md` y su Word)
- [ ] Presentación (15 a 20 minutos mostrando el sistema con Postman, repartida entre los 4; guion en
      la sección 21 de la guía)

## Para el examen final

- [ ] **Alarma de temperatura**: al recibir un dato con temperatura mayor al umbral, crear una
      alarma y mandar un mail. No volver a mandar hasta que un usuario la acepte.
      Umbral y mails configurables. Idea: entidad `Alarma`, evento + listener asíncrono
      (mismo patrón que `UserEvent`), `EmailBusiness` del proyecto base.
- [ ] **Tiempo real**: después de cada dato válido publicar la orden por WebSocket
      (`SimpMessagingTemplate`, destino `/topic/ordenes`). La configuración ya está en `WebSocketConfig`.
- [ ] **Frontend** (HTML + JS en `iw3/src/main/resources/static/ui`): login, lista de órdenes
      con filtros y orden, ETA, tiempo transcurrido, alarmas con aceptación, conciliación.
- [ ] Endpoints de alarmas: listar y aceptar (con usuario, fecha y observación).

## Dudas para consultar al profesor

1. ~~Somos 4 integrantes y la consigna dice grupos de hasta 3.~~ **Resuelta:** en el video de
   criterios el profesor dice "tenemos un grupo de cuatro, no hay problema con eso".
2. ~~¿Quién genera la contraseña de activación?~~ **Resuelta:** en el video de la consigna dice
   "nuestro backend genera una password de activación" (es lo que hicimos).
3. ¿El sistema de carga tiene que mandar la contraseña en cada dato de detalle, o alcanza con la
   activación? Hoy alcanza con la activación.
4. Si SAP manda una orden con un número que ya existe, ¿es error o actualización? Hoy es error (302).
5. ¿Hay que descartar densidades fuera del rango 0 a 1? Hoy no se descartan.
6. Para la alarma, ¿"configurable" alcanza con `application.properties` / variables de entorno,
   o tiene que poder cambiarse desde la interfaz?
7. ¿El frontend puede estar dentro del backend (carpeta `static`), o lo quiere como proyecto separado?
8. ~~¿Hay un formato de JSON fijo para lo que mandan SAP y TMS?~~ **Resuelta:** en el video de
   criterios dice que "toda la definición de endpoint de la API queda a criterio de los grupos".
9. En el video de la consigna el profesor dice que la frecuencia de guardado del detalle "se va a tener
   que poder definir en nuestra base de datos". Hoy está en `application.properties`. Ver si hay que
   pasarla a una tabla para el final.
