-- 1. Tablas independientes / catálogo
CREATE TABLE rescue_centers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL
);

CREATE TABLE expertise (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE specialists (
    id BIGSERIAL PRIMARY KEY,
    professional_code VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Casos de Rescate (N:1 con RescueCenter)
CREATE TABLE rescue_cases (
    id BIGSERIAL PRIMARY KEY,
    case_code VARCHAR(50) NOT NULL UNIQUE,
    rescue_date DATE NOT NULL,
    rescue_location VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL,
    rescue_center_id BIGINT NOT NULL,
    CONSTRAINT fk_rescue_cases_center FOREIGN KEY (rescue_center_id) REFERENCES rescue_centers(id),
    CONSTRAINT chk_rescue_case_status CHECK (status IN (
        'ADMITTED', 'UNDER_EVALUATION', 'IN_REHABILITATION', 
        'READY_FOR_RELEASE', 'RELEASED', 'CLOSED'
    ))
);

-- 3. Animales (1:1 con RescueCase via UNIQUE en FK)
CREATE TABLE animals (
    id BIGSERIAL PRIMARY KEY,
    animal_code VARCHAR(50) NOT NULL UNIQUE,
    common_name VARCHAR(100) NOT NULL,
    scientific_name VARCHAR(100) NOT NULL,
    sex VARCHAR(20) NOT NULL,
    rescue_case_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_animals_rescue_case FOREIGN KEY (rescue_case_id) REFERENCES rescue_cases(id)
);

-- 4. Expedientes Médicos (1:1 con Animal via UNIQUE en FK)
CREATE TABLE medical_records (
    id BIGSERIAL PRIMARY KEY,
    animal_id BIGINT NOT NULL UNIQUE,
    initial_weight DECIMAL(10, 2) NOT NULL,
    initial_condition VARCHAR(100) NOT NULL,
    injuries TEXT,
    observations TEXT,
    CONSTRAINT fk_medical_records_animal FOREIGN KEY (animal_id) REFERENCES animals(id)
);

-- 5. Tabla Intermedia Specialist - Expertise (N:M)
CREATE TABLE specialist_expertise (
    specialist_id BIGINT NOT NULL,
    expertise_id BIGINT NOT NULL,
    PRIMARY KEY (specialist_id, expertise_id),
    CONSTRAINT fk_se_specialist FOREIGN KEY (specialist_id) REFERENCES specialists(id) ON DELETE CASCADE,
    CONSTRAINT fk_se_expertise FOREIGN KEY (expertise_id) REFERENCES expertise(id) ON DELETE CASCADE
);

-- 6. Tratamientos (N:1 con Animal y N:1 con Specialist)
CREATE TABLE treatments (
    id BIGSERIAL PRIMARY KEY,
    animal_id BIGINT NOT NULL,
    specialist_id BIGINT NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    CONSTRAINT fk_treatments_animal FOREIGN KEY (animal_id) REFERENCES animals(id),
    CONSTRAINT fk_treatments_specialist FOREIGN KEY (specialist_id) REFERENCES specialists(id)
);

-- 7. Índices
CREATE INDEX idx_rescue_cases_center ON rescue_cases(rescue_center_id);
CREATE INDEX idx_rescue_cases_status ON rescue_cases(status);
CREATE INDEX idx_rescue_cases_date ON rescue_cases(rescue_date);
CREATE INDEX idx_treatments_animal ON treatments(animal_id);
CREATE INDEX idx_treatments_specialist ON treatments(specialist_id);
CREATE INDEX idx_treatments_performed ON treatments(performed_at);