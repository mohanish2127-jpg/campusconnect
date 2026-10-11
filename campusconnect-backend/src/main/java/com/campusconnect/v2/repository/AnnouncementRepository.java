package com.campusconnect.v2.repository;

import com.campusconnect.v2.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByOrderByPinnedDescCreatedAtDesc();
}
