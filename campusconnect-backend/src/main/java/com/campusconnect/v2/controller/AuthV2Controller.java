package com.campusconnect.v2.controller;

import com.campusconnect.model.Student;
import com.campusconnect.model.Teacher;
import com.campusconnect.repository.StudentRepository;
import com.campusconnect.repository.TeacherRepository;
import com.campusconnect.v2.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/auth")
@CrossOrigin(origins = "*")
public class AuthV2Controller {

    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private TokenService tokens;

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String role = clean(body.get("role"));
        String username = clean(body.get("username"));
        String password = body.get("password");

        Map<String, Object> out = new LinkedHashMap<>();
        if ("student".equals(role)) {
            Student s = studentRepository.findByStudentIdOrEmail(username, username).orElse(null);
            if (s != null && s.getPassword() != null && s.getPassword().equals(password)) {
                s.setPassword(null);
                out.put("token", tokens.issue("student", s.getStudentId(), s.getName()));
                out.put("role", "student");
                out.put("user", s);
                return out;
            }
        } else if ("teacher".equals(role)) {
            Teacher t = teacherRepository.findByTeacherIdOrEmail(username, username).orElse(null);
            if (t != null && t.getPassword() != null && t.getPassword().equals(password)) {
                t.setPassword(null);
                out.put("token", tokens.issue("teacher", t.getTeacherId(), t.getName()));
                out.put("role", "teacher");
                out.put("user", t);
                return out;
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID/email or password");
    }

    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody Map<String, String> body) {
        String studentId = clean(body.get("studentId"));
        String name = clean(body.get("name"));
        String email = clean(body.get("email"));
        String password = body.get("password");

        if (studentId.isEmpty() || name.isEmpty() || email.isEmpty()) {
            throw bad("Student ID, name and email are required");
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw bad("Enter a valid email address");
        }
        if (password == null || password.length() < 6) {
            throw bad("Password must be at least 6 characters");
        }
        if (studentRepository.findByStudentIdOrEmail(studentId, email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student ID or email is already registered");
        }

        Student s = new Student();
        s.setStudentId(studentId);
        s.setName(name);
        s.setEmail(email);
        s.setPhone(clean(body.get("phone")));
        s.setDepartment(clean(body.get("department")));
        s.setSemester(clean(body.get("semester")));
        s.setPassword(password);
        studentRepository.save(s);
        s.setPassword(null);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", tokens.issue("student", s.getStudentId(), s.getName()));
        out.put("role", "student");
        out.put("user", s);
        return out;
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @RequestBody Map<String, String> body) {
        TokenService.Claims me = tokens.verify(auth);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            throw bad("New password must be at least 6 characters");
        }

        if ("student".equals(me.role())) {
            Student s = studentRepository.findByStudentIdOrEmail(me.id(), me.id()).orElseThrow(() -> bad("Account not found"));
            if (s.getPassword() == null || !s.getPassword().equals(oldPassword)) throw bad("Current password is wrong");
            s.setPassword(newPassword);
            studentRepository.save(s);
        } else {
            Teacher t = teacherRepository.findByTeacherIdOrEmail(me.id(), me.id()).orElseThrow(() -> bad("Account not found"));
            if (t.getPassword() == null || !t.getPassword().equals(oldPassword)) throw bad("Current password is wrong");
            t.setPassword(newPassword);
            teacherRepository.save(t);
        }
        return Map.of("message", "Password changed");
    }
}
