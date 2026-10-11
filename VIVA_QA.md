# CampusConnect - Viva Questions and Answers

| # | Question | Short Answer |
|---|----------|--------------|
| 1 | What is CampusConnect? | A College Club and Event Management System. Teachers manage students, clubs and events; students log in, view clubs and events, and join or register through Google Forms. |
| 2 | Why did you choose this project? | Clubs and events in college are usually managed on paper or WhatsApp. One system keeps all information in one place and is easy to update. |
| 3 | What is Java? | A object-oriented, platform-independent programming language. Our backend is written in Java. |
| 4 | What is Spring Boot? | A Java framework that creates a ready-to-run web application with an embedded Tomcat server and very little configuration. |
| 5 | What is MySQL? | An open-source relational database that stores data in tables. |
| 6 | What is SQL? | Structured Query Language, used to create, read, update and delete data in a relational database. |
| 7 | What is JPA? | Java Persistence API, a standard that lets Java objects be saved to database tables without writing SQL by hand. |
| 8 | What is Hibernate? | The tool that implements JPA. It converts Java objects to SQL queries and creates the tables automatically. |
| 9 | What is a REST API? | A way for the frontend and backend to talk over HTTP using URLs and methods like GET, POST, PUT and DELETE. Data is exchanged as JSON. |
| 10 | What is a Controller? | A Java class marked with @RestController that receives HTTP requests and sends back responses. |
| 11 | What is a Repository? | An interface that extends JpaRepository. It gives ready-made methods such as save, findAll, findById and deleteById. |
| 12 | How does the frontend talk to the backend? | JavaScript uses fetch() to send HTTP requests to Spring Boot at http://localhost:8080/api. The frontend never connects to MySQL directly. |
| 13 | How does the backend talk to MySQL? | The controller calls the repository, JPA and Hibernate convert the call to SQL, and the MySQL JDBC driver runs it on the campusconnect database. |
| 14 | What is an Entity? | A Java class that represents a database table. Each object is one row. |
| 15 | What is @Entity? | An annotation that tells JPA this class should be mapped to a table. |
| 16 | What is @Id? | It marks the primary key field of the entity. |
| 17 | What is @GeneratedValue? | It makes the database generate the id automatically. IDENTITY uses MySQL auto-increment. |
| 18 | Why MySQL instead of MongoDB? | The data is structured (students, teachers, clubs, events), and the syllabus requires SQL. MySQL fits fixed tables and is easy to explain. |
| 19 | What are the tables? | students, teachers, clubs and events. |
| 20 | What is CRUD? | Create, Read, Update, Delete. The teacher dashboard performs all four on students, clubs and events. |
| 21 | What is GET? | Reads data. Example: GET /api/clubs returns all clubs. |
| 22 | What is POST? | Creates new data. Example: POST /api/students adds a student. |
| 23 | What is PUT? | Updates existing data. Example: PUT /api/events/3 edits event 3. |
| 24 | What is DELETE? | Removes data. Example: DELETE /api/clubs/2 deletes club 2. |
| 25 | Why are Google Forms used? | They collect club applications and event registrations without extra tables or backend code. The form link is stored in the database and opened from a button. |
| 26 | What are the limitations? | Passwords are stored as plain text, login does not use tokens, there is no record of who joined which club inside the system, and event date and time are plain text. |
| 27 | What improvements can be added? | Password hashing, token-based login, membership and registration tables, email notifications, an admin role and event seat tracking. |

## Extra points the examiner may ask

- **Architecture:** Frontend (HTML, CSS, Bootstrap, JavaScript) -> Controller -> Repository -> MySQL.
- **Login:** The user sends ID or email plus password to /api/login/student or /api/login/teacher. The server checks the database and returns the user without the password.
- **Where is the session kept?** In the browser's localStorage after login. Logout clears it.
- **How are passwords hidden?** Controllers set the password to null before sending any student or teacher to the frontend.
- **What is CORS?** A browser rule for requests between different origins. @CrossOrigin allows the HTML pages to call the Spring Boot API.
- **Default login details:** Teacher T101 / teacher123, Student S101 / student123 (created on first start if the tables are empty).
