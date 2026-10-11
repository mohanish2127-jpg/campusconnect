package com.campusconnect.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clubs")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String clubName;
    private String description;
    private String department;
    private String teacherCoordinator;
    private String googleFormLink;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTeacherCoordinator() { return teacherCoordinator; }
    public void setTeacherCoordinator(String teacherCoordinator) { this.teacherCoordinator = teacherCoordinator; }

    public String getGoogleFormLink() { return googleFormLink; }
    public void setGoogleFormLink(String googleFormLink) { this.googleFormLink = googleFormLink; }
}
