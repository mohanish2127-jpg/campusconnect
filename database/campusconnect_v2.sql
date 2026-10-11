USE campusconnect;

CREATE TABLE IF NOT EXISTS registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kind VARCHAR(10) NOT NULL,
    target_id BIGINT NOT NULL,
    student_id VARCHAR(255) NOT NULL,
    student_name VARCHAR(255),
    department VARCHAR(255),
    semester VARCHAR(255),
    attended BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6),
    UNIQUE KEY uk_registration (kind, target_id, student_id)
);

CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    message VARCHAR(2000),
    posted_by VARCHAR(255),
    pinned BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6)
);
