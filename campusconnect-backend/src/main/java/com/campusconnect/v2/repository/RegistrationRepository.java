package com.campusconnect.v2.repository;

import com.campusconnect.v2.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByStudentIdOrderByCreatedAtDesc(String studentId);

    List<Registration> findByKindAndTargetIdOrderByCreatedAtAsc(String kind, Long targetId);

    List<Registration> findByKind(String kind);

    long countByKindAndTargetId(String kind, Long targetId);

    Optional<Registration> findByKindAndTargetIdAndStudentId(String kind, Long targetId, String studentId);

    void deleteByKindAndTargetId(String kind, Long targetId);
}
