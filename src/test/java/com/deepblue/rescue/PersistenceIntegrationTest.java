package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings({"resource", "unused", "SqlResolve"})
@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("deepblue_test")
            .withUsername("deepblue")
            .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;
    @Autowired
    private RescueCaseRepository rescueCaseRepository;
    @Autowired
    private AnimalRepository animalRepository;
    @Autowired
    private MedicalRecordRepository medicalRecordRepository;
    @Autowired
    private SpecialistRepository specialistRepository;
    @Autowired
    private ExpertiseRepository expertiseRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ------------------------------------------------------------------ helpers
    private RescueCenter center(String code) {
        return rescueCenterRepository.save(new RescueCenter(code, "Centro " + code, "Santa Marta"));
    }

    private RescueCase rescueCase(RescueCenter center, String code, RescueStatus status, LocalDate date) {
        RescueCase c = new RescueCase();
        c.setCaseCode(code);
        c.setRescueDate(date);
        c.setRescueLocation("Bahía Concha");
        c.setStatus(status);
        center.addCase(c);
        return c;
    }

    /**
     * Crea y persiste caso + animal (cascade desde RescueCase).
     */
    private Animal animalInCase(RescueCenter center, String caseCode, RescueStatus status,
            String animalCode, String commonName) {
        RescueCase c = rescueCase(center, caseCode, status, LocalDate.of(2026, 8, 18));
        Animal a = new Animal();
        a.setAnimalCode(animalCode);
        a.setCommonName(commonName);
        a.setScientificName("Chelonia mydas");
        a.setSex(AnimalSex.FEMALE);
        c.assignAnimal(a);
        rescueCaseRepository.saveAndFlush(c);
        return a;
    }

    private Specialist specialist(String code, String first, String last, String email, String... expertise) {
        Specialist s = new Specialist();
        s.setProfessionalCode(code);
        s.setFirstName(first);
        s.setLastName(last);
        s.setEmail(email);
        for (String name : expertise) {
            Expertise e = expertiseRepository.findByNameIgnoreCase(name).orElseThrow();
            s.addExpertise(e);
        }
        return specialistRepository.saveAndFlush(s);
    }

    private Treatment treatment(Animal a, Specialist s, TreatmentType type, LocalDateTime at) {
        Treatment t = new Treatment();
        t.setAnimal(a);
        t.setSpecialist(s);
        t.setType(type);
        t.setPerformedAt(at);
        t.setDescription("Tratamiento " + type);
        return treatmentRepository.saveAndFlush(t);
    }

    // ------------------------------------------------------------------ Flyway
    @Test
    @DisplayName("Flyway ejecutó V1, V2 y V3")
    void flywayAppliedMigrations() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true", String.class);
        assertThat(versions).contains("1", "2", "3");
    }

    @Test
    @DisplayName("V2 insertó el catálogo de expertise")
    void expertiseCatalogLoaded() {
        assertThat(expertiseRepository.findByNameIgnoreCase("trauma")).isPresent();
        assertThat(expertiseRepository.findByNameIgnoreCase("REHABILITATION")).isPresent();
        assertThat(expertiseRepository.count()).isGreaterThanOrEqualTo(6);
    }

    // ------------------------------------------------------------------ métodos heredados
    @Test
    @DisplayName("save, findById, existsById y count")
    void inheritedMethods() {
        long before = rescueCenterRepository.count();

        RescueCenter saved = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        assertThat(saved.getId()).isNotNull();
        assertThat(rescueCenterRepository.findById(saved.getId())).isPresent();
        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();
        assertThat(rescueCenterRepository.count()).isEqualTo(before + 1);
        assertThat(rescueCenterRepository.findByCode("DB-CAR")).isPresent();
    }

    // ------------------------------------------------------------------ relaciones
    @Test
    @DisplayName("RescueCenter 1:N RescueCase")
    void oneToMany() {
        RescueCenter c = center("DB-CAR");
        rescueCaseRepository.saveAndFlush(rescueCase(c, "RES-A", RescueStatus.ADMITTED, LocalDate.of(2026, 8, 1)));
        rescueCaseRepository.saveAndFlush(rescueCase(c, "RES-B", RescueStatus.ADMITTED, LocalDate.of(2026, 8, 2)));

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-CAR");
        assertThat(cases).hasSize(2)
                .allSatisfy(rc -> assertThat(rc.getRescueCenter()).isSameAs(c));
    }

    @Test
    @DisplayName("RescueCase 1:1 Animal")
    void caseAnimalOneToOne() {
        RescueCenter c = center("DB-CAR");
        Animal a = animalInCase(c, "RES-2026-001", RescueStatus.ADMITTED, "AN-2026-001", "Green Sea Turtle");

        RescueCase rc = a.getRescueCase();
        assertThat(rc.getAnimal()).isSameAs(a);
        assertThat(a.getRescueCase()).isSameAs(rc);
        assertThat(a.getId()).isNotNull();
    }

    @Test
    @DisplayName("Animal 1:1 MedicalRecord con cascade")
    void animalMedicalRecordOneToOne() {
        RescueCenter c = center("DB-CAR");
        RescueCase rc = rescueCase(c, "RES-2026-002", RescueStatus.ADMITTED, LocalDate.of(2026, 8, 18));
        Animal a = new Animal();
        a.setAnimalCode("AN-2026-002");
        a.setCommonName("Green Sea Turtle");
        a.setScientificName("Chelonia mydas");
        a.setSex(AnimalSex.MALE);
        rc.assignAnimal(a);

        MedicalRecord mr = new MedicalRecord();
        mr.setInitialWeight(new BigDecimal("28.40"));
        mr.setInitialCondition("STABLE");
        mr.setInjuries("Left front flipper injury");
        a.assignMedicalRecord(mr);

        rescueCaseRepository.saveAndFlush(rc);

        assertThat(a.getId()).isNotNull();
        assertThat(mr.getId()).isNotNull();
        assertThat(medicalRecordRepository.findById(mr.getId())).isPresent();
        assertThat(mr.getAnimal()).isSameAs(a);
    }

    @Test
    @DisplayName("Specialist N:M Expertise")
    void manyToMany() {
        Specialist elena = specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org",
                "Trauma", "Rehabilitation");

        Specialist found = specialistRepository.findById(elena.getId()).orElseThrow();
        assertThat(found.getExpertiseAreas()).hasSize(2);
        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from specialist_expertise where specialist_id = ?",
                Integer.class, elena.getId());
        assertThat(rows).isEqualTo(2);
    }

    // ------------------------------------------------------------------ Query Methods
    @Test
    @DisplayName("Query Method por status, ordenado por fecha")
    void findByStatus() {
        RescueCenter c = center("DB-CAR");
        rescueCaseRepository.saveAndFlush(rescueCase(c, "RES-001", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 8, 3)));
        rescueCaseRepository.saveAndFlush(rescueCase(c, "RES-002", RescueStatus.READY_FOR_RELEASE, LocalDate.of(2026, 8, 2)));
        rescueCaseRepository.saveAndFlush(rescueCase(c, "RES-003", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 8, 1)));

        List<RescueCase> result = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(result).extracting(RescueCase::getCaseCode).containsExactly("RES-003", "RES-001");
    }

    @Test
    @DisplayName("Query Method con fecha: posteriores, más reciente primero")
    void findByDateAfter() {
        RescueCenter c = center("DB-CAR");
        rescueCaseRepository.saveAndFlush(rescueCase(c, "RES-D1", RescueStatus.ADMITTED, LocalDate.of(2026, 7, 1)));
        rescueCaseRepository.saveAndFlush(rescueCase(c, "RES-D2", RescueStatus.ADMITTED, LocalDate.of(2026, 8, 10)));
        rescueCaseRepository.saveAndFlush(rescueCase(c, "RES-D3", RescueStatus.ADMITTED, LocalDate.of(2026, 8, 20)));

        assertThat(rescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc(LocalDate.of(2026, 8, 1)))
                .extracting(RescueCase::getCaseCode).containsExactly("RES-D3", "RES-D2");
    }

    @Test
    @DisplayName("Query Method navegando Animal → RescueCase → RescueCenter")
    void findAnimalsByCenterCode() {
        RescueCenter car = center("DB-CAR");
        RescueCenter pac = center("DB-PAC");
        animalInCase(car, "RES-C1", RescueStatus.ADMITTED, "AN-C1", "Green Sea Turtle");
        animalInCase(pac, "RES-P1", RescueStatus.ADMITTED, "AN-P1", "Hawksbill Turtle");

        assertThat(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR"))
                .extracting(Animal::getAnimalCode).containsExactly("AN-C1");
        assertThat(animalRepository.findByRescueCaseStatus(RescueStatus.ADMITTED)).hasSize(2);
    }

    // ------------------------------------------------------------------ JPQL
    @Test
    @DisplayName("JPQL: especialistas activos por expertise")
    void findActiveSpecialistsByExpertise() {
        specialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org", "Trauma", "Rehabilitation");
        specialist("SPEC-M", "Mateo", "Gómez", "mateo@deepblue.org", "Marine Mammals", "Rehabilitation");
        specialist("SPEC-S", "Sofia", "Rojas", "sofia@deepblue.org", "Marine Birds", "Trauma");

        assertThat(specialistRepository.findActiveByExpertise("trauma"))
                .extracting(Specialist::getProfessionalCode).containsExactly("SPEC-S", "SPEC-E");
    }

    @Test
    @DisplayName("Query Method: tratamientos de un animal ordenados cronológicamente")
    void treatmentsByAnimalOrdered() {
        RescueCenter c = center("DB-CAR");
        Animal a = animalInCase(c, "RES-T1", RescueStatus.IN_REHABILITATION, "AN-T1", "Green Sea Turtle");
        Specialist elena = specialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org", "Trauma");
        Specialist mateo = specialist("SPEC-M", "Mateo", "Gómez", "mateo@deepblue.org", "Marine Mammals");

        treatment(a, mateo, TreatmentType.OBSERVATION, LocalDateTime.of(2026, 8, 20, 10, 0));
        treatment(a, elena, TreatmentType.WOUND_CARE, LocalDateTime.of(2026, 8, 18, 10, 0));
        treatment(a, elena, TreatmentType.HYDRATION, LocalDateTime.of(2026, 8, 19, 10, 0));

        assertThat(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(a.getId()))
                .extracting(Treatment::getType)
                .containsExactly(TreatmentType.WOUND_CARE, TreatmentType.HYDRATION, TreatmentType.OBSERVATION);
    }

    @Test
    @DisplayName("JPQL: tratamientos en un intervalo")
    void treatmentsBetween() {
        RescueCenter c = center("DB-CAR");
        Animal a = animalInCase(c, "RES-T2", RescueStatus.IN_REHABILITATION, "AN-T2", "Green Sea Turtle");
        Specialist s = specialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org", "Trauma");

        treatment(a, s, TreatmentType.WOUND_CARE, LocalDateTime.of(2026, 8, 1, 10, 0));
        Treatment middle = treatment(a, s, TreatmentType.HYDRATION, LocalDateTime.of(2026, 8, 10, 10, 0));
        treatment(a, s, TreatmentType.OBSERVATION, LocalDateTime.of(2026, 8, 20, 10, 0));

        assertThat(treatmentRepository.findPerformedBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0), LocalDateTime.of(2026, 8, 15, 0, 0)))
                .containsExactly(middle);
    }

    @Test
    @DisplayName("JPQL: tratamientos por centro (3 entidades)")
    void treatmentsByCenter() {
        RescueCenter car = center("DB-CAR");
        RescueCenter pac = center("DB-PAC");
        Animal a1 = animalInCase(car, "RES-C1", RescueStatus.IN_REHABILITATION, "AN-C1", "Green Sea Turtle");
        Animal a2 = animalInCase(pac, "RES-P1", RescueStatus.IN_REHABILITATION, "AN-P1", "Hawksbill Turtle");
        Specialist s = specialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org", "Trauma");

        Treatment t1 = treatment(a1, s, TreatmentType.WOUND_CARE, LocalDateTime.of(2026, 8, 19, 9, 0));
        treatment(a2, s, TreatmentType.WOUND_CARE, LocalDateTime.of(2026, 8, 19, 9, 0));

        assertThat(treatmentRepository.findByCenterCode("DB-CAR")).containsExactly(t1);
    }

    @Test
    @DisplayName("JPQL N:M: tratamientos por expertise del especialista, sin duplicados")
    void treatmentsBySpecialistExpertise() {
        RescueCenter c = center("DB-CAR");
        Animal a = animalInCase(c, "RES-T3", RescueStatus.IN_REHABILITATION, "AN-T3", "Green Sea Turtle");
        Specialist elena = specialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org",
                "Marine Reptiles", "Trauma", "Rehabilitation");
        Specialist sofia = specialist("SPEC-S", "Sofia", "Rojas", "sofia@deepblue.org", "Marine Birds");

        Treatment t1 = treatment(a, elena, TreatmentType.WOUND_CARE, LocalDateTime.of(2026, 8, 19, 9, 0));
        treatment(a, sofia, TreatmentType.NUTRITION, LocalDateTime.of(2026, 8, 20, 9, 0));

        assertThat(treatmentRepository.findBySpecialistExpertise("rehabilitation")).containsExactly(t1);
    }

    @Test
    @DisplayName("Reto sin guía: animales en rehabilitación tratados por especialista en Trauma")
    void animalsInRehabilitationTreatedByTraumaSpecialist() {
        RescueCenter c = center("DB-CAR");
        Animal match = animalInCase(c, "RES-R1", RescueStatus.IN_REHABILITATION, "AN-R1", "Green Sea Turtle");
        Animal wrongStatus = animalInCase(c, "RES-R2", RescueStatus.ADMITTED, "AN-R2", "Green Sea Turtle");
        Animal wrongExpertise = animalInCase(c, "RES-R3", RescueStatus.IN_REHABILITATION, "AN-R3", "Green Sea Turtle");

        Specialist trauma = specialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org", "Trauma", "Rehabilitation");
        Specialist birds = specialist("SPEC-S", "Sofia", "Rojas", "sofia@deepblue.org", "Marine Birds");

        // dos tratamientos al mismo animal: no debe duplicarse en el resultado
        treatment(match, trauma, TreatmentType.WOUND_CARE, LocalDateTime.of(2026, 8, 19, 9, 0));
        treatment(match, trauma, TreatmentType.HYDRATION, LocalDateTime.of(2026, 8, 20, 9, 0));
        treatment(wrongStatus, trauma, TreatmentType.WOUND_CARE, LocalDateTime.of(2026, 8, 19, 9, 0));
        treatment(wrongExpertise, birds, TreatmentType.NUTRITION, LocalDateTime.of(2026, 8, 19, 9, 0));

        assertThat(animalRepository.findByStatusAndTreatedBySpecialistWithExpertise(
                RescueStatus.IN_REHABILITATION, "TRAUMA")).containsExactly(match);
    }

    // ------------------------------------------------------------------ constraints
    @Test
    @DisplayName("UNIQUE: animal_code duplicado")
    void uniqueAnimalCode() {
        RescueCenter c = center("DB-CAR");
        animalInCase(c, "RES-U1", RescueStatus.ADMITTED, "AN-100", "Green Sea Turtle");

        assertThatThrownBy(() -> animalInCase(c, "RES-U2", RescueStatus.ADMITTED, "AN-100", "Green Sea Turtle"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("UNIQUE: tracking_device_code (V3) duplicado")
    void uniqueTrackingDevice() {
        RescueCenter c = center("DB-CAR");
        Animal a1 = animalInCase(c, "RES-G1", RescueStatus.ADMITTED, "AN-G1", "Green Sea Turtle");
        a1.setTrackingDeviceCode("GPS-999");
        animalRepository.saveAndFlush(a1);

        Animal a2 = animalInCase(c, "RES-G2", RescueStatus.ADMITTED, "AN-G2", "Green Sea Turtle");
        a2.setTrackingDeviceCode("GPS-999");

        assertThatThrownBy(() -> animalRepository.saveAndFlush(a2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FK: tratamiento con animal inexistente")
    void foreignKeyViolation() {
        Specialist s = specialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org", "Trauma");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into treatments (animal_id, specialist_id, performed_at, type) values (?, ?, now(), 'WOUND_CARE')",
                999_999L, s.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK: status inválido en rescue_cases")
    void checkConstraintOnStatus() {
        RescueCenter c = center("DB-CAR");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id) "
                + "values ('RES-BAD', current_date, 'X', 'FLYING', ?)", c.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("UNIQUE(animal_id): un animal no puede tener dos expedientes")
    void oneToOneEnforcedByDatabase() {
        RescueCenter c = center("DB-CAR");
        Animal a = animalInCase(c, "RES-M1", RescueStatus.ADMITTED, "AN-M1", "Green Sea Turtle");
        String sql = "insert into medical_records (animal_id, initial_weight, initial_condition) values (?, 10.00, 'STABLE')";
        jdbcTemplate.update(sql, a.getId());

        assertThatThrownBy(() -> jdbcTemplate.update(sql, a.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ reto integrador
    @Test
    @DisplayName("Reto integrador: tortuga en DeepBlue Caribbean")
    void integratorChallenge() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta"));

        RescueCase rc = rescueCase(center, "RES-2026-100", RescueStatus.IN_REHABILITATION, LocalDate.of(2026, 8, 18));

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        animal.setTrackingDeviceCode("GPS-999");
        rc.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord();
        record.setInitialWeight(new BigDecimal("27.80"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Injury caused by fishing net");
        record.setObservations("Possible plastic ingestion");
        animal.assignMedicalRecord(record);

        rescueCaseRepository.saveAndFlush(rc);

        Specialist elena = specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org",
                "Marine Reptiles", "Trauma", "Rehabilitation");

        Treatment t1 = treatment(animal, elena, TreatmentType.WOUND_CARE, LocalDateTime.of(2026, 8, 19, 9, 0));
        Treatment t2 = treatment(animal, elena, TreatmentType.HYDRATION, LocalDateTime.of(2026, 8, 20, 9, 0));

        // 1
        assertThat(rescueCaseRepository.existsByCaseCode("RES-2026-100")).isTrue();
        // 2
        assertThat(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .extracting(RescueCase::getCaseCode).contains("RES-2026-100");
        // 3
        assertThat(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR")).containsExactly(animal);
        // 4
        assertThat(animalRepository.findByCommonNameContainingIgnoreCase("turtle")).containsExactly(animal);
        // 5
        assertThat(specialistRepository.findActiveByExpertise("Trauma")).containsExactly(elena);
        // 6
        assertThat(treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-2026-100"))
                .containsExactly(t1, t2);
        // 7
        assertThat(treatmentRepository.findBySpecialistExpertise("Rehabilitation")).containsExactly(t1, t2);
        // 8
        assertThat(treatmentRepository.findPerformedBetween(
                LocalDateTime.of(2026, 8, 19, 0, 0), LocalDateTime.of(2026, 8, 19, 23, 59)))
                .containsExactly(t1);
    }
}
