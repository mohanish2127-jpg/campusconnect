package com.campusconnect.repository;

import com.campusconnect.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByNameContainingIgnoreCase(String name);

    Optional<Student> findByStudentIdOrEmail(String studentId, String email);
}
