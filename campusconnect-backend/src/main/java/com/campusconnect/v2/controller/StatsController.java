package com.campusconnect.v2.controller;

import com.campusconnect.model.Event;
import com.campusconnect.model.Student;
import com.campusconnect.repository.ClubRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.StudentRepository;
import com.campusconnect.v2.repository.RegistrationRepository;
import com.campusconnect.v2.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired private StudentRepository students;
    @Autowired private ClubRepository clubs;
    @Autowired private EventRepository events;
    @Autowired private RegistrationRepository registrations;
    @Autowired private TokenService tokens;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader(value = "Authorization", required = false) String auth) {
        tokens.require(auth, "teacher");

        List<Map<String, Object>> perEvent = new ArrayList<>();
        for (Event e : events.findAll()) {
            long taken = registrations.countByKindAndTargetId("EVENT", e.getId());
            long attended = registrations.findByKindAndTargetIdOrderByCreatedAtAsc("EVENT", e.getId())
                    .stream().filter(r -> r.isAttended()).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getId());
            row.put("name", e.getEventName());
            row.put("date", e.getDate());
            row.put("registered", taken);
            row.put("attended", attended);
            row.put("capacity", e.getMaximumParticipants());
            perEvent.add(row);
        }

        Map<String, Long> byDept = new TreeMap<>();
        for (Student s : students.findAll()) {
            String d = s.getDepartment() == null || s.getDepartment().isBlank() ? "Unspecified" : s.getDepartment();
            byDept.merge(d, 1L, Long::sum);
        }

        List<Map<String, Object>> perClub = new ArrayList<>();
        clubs.findAll().forEach(c -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("name", c.getClubName());
            row.put("members", registrations.countByKindAndTargetId("CLUB", c.getId()));
            perClub.add(row);
        });

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("students", students.count());
        out.put("clubs", clubs.count());
        out.put("events", events.count());
        out.put("registrations", registrations.count());
        out.put("perEvent", perEvent);
        out.put("perClub", perClub);
        out.put("studentsByDepartment", byDept);
        return out;
    }
}
