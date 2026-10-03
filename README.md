# NotificationSystemKafka

API REST construida con Spring Boot para gestionar usuarios, presupuestos mensuales y gastos. El sistema publica eventos en Kafka cuando se registran usuarios, se crean gastos o se supera un presupuesto, y consume esos eventos para enviar notificaciones por email.

## Tabla de contenidos

- [Tecnologias](#tecnologias)
- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Configuracion](#configuracion)
- [Ejecucion con Docker](#ejecucion-con-docker)
- [Ejecucion local](#ejecucion-local)
- [API REST](#api-rest)
- [Respuestas de error](#respuestas-de-error)
- [Eventos Kafka](#eventos-kafka)
- [Notificaciones por email](#notificaciones-por-email)
- [Modelo de datos](#modelo-de-datos)
- [Pruebas](#pruebas)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Notas de desarrollo](#notas-de-desarrollo)

## Tecnologias

- Java 21
- Spring Boot 3.5.14
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Kafka
- Spring Mail
- springdoc-openapi 2.8.17 (Swagger UI)
- Lombok
- PostgreSQL 16
- Apache Kafka con Zookeeper
- MailHog para emails en desarrollo
- Maven
- Docker y Docker Compose
- Tests: JUnit 5, Mockito, H2 en memoria y spring-kafka-test

## Arquitectura

El proyecto sigue una arquitectura por capas:

- `controller`: expone los endpoints HTTP.
- `service`: contiene la logica de negocio.
- `repository`: acceso a datos con Spring Data JPA.
- `model`: entidades JPA.
- `dto`: objetos de entrada y salida de la API.
- `kafka`: productores, consumidores, eventos y constantes de topics.
- `notifications`: envio de emails.
- `exception`: manejo centralizado de errores.
- `config`: configuracion de CORS (`CorsConfig`) y de OpenAPI (`OpenApiConfig`).

Flujo principal:

1. Un cliente llama a la API REST.
2. El controlador valida el request y delega al servicio.
3. El servicio persiste o consulta datos en PostgreSQL.
4. Algunas operaciones publican eventos en Kafka.
5. Los consumidores procesan eventos y envian emails mediante Spring Mail.

## Requisitos

Para ejecutar con Docker:

- Docker
- Docker Compose

Para ejecutar localmente:

- Java 21
- Maven o el wrapper incluido (`mvnw.cmd` en Windows)
- PostgreSQL
- Kafka
- Un servidor SMTP local o externo

## Configuracion

La aplicacion lee variables de entorno y tambien importa opcionalmente un archivo `.env` en la raiz del proyecto:

```properties
spring.config.import=optional:file:.env[.properties]
```

Variables soportadas:

| Variable | Valor por defecto | Descripcion |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/notifications` | URL JDBC de PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuario de base de datos |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Password de base de datos |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9094` | Bootstrap servers de Kafka |
| `SPRING_MAIL_HOST` | `localhost` | Host SMTP |
| `SPRING_MAIL_PORT` | `1025` | Puerto SMTP |
| `SPRING_MAIL_USERNAME` | vacio | Usuario SMTP |
| `SPRING_MAIL_PASSWORD` | vacio | Password SMTP |
| `SPRING_MAIL_SMTP_AUTH` | `false` | Habilita autenticacion SMTP |
| `SPRING_MAIL_SMTP_STARTTLS_ENABLE` | `false` | Habilita STARTTLS |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:4200` | Origenes permitidos por CORS, separados por coma |

Ejemplo de `.env` para desarrollo local:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/notifications
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9094
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
SPRING_MAIL_SMTP_AUTH=false
SPRING_MAIL_SMTP_STARTTLS_ENABLE=false
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Ejecucion con Docker

Levantar todos los servicios:

```bash
docker compose up --build
```

Servicios expuestos:

| Servicio | URL / Puerto |
| --- | --- |
| API | `http://localhost:8080` |
| PostgreSQL | `localhost:5432` |
| Kafka | `localhost:9094` |
| MailHog SMTP | `localhost:1025` |
| MailHog UI | `http://localhost:8025` |

Detener servicios:

```bash
docker compose down
```

Eliminar tambien el volumen de PostgreSQL:

```bash
docker compose down -v
```

El `Dockerfile` es multi-stage (Maven + Temurin 21) y compila la imagen con `-DskipTests`. El servicio `app` de `docker-compose.yml` no define `APP_CORS_ALLOWED_ORIGINS`, por lo que usa los origenes por defecto.

## Ejecucion local

1. Levantar PostgreSQL, Kafka y un SMTP local. Con el `docker-compose.yml` del proyecto se pueden levantar los servicios de soporte:

```bash
docker compose up postgres zookeeper kafka mailhog
```

2. Ejecutar la aplicacion:

```bash
.\mvnw.cmd spring-boot:run
```

La API quedara disponible en:

```text
http://localhost:8080
```

## API REST

Base URL:

```text
http://localhost:8080/api/v1
```

Documentacion interactiva:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

Codigos de exito comunes a los tres recursos:

| Operacion | Codigo |
| --- | --- |
| `GET` (lista o por ID) | `200 OK` |
| `POST` | `201 Created` |
| `PUT` | `200 OK` |
| `DELETE` | `204 No Content` |

El `{id}` de la ruta debe ser positivo; un valor menor o igual a `0` es invalido (`400`).

### Usuarios

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| `GET` | `/users` | Lista usuarios |
| `GET` | `/users/{id}` | Obtiene un usuario por ID |
| `POST` | `/users` | Crea un usuario |
| `PUT` | `/users/{id}` | Actualiza un usuario |
| `DELETE` | `/users/{id}` | Elimina un usuario |

Crear usuario:

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Juan Perez\",\"email\":\"juan@example.com\"}"
```

Request:

```json
{
  "name": "Juan Perez",
  "email": "juan@example.com"
}
```

Response:

```json
{
  "id": 1,
  "name": "Juan Perez",
  "email": "juan@example.com",
  "createdAt": "2026-06-01T17:30:00"
}
```

Reglas:

- `name` es obligatorio y tiene maximo 100 caracteres.
- `email` es obligatorio, debe tener formato valido y maximo 150 caracteres.
- El email se normaliza a minusculas.
- No puede haber dos usuarios con el mismo email. Al crear se valida de forma explicita; al actualizar (`PUT`) el duplicado lo detecta la restriccion unica de la base de datos y devuelve un `409` generico.
- No se puede eliminar un usuario que tenga presupuestos o gastos asociados: la violacion de clave foranea devuelve `409`.

### Presupuestos

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| `GET` | `/budgets` | Lista presupuestos |
| `GET` | `/budgets/{id}` | Obtiene un presupuesto por ID |
| `POST` | `/budgets` | Crea un presupuesto |
| `PUT` | `/budgets/{id}` | Actualiza un presupuesto |
| `DELETE` | `/budgets/{id}` | Elimina un presupuesto |

Crear presupuesto:

```bash
curl -X POST http://localhost:8080/api/v1/budgets \
  -H "Content-Type: application/json" \
  -d "{\"userId\":1,\"category\":\"food\",\"limitAmount\":300.00,\"month\":\"2026-06\"}"
```

Request:

```json
{
  "userId": 1,
  "category": "food",
  "limitAmount": 300.00,
  "month": "2026-06"
}
```

Response:

```json
{
  "id": 1,
  "userId": 1,
  "category": "food",
  "limitAmount": 300.00,
  "month": "2026-06"
}
```

Reglas:

- `userId` debe existir.
- `category` es obligatoria y tiene maximo 80 caracteres.
- `category` se normaliza a minusculas.
- `limitAmount` es obligatorio y positivo.
- `month` debe tener formato `yyyy-MM`.
- Solo puede existir un presupuesto por usuario, categoria y mes.

### Gastos

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| `GET` | `/expenses` | Lista gastos |
| `GET` | `/expenses/{id}` | Obtiene un gasto por ID |
| `POST` | `/expenses` | Crea un gasto |
| `PUT` | `/expenses/{id}` | Actualiza un gasto |
| `DELETE` | `/expenses/{id}` | Elimina un gasto |

Crear gasto:

```bash
curl -X POST http://localhost:8080/api/v1/expenses \
  -H "Content-Type: application/json" \
  -d "{\"userId\":1,\"description\":\"Lunch\",\"category\":\"food\",\"amount\":25.50,\"date\":\"2026-06-01\"}"
```

Request:

```json
{
  "userId": 1,
  "description": "Lunch",
  "category": "food",
  "amount": 25.50,
  "date": "2026-06-01"
}
```

Response:

```json
{
  "id": 1,
  "userId": 1,
  "description": "Lunch",
  "category": "food",
  "amount": 25.50,
  "date": "2026-06-01"
}
```

Reglas:

- `userId` debe existir.
- `description` es opcional y tiene maximo 255 caracteres.
- `category` es obligatoria y tiene maximo 80 caracteres.
- `category` se normaliza a minusculas.
- `amount` es obligatorio y debe ser mayor que `0`.
- `date` debe ser presente o pasada. Si no se envia, se usa la fecha actual.

## Respuestas de error

Los errores gestionados por `GlobalExceptionHandler` se devuelven con una estructura comun:

```json
{
  "timestamp": "2026-06-01T15:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users",
  "validationErrors": {
    "email": "Email must have a valid format"
  }
}
```

Codigos habituales:

| Codigo | Caso |
| --- | --- |
| `400` | Validacion invalida |
| `404` | Recurso no encontrado |
| `409` | Conflicto por duplicados o integridad de datos |

Notas:

- Los `404` de `GET`, `PUT` y `DELETE` sobre `/{id}` inexistente se devuelven **sin cuerpo**. Solo el `404` por un `userId` inexistente en el body de un `POST`/`PUT` (mensaje `User not found`) usa la estructura comun.
- `validationErrors` solo se rellena en los `400` de validacion; en el resto es un objeto vacio.
- Un `409` por violacion de integridad (por ejemplo email duplicado en `PUT` o usuario con datos asociados) lleva el mensaje generico `The request conflicts with existing data`.

## Eventos Kafka

Topics definidos:

| Topic | Evento | Productor | Consumidor |
| --- | --- | --- | --- |
| `user-registered` | `UserRegisteredEvent` | Al crear usuario | Envia email de bienvenida |
| `expense-created` | `ExpenseCreatedEvent` | Al crear gasto | Sin consumidor actual |
| `budget-exceeded` | `BudgetExceededEvent` | Al superar presupuesto | Envia email de presupuesto excedido |

Los eventos se serializan como JSON (`JsonSerializer` / `JsonDeserializer`, con `trusted.packages` limitado a `com.jnrptt.notificationsystemkafka.kafka.event`).

| Evento | Campos |
| --- | --- |
| `UserRegisteredEvent` | `userId`, `name`, `email`, `registeredAt` |
| `ExpenseCreatedEvent` | `expenseId`, `userId`, `userEmail`, `description`, `category`, `amount`, `date` |
| `BudgetExceededEvent` | `userId`, `userEmail`, `category`, `limitAmount`, `total`, `month` |

`ExpenseCreatedEvent` se publica solo al **crear** un gasto, no al actualizarlo.

### Flujo de presupuesto excedido

1. Se crea o actualiza un gasto.
2. La aplicacion busca un presupuesto del mismo usuario, categoria y mes.
3. Calcula el total mensual de gastos de esa categoria.
4. Si el total supera el limite, publica `BudgetExceededEvent`.
5. El consumidor envia un email al usuario.

El evento se publica en **cada** creacion o actualizacion de gasto mientras el total siga por encima del limite, por lo que el usuario puede recibir varios emails en el mismo mes.

### Consumidores, reintentos y DLT

- Grupo de consumidores: `notification-group`, con `auto-offset-reset=earliest`.
- Los consumidores usan `@RetryableTopic` con 3 intentos y backoff de 2 segundos. Spring Kafka crea los topics de reintento y el DLT automaticamente.
- Si el procesamiento sigue fallando, el mensaje llega al DLT (`DltStrategy.FAIL_ON_ERROR`) y el `@DltHandler` registra el error en el log.

## Notificaciones por email

El servicio `EmailNotificationService` envia:

- Email de bienvenida cuando se registra un usuario (asunto `Bienvenido`).
- Email de aviso cuando se supera un presupuesto (asunto `Aviso de gasto excedido`). El texto es generico: no incluye categoria, limite ni total, aunque `BudgetExceededEvent` los contiene.

En Docker Compose se usa MailHog:

- SMTP: `localhost:1025`
- Interfaz web: `http://localhost:8025`

## Modelo de datos

### User

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `Long` | ID autogenerado |
| `name` | `String` | Obligatorio, maximo 100 |
| `email` | `String` | Obligatorio, unico, maximo 150 |
| `createdAt` | `LocalDateTime` | Fecha de creacion |

### Budget

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `Long` | ID autogenerado |
| `user` | `User` | Relacion obligatoria |
| `category` | `String` | Obligatoria, maximo 80 |
| `limitAmount` | `BigDecimal` | Obligatorio |
| `month` | `String` | Formato `yyyy-MM` |

Restriccion unica:

```text
user_id + category + month
```

### Expense

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `Long` | ID autogenerado |
| `user` | `User` | Relacion obligatoria |
| `amount` | `BigDecimal` | Obligatorio |
| `description` | `String` | Opcional, maximo 255 |
| `category` | `String` | Obligatoria, maximo 80 |
| `date` | `LocalDate` | Fecha del gasto |

## Pruebas

Ejecutar tests:

```bash
.\mvnw.cmd test
.\mvnw.cmd test -Dtest=ExpenseServiceTest
```

Los tests no necesitan PostgreSQL, Kafka ni SMTP: el test de contexto usa H2 en memoria (modo PostgreSQL) y el resto son tests unitarios con mocks.

El proyecto incluye pruebas para:

- `UserService`, `BudgetService` y `ExpenseService`
- `EmailNotificationService`
- `NotificationConsumer`
- `GlobalExceptionHandler`
- Carga del contexto de Spring Boot y disponibilidad de `/api-docs`

## Estructura del proyecto

```text
.
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/jnrptt/notificationsystemkafka
│   │   │   ├── config
│   │   │   ├── controller
│   │   │   ├── dto
│   │   │   ├── exception
│   │   │   ├── kafka
│   │   │   ├── model
│   │   │   ├── notifications
│   │   │   ├── repository
│   │   │   └── service
│   │   └── resources/application.properties
│   └── test
│       └── java/com/jnrptt/notificationsystemkafka
│           ├── exception
│           ├── kafka/consumer
│           ├── notifications
│           └── service
└── README.md
```

## Notas de desarrollo

- CORS aplica solo a `/api/**` (metodos `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`) y por defecto permite `http://localhost:3000`, `http://localhost:5173` y `http://localhost:4200`. Puedes cambiarlo con `APP_CORS_ALLOWED_ORIGINS`.
- Hibernate esta configurado con `spring.jpa.hibernate.ddl-auto=update`, por lo que el esquema se actualiza automaticamente en desarrollo.
- El puerto de Kafka para conexiones desde el host es `9094`.
- El puerto interno usado por la aplicacion dentro de Docker Compose es `kafka:9092`.
- El topic `expense-created` se publica al crear gastos, pero actualmente no hay un consumidor asociado.
