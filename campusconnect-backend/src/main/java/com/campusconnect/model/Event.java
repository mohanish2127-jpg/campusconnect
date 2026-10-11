package com.campusconnect.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventName;
    private String description;
    private String date;
    private String time;
    private String venue;
    private Integer maximumParticipants;
    private String googleFormLink;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public Integer getMaximumParticipants() { return maximumParticipants; }
    public void setMaximumParticipants(Integer maximumParticipants) { this.maximumParticipants = maximumParticipants; }

    public String getGoogleFormLink() { return googleFormLink; }
    public void setGoogleFormLink(String googleFormLink) { this.googleFormLink = googleFormLink; }
}
