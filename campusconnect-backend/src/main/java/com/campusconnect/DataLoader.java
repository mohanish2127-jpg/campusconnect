package com.campusconnect;

import com.campusconnect.model.Student;
import com.campusconnect.model.Teacher;
import com.campusconnect.repository.StudentRepository;
import com.campusconnect.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public void run(String... args) {
        if (teacherRepository.count() == 0) {
            Teacher teacher = new Teacher();
            teacher.setTeacherId("T101");
            teacher.setName("Admin Teacher");
            teacher.setEmail("teacher@campusconnect.com");
            teacher.setPassword("teacher123");
            teacherRepository.save(teacher);
        }

        if (studentRepository.count() == 0) {
            Student student = new Student();
            student.setStudentId("S101");
            student.setName("Demo Student");
            student.setEmail("student@campusconnect.com");
            student.setPhone("9999999999");
            student.setDepartment("Computer Engineering");
            student.setSemester("5");
            student.setPassword("student123");
            studentRepository.save(student);
        }
    }
}
