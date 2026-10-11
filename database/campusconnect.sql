USE campusconnect;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(255),
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    department VARCHAR(255),
    semester VARCHAR(255),
    password VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS teachers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id VARCHAR(255),
    name VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS clubs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_name VARCHAR(255),
    description VARCHAR(255),
    department VARCHAR(255),
    teacher_coordinator VARCHAR(255),
    google_form_link VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_name VARCHAR(255),
    description VARCHAR(255),
    date VARCHAR(255),
    time VARCHAR(255),
    venue VARCHAR(255),
    maximum_participants INT,
    google_form_link VARCHAR(255)
);

SHOW TABLES;
