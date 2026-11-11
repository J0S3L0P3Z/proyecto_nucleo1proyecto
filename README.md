# Backend de programación de horarios — Proyecto Núcleo 1

Aplicación REST con **Java 21 + Spring Boot 3.4.5 + persistencia JSON**, empaquetada como **WAR**. Cubre la programación semestral de horarios con asignación manual asistida y el **Módulo de Inscripción y Cierre de Grupos**: inscripción con validación de cupo y cruces, detección de grupos por debajo del mínimo, propuestas de reasignación con el enfoque **greedy / first-fit**, aprobación del Comité Táctico y registro de auditoría.

## Requisitos

- JDK 21 y Apache Maven 3.9+ (o Eclipse con m2e). Conexión a Internet la primera vez para descargar dependencias.

## Variables de configuración

| Variable | Uso | Valor por defecto |
|---|---|---|
| `BOOTSTRAP_DIRECTOR_PASSWORD` | Contraseña del director inicial (obligatoria la primera vez, mínimo 12 caracteres recomendados) | vacía |
| `BOOTSTRAP_DIRECTOR_USER` | Usuario del director inicial | `director` |
| `DEMO_PASSWORD` | Si se define, crea los usuarios de demostración `comite`, `wilson.rojas` (docente) y `estudiante1` con esa contraseña | vacía |
| `CORS_ORIGINS` | Orígenes del frontend permitidos, separados por comas | `http://localhost:4200` |
| `APP_DATA_DIR` | Carpeta donde se guarda `estado.json` | `./data` |
| `PORT` | Puerto del servidor embebido | `8080` |

## Ejecución local

En PowerShell:

```powershell
$env:BOOTSTRAP_DIRECTOR_PASSWORD="CambiarEstaClaveSegura2026!"
$env:DEMO_PASSWORD="DemoBosque2026!"
mvn spring-boot:run
```

En Eclipse: **File > Import > Existing Maven Projects**, luego **Run As > Run Configurations > Java Application > HorariosApplication**, y en la pestaña **Environment** agregue las variables. Si Eclipse muestra errores tras importar, use **Maven > Update Project**.

## Generar y desplegar el WAR

```bash
mvn clean package
```

Genera `target/horarios.war`, que se puede usar de dos formas:

1. **Como aplicación independiente** (Tomcat embebido): `java -jar target/horarios.war`. La API queda en `http://localhost:8080/api/...`.
2. **En un Tomcat externo 10.1 o superior** (Jakarta EE 10): copie el WAR a `webapps/`. La API queda en `http://servidor:8080/horarios/api/...`, así que el frontend debe usar esa ruta. Defina las variables de entorno en Tomcat (por ejemplo en `bin/setenv.bat` o `bin/setenv.sh`) y un `APP_DATA_DIR` absoluto con permisos de escritura. Tomcat 9 no sirve porque usa `javax` en lugar de `jakarta`.

`mvn clean package` ejecuta las pruebas; `AplicacionTest` levanta el contexto completo de Spring. Para omitirlas: `mvn clean package -DskipTests`.

### Render (Docker)

El `Dockerfile` compila el WAR y lo ejecuta con Tomcat embebido. En Render: **New > Web Service**, runtime **Docker**, plan **Free**, y en **Environment** defina `BOOTSTRAP_DIRECTOR_PASSWORD`, `DEMO_PASSWORD` y `CORS_ORIGINS` (URL del frontend). Health check: `/api/publico/horarios`. El plan gratuito no conserva archivos al reiniciar, por eso el sistema vuelve a cargar los datos iniciales cada vez que arranca sin `estado.json`.

## Datos precargados

Si no existe `estado.json`, se carga `src/main/resources/datos-iniciales.json`: 7 docentes, 7 cursos, 11 aulas, 13 grupos, el horario **2026-2 versión 1 publicado** (27 sesiones que cumplen R1 a R10), 110 estudiantes con 360 inscripciones y el **periodo de inscripción abierto**. Hay dos grupos por debajo del mínimo para demostrar el cierre: **12598 G2** (6 inscritos) y **13017 G2** (8 inscritos; su alternativa solo tiene 6 cupos, así que 2 estudiantes quedan sin alternativa). Las horas máximas por vinculación quedaron en 24, 18, 12 y 6; ajústelas con el Estatuto Docente.

## Roles

- `DIRECTOR`: opera todo el sistema.
- `COMITE`: Comité Táctico; panel, reasignaciones, cierre de grupos y auditoría.
- `DOCENTE`: consulta su horario.
- `ESTUDIANTE`: consulta horarios y se inscribe en grupos.

Autenticación HTTP Basic. Para entrar, el frontend llama a `GET /api/consulta/yo`, que devuelve rol, docente y estudiante vinculados.

## Endpoints

**Públicos:** `GET /api/publico/horarios`, `GET /api/publico/horarios/{id}?tipo=todos|curso|grupo&filtro=`, `GET /api/publico/catalogo`.

**Consulta e inscripción (autenticados):** `GET /api/consulta/yo`, `GET /api/consulta/horarios/{id}?tipo=&filtro=`, `GET /api/inscripcion/oferta`, `GET /api/inscripcion/mis-grupos`, `POST|DELETE /api/inscripcion/mis-grupos/{grupoId}`.

