package com.campusconnect.controller;

import com.campusconnect.model.Student;
import com.campusconnect.model.Teacher;
import com.campusconnect.repository.StudentRepository;
import com.campusconnect.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/login")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @PostMapping("/student")
    public ResponseEntity<?> studentLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Student student = studentRepository.findByStudentIdOrEmail(username, username).orElse(null);

        if (student != null && student.getPassword() != null && student.getPassword().equals(password)) {
            student.setPassword(null);
            return ResponseEntity.ok(student);
        }
        return ResponseEntity.status(401).body(Map.of("message", "Invalid student ID/email or password"));
    }

    @PostMapping("/teacher")
    public ResponseEntity<?> teacherLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Teacher teacher = teacherRepository.findByTeacherIdOrEmail(username, username).orElse(null);

        if (teacher != null && teacher.getPassword() != null && teacher.getPassword().equals(password)) {
            teacher.setPassword(null);
            return ResponseEntity.ok(teacher);
        }
        return ResponseEntity.status(401).body(Map.of("message", "Invalid teacher ID/email or password"));
    }
}
