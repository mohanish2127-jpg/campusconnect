# CampusConnect (cc)

College Club and Event Management System. Diploma project.

## Folder structure

```
cc
|-- README.md
|-- VIVA_QA.md
|-- css
|   `-- style.css
|-- js
|   `-- script.js
|-- images
|   |-- clubs
|   `-- events
|-- frontend
|   |-- index.html
|   |-- student-login.html
|   |-- teacher-login.html
|   |-- student-dashboard.html
|   |-- teacher-dashboard.html
|   |-- clubs.html
|   `-- events.html
|-- database
|   `-- campusconnect.sql
`-- campusconnect-backend
    |-- pom.xml
    |-- mvnw
    |-- mvnw.cmd
    |-- .mvn/wrapper/maven-wrapper.properties
    `-- src
        |-- main
        |   |-- java/com/campusconnect
        |   |   |-- CampusconnectBackendApplication.java
        |   |   |-- DataLoader.java
        |   |   |-- model        (Student, Teacher, Club, Event)
        |   |   |-- repository   (Student, Teacher, Club, Event)
        |   |   `-- controller   (Student, Teacher, Club, Event, Login)
        |   `-- resources/application.properties
        `-- test/java/com/campusconnect/CampusconnectBackendApplicationTests.java
```

## Before the first run

1. Open `campusconnect-backend/src/main/resources/application.properties`.
2. Replace `YOUR_MYSQL_PASSWORD` with your MySQL root password.
3. Make sure the MySQL80 service is running and the `campusconnect` database exists.

## Run

```
cd campusconnect-backend
mvnw.cmd spring-boot:run
```

Then open `frontend/index.html` in the browser.

## Default logins (created on first start)

| Role | ID | Password |
|------|----|----------|
| Teacher | T101 | teacher123 |
| Student | S101 | student123 |

## Main API endpoints

| Method | URL | Purpose |
|--------|-----|---------|
| POST | /api/login/student | Student login |
| POST | /api/login/teacher | Teacher login |
| GET/POST | /api/students, /api/clubs, /api/events, /api/teachers | List and add |
| GET/PUT/DELETE | /api/{students,clubs,events,teachers}/{id} | One record |
| GET | /api/students/search?name=ab | Search students |
