package com.campusconnect.controller;

import com.campusconnect.model.Teacher;
import com.campusconnect.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    private Teacher hidePassword(Teacher teacher) {
        teacher.setPassword(null);
        return teacher;
    }

    @PostMapping
    public Teacher addTeacher(@RequestBody Teacher teacher) {
        Teacher saved = teacherRepository.save(teacher);
        return hidePassword(saved);
    }

    @GetMapping
    public List<Teacher> getAllTeachers() {
        List<Teacher> teachers = teacherRepository.findAll();
        teachers.forEach(this::hidePassword);
        return teachers;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Teacher> getTeacherById(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .map(t -> ResponseEntity.ok(hidePassword(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Teacher> updateTeacher(@PathVariable Long id, @RequestBody Teacher updated) {
        return teacherRepository.findById(id).map(teacher -> {
            teacher.setTeacherId(updated.getTeacherId());
            teacher.setName(updated.getName());
            teacher.setEmail(updated.getEmail());
            if (updated.getPassword() != null && !updated.getPassword().isEmpty()) {
                teacher.setPassword(updated.getPassword());
            }
            Teacher saved = teacherRepository.save(teacher);
            return ResponseEntity.ok(hidePassword(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        if (!teacherRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        teacherRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
