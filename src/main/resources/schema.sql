CREATE TABLE IF NOT EXISTS patients (
    id             INT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    blood_pressure VARCHAR(10),
    temperature    DECIMAL(4,1),
    heart_rate     INT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS clinical_notes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id  INT NOT NULL,
    author      VARCHAR(100) NOT NULL,
    note        TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notes_patient (patient_id),
    FOREIGN KEY (patient_id) REFERENCES patients(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medicines (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL UNIQUE,
    quantity         INT NOT NULL,
    total_dispensed  INT NOT NULL DEFAULT 0,
    total_rejected   INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_quantity_nonneg CHECK (quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dispensing_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_id  INT NOT NULL,
    actor        VARCHAR(100) NOT NULL,
    patient_id   INT NULL,
    doses        INT NOT NULL,
    outcome      VARCHAR(20) NOT NULL,
    logged_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lab_requests (
    id            INT PRIMARY KEY,
    patient_id    INT NOT NULL,
    test_type     VARCHAR(100) NOT NULL,
    requested_by  VARCHAR(100) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_by  VARCHAR(100) NULL,
    result        TEXT NULL,
    completed_at  TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (patient_id) REFERENCES patients(id)
) ENGINE=InnoDB;