**Director, maestros (ControladorMaestros):** `GET /api/director/estado`, `GET|PUT /api/director/parametros`, CRUD en `/api/director/docentes`, `/cursos`, `/aulas` y `/estudiantes`, `POST /api/director/usuarios`.

**Director, planificación (ControladorPlanificacion):** `GET /api/director/cursos/{id}/plan`, `POST /api/director/cursos/{id}/grupos?cantidad=`, `POST /api/director/horarios?semestre=`, `GET /api/director/horarios/{id}/sesiones/{s}/celdas?docenteId=&aulaId=`, `POST .../sesiones/{s}/validar`, `PUT|DELETE .../sesiones/{s}`.

**Director, horario (ControladorHorario):** `POST /api/director/horarios/{id}/publicar`, `POST /api/director/horarios/{id}/nueva-version`, `GET /api/director/horarios/{id}/carga`.

**Director, inscripción (ControladorInscripcion):** `POST /api/director/inscripciones` con `{"estudianteId":"...","grupoId":"..."}`, `DELETE /api/director/inscripciones?estudianteId=&grupoId=`, `GET /api/director/grupos/{id}/inscritos`, `POST /api/director/inscripciones/cerrar`, `POST /api/director/inscripciones/abrir`.

**Comité Táctico (ControladorComite):** `GET /api/comite/panel`, `GET /api/comite/reasignaciones?estado=PENDIENTE|APROBADA|RECHAZADA|SIN_ALTERNATIVA`, `POST /api/comite/reasignaciones/{id}/aprobar`, `POST /api/comite/reasignaciones/{id}/rechazar?motivo=`, `POST /api/comite/reasignaciones/aprobar-todas`, `POST /api/comite/grupos/{id}/cerrar`, `GET /api/comite/auditoria`.

## Flujo de inscripción y cierre

1. Con un horario publicado y el periodo abierto, el estudiante se inscribe. Se valida que el grupo esté abierto y ofrecido, que tenga cupo (40 + 10 % = 44), que el estudiante no tenga otro grupo del mismo curso y que no haya cruce de horario. Los intentos rechazados también se auditan.
2. El director cierra el periodo. Se detectan los grupos con menos inscritos que el mínimo (10, configurable) y, para cada estudiante afectado, se propone el **primer** grupo del mismo curso, en orden de número, que esté abierto, no sea crítico, tenga cupo (descontando cupos ya propuestos) y no se cruce con su horario. Si no hay, la propuesta queda **SIN_ALTERNATIVA**.
3. El Comité aprueba o rechaza cada propuesta, o todas. Al aprobar se revalida y se mueve la inscripción.
4. Con las propuestas decididas, el Comité cierra el grupo; los estudiantes que sigan en él quedan sin ese grupo.
5. El director crea una nueva versión del horario, que excluye las sesiones de los grupos cerrados. Las consultas ya no muestran grupos cerrados.

Reabrir el periodo descarta las propuestas que aún no se han decidido.

## Arquitectura (coincide con el diagrama de clases del informe)

- **Controladores** (`api`): `ControladorMaestros`, `ControladorPlanificacion`, `ControladorHorario`, `ControladorInscripcion` y `ControladorComite`. Solo coordinan; no contienen reglas.
- **Servicios** (`service`): `ServicioMaestros`, `ServicioPlanificacion`, `ServicioHorario`, `ServicioInscripcion`, `ServicioAuditoria`, `CalculadorGrupos` (⌈D / (cupo × (1 + tolerancia))⌉) y `ValidadorRestricciones`.
- **Reglas** (`service.reglas`): interfaz `Regla` (`codigo()`, `esDura()`, `evaluar(sesion, ctx)`) con una clase por regla, de `R01Franja` a `R12HuecoLargo`. Agregar una regla no modifica las demás.
- **Estructuras de datos**: `MapaOcupacion` mantiene mapas de ocupación de docente, aula y grupo por franja de una hora, de modo que detectar un cruce cuesta una consulta por hora de la sesión.
- **Modelo** (`model.Domain`), **persistencia** (`AlmacenJson`, escritura atómica y operaciones serializadas) y **seguridad** (`Seguridad`).

## Decisiones y limitaciones

- R12 usa un umbral configurable de 3 horas libres y `SOLO_DIURNO` usa `finDiurno=18`; son decisiones de implementación, no valores institucionales confirmados.
- First-fit no garantiza la asignación óptima: toma el primer grupo que cumple. Si las pruebas lo justifican, se puede evaluar programación por restricciones.
- La auditoría solo se agrega; la API no tiene operaciones para modificarla ni borrarla. Como se guarda en un archivo JSON, quien tenga acceso al servidor sí podría editarlo.
- La persistencia JSON está diseñada para **un solo proceso**. HTTP Basic requiere **HTTPS** en cualquier despliegue no local.
- Las horas máximas por vinculación (RE-11) deben confirmarse con el Estatuto Docente.

## Pruebas

`mvn test` ejecuta las pruebas de reglas (`ValidadorRestriccionesTest`), del cálculo de grupos, del módulo de inscripción (cruce, cupo, first-fit, propuesta sin alternativa), de la carga de los datos iniciales y del arranque de Spring (`AplicacionTest`).
