package com.campusconnect.repository;

import com.campusconnect.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByTeacherIdOrEmail(String teacherId, String email);
}
