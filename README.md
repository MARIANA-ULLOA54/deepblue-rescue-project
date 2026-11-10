# DeepBlue Rescue

Capa de persistencia de una plataforma de rescate de fauna marina. Java 21, Spring Boot 4.1.2, Spring Data JPA (Hibernate), Flyway, PostgreSQL y Testcontainers.

## Modelo de datos

Tablas: `rescue_centers`, `rescue_cases`, `animals`, `medical_records`, `specialists`, `expertise`, `treatments` y la tabla asociativa `specialist_expertise`.

| Relación | Implementación |
|---|---|
| RescueCenter 1:N RescueCase | FK `rescue_cases.rescue_center_id` |
| RescueCase 1:1 Animal | FK `animals.rescue_case_id` + UNIQUE |
| Animal 1:1 MedicalRecord | FK `medical_records.animal_id` + UNIQUE |
| Specialist N:M Expertise | tabla `specialist_expertise` (PK compuesta) |
| Animal / Specialist 1:N Treatment | FKs NOT NULL en `treatments` |

`rescue_cases.status` tiene un CHECK con los seis estados válidos.

## Ejecutar la aplicación

Requiere un PostgreSQL local (`DB_URL`, `DB_USER`, `DB_PASSWORD`; por defecto `localhost:5432/deepblue`, `postgres/postgres`).

```bash
./mvnw spring-boot:run
```

## Ejecutar los tests

Requiere Docker en ejecución (Testcontainers levanta `postgres:18-alpine`).

```bash
./mvnw clean test
```

## Flyway

Migraciones en `src/main/resources/db/migration`:
- `V1__create_schema.sql`: tablas, PK, FK, UNIQUE, CHECK e índices.
- `V2__insert_expertise_catalog.sql`: catálogo de expertise.
- `V3__add_tracking_device_to_animal.sql`: columna `tracking_device_code` (nullable, única).

Hibernate solo valida el esquema (`ddl-auto: validate`).

## Testcontainers

`PersistenceIntegrationTest` usa un PostgreSQL real con `@Testcontainers` y `@ServiceConnection`. Cubre Flyway, métodos heredados, relaciones, Query Methods, JPQL, constraints (UNIQUE, FK, CHECK) y el reto integrador.

## Query Methods

- `RescueCenterRepository`: `findByCode`
- `RescueCaseRepository`: `findByCaseCode`, `existsByCaseCode`, `findByStatusOrderByRescueDateAsc`, `findByRescueCenterCode`, `findByRescueDateAfterOrderByRescueDateDesc`
- `AnimalRepository`: `findByAnimalCode`, `findByCommonNameContainingIgnoreCase`, `findByRescueCaseStatus`, `findByRescueCaseRescueCenterCode`
- `ExpertiseRepository`: `findByNameIgnoreCase`
- `SpecialistRepository`: `findByProfessionalCode`
- `TreatmentRepository`: `findByAnimalIdOrderByPerformedAtAsc`, `findByAnimalAnimalCodeOrderByPerformedAtAsc`

## Consultas JPQL

- `SpecialistRepository.findActiveByExpertise`
- `TreatmentRepository.findPerformedBetween`
- `TreatmentRepository.findByCenterCode` (Treatment → Animal → RescueCase → RescueCenter)
- `TreatmentRepository.findBySpecialistExpertise` (N:M, `distinct`)
- `AnimalRepository.findByStatusAndTreatedBySpecialistWithExpertise` (reto sin guía)
