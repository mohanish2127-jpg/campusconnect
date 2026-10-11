package com.campusconnect.controller;

import com.campusconnect.model.Student;
import com.campusconnect.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    private Student hidePassword(Student student) {
        student.setPassword(null);
        return student;
    }

    @PostMapping
    public Student addStudent(@RequestBody Student student) {
        Student saved = studentRepository.save(student);
        return hidePassword(saved);
    }

    @GetMapping
    public List<Student> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        students.forEach(this::hidePassword);
        return students;
    }

    @GetMapping("/search")
    public List<Student> searchStudents(@RequestParam String name) {
        List<Student> students = studentRepository.findByNameContainingIgnoreCase(name);
        students.forEach(this::hidePassword);
        return students;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(s -> ResponseEntity.ok(hidePassword(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student updated) {
        return studentRepository.findById(id).map(student -> {
            student.setStudentId(updated.getStudentId());
            student.setName(updated.getName());
            student.setEmail(updated.getEmail());
            student.setPhone(updated.getPhone());
            student.setDepartment(updated.getDepartment());
            student.setSemester(updated.getSemester());
            if (updated.getPassword() != null && !updated.getPassword().isEmpty()) {
                student.setPassword(updated.getPassword());
            }
            Student saved = studentRepository.save(student);
            return ResponseEntity.ok(hidePassword(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        if (!studentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        studentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
