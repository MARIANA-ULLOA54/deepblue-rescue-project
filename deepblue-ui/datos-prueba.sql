-- Datos de prueba (ejecutar en la base deepblue despues de que Flyway cree las tablas)
INSERT INTO rescue_centers (code, name, city) VALUES ('RC-CTR-01', 'Centro Caribe', 'Santa Marta');
INSERT INTO specialists (professional_code, first_name, last_name, email, active) VALUES ('SP-001', 'Laura', 'Gomez', 'laura.gomez@deepblue.org', TRUE);
INSERT INTO rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id) VALUES
  ('CASE-001', '2026-10-01', 'Playa Rodadero', 'ADMITTED', 1),
  ('CASE-002', '2026-10-05', 'Bahia de Taganga', 'UNDER_EVALUATION', 1),
  ('CASE-003', '2026-10-08', 'Isla de Salamanca', 'IN_REHABILITATION', 1);
INSERT INTO animals (animal_code, common_name, scientific_name, sex, rescue_case_id) VALUES
  ('AN-001', 'Tortuga verde', 'Chelonia mydas', 'FEMALE', 1),
  ('AN-002', 'Delfin nariz de botella', 'Tursiops truncatus', 'MALE', 2),
  ('AN-003', 'Pelicano pardo', 'Pelecanus occidentalis', 'UNKNOWN', 3);
INSERT INTO medical_records (animal_id, initial_weight, initial_condition, injuries, observations) VALUES
  (1, 85.50, 'Debil', 'Herida en aleta', 'Enredada en red de pesca'),
  (2, 180.00, 'Estable', NULL, 'Varamiento'),
  (3, 3.20, 'Critico', 'Fractura en ala', NULL);
