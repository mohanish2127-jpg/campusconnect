package com.campusconnect.v2.controller;

import com.campusconnect.model.Club;
import com.campusconnect.model.Event;
import com.campusconnect.model.Student;
import com.campusconnect.repository.ClubRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.StudentRepository;
import com.campusconnect.v2.model.Registration;
import com.campusconnect.v2.repository.RegistrationRepository;
import com.campusconnect.v2.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = "*")
public class RegistrationController {

    private static final Object LOCK = new Object();

    @Autowired private RegistrationRepository registrations;
    @Autowired private EventRepository events;
    @Autowired private ClubRepository clubs;
    @Autowired private StudentRepository students;
    @Autowired private TokenService tokens;

    private static ResponseStatusException status(HttpStatus s, String message) {
        return new ResponseStatusException(s, message);
    }

    private static String eventStatus(Event e) {
        try {
            LocalDate d = LocalDate.parse(e.getDate() == null ? "" : e.getDate().trim());
            LocalDate today = LocalDate.now();
            if (d.isBefore(today)) return "PAST";
            return d.isEqual(today) ? "TODAY" : "UPCOMING";
        } catch (DateTimeParseException ex) {
            return "UNKNOWN";
        }
    }

    @GetMapping("/events/overview")
    public List<Map<String, Object>> eventsOverview(@RequestHeader(value = "Authorization", required = false) String auth) {
        TokenService.Claims me = tokens.verify(auth);
        Set<Long> mine = new HashSet<>();
        if ("student".equals(me.role())) {
            registrations.findByStudentIdOrderByCreatedAtDesc(me.id()).stream()
                    .filter(r -> "EVENT".equals(r.getKind())).forEach(r -> mine.add(r.getTargetId()));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Event e : events.findAll()) {
            long taken = registrations.countByKindAndTargetId("EVENT", e.getId());
            Integer max = e.getMaximumParticipants();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("event", e);
            row.put("registered", taken);
            row.put("seatsLeft", max == null || max <= 0 ? null : Math.max(0, max - taken));
            row.put("full", max != null && max > 0 && taken >= max);
            row.put("status", eventStatus(e));
            row.put("joinedByMe", mine.contains(e.getId()));
            out.add(row);
        }
        out.sort(Comparator.comparing((Map<String, Object> r) -> "PAST".equals(r.get("status")))
                .thenComparing(r -> ((Event) r.get("event")).getDate() == null ? "" : ((Event) r.get("event")).getDate()));
        return out;
    }

    @GetMapping("/clubs/overview")
    public List<Map<String, Object>> clubsOverview(@RequestHeader(value = "Authorization", required = false) String auth) {
        TokenService.Claims me = tokens.verify(auth);
        Set<Long> mine = new HashSet<>();
        if ("student".equals(me.role())) {
            registrations.findByStudentIdOrderByCreatedAtDesc(me.id()).stream()
                    .filter(r -> "CLUB".equals(r.getKind())).forEach(r -> mine.add(r.getTargetId()));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Club c : clubs.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("club", c);
            row.put("members", registrations.countByKindAndTargetId("CLUB", c.getId()));
            row.put("joinedByMe", mine.contains(c.getId()));
            out.add(row);
        }
        return out;
    }

    @PostMapping("/registrations")
    public Registration register(@RequestHeader(value = "Authorization", required = false) String auth,
                                 @RequestBody Map<String, Object> body) {
        TokenService.Claims me = tokens.require(auth, "student");
        String kind = String.valueOf(body.get("kind")).toUpperCase();
        if (!kind.equals("EVENT") && !kind.equals("CLUB") || body.get("targetId") == null) {
            throw status(HttpStatus.BAD_REQUEST, "kind (EVENT or CLUB) and targetId are required");
        }
        Long targetId;
        try {
            targetId = Long.valueOf(body.get("targetId").toString());
        } catch (NumberFormatException e) {
            throw status(HttpStatus.BAD_REQUEST, "targetId must be a number");
        }
        Student student = students.findByStudentIdOrEmail(me.id(), me.id())
                .orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Student account not found"));

        synchronized (LOCK) {
            if (kind.equals("EVENT")) {
                Event e = events.findById(targetId).orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Event not found"));
                if ("PAST".equals(eventStatus(e))) throw status(HttpStatus.CONFLICT, "This event is already over");
                Integer max = e.getMaximumParticipants();
                if (max != null && max > 0 && registrations.countByKindAndTargetId("EVENT", targetId) >= max) {
                    throw status(HttpStatus.CONFLICT, "Sorry, this event is full");
                }
            } else if (!clubs.existsById(targetId)) {
                throw status(HttpStatus.NOT_FOUND, "Club not found");
            }
            if (registrations.findByKindAndTargetIdAndStudentId(kind, targetId, me.id()).isPresent()) {
                throw status(HttpStatus.CONFLICT, "You have already registered");
            }
            Registration r = new Registration();
            r.setKind(kind);
            r.setTargetId(targetId);
            r.setStudentId(student.getStudentId());
            r.setStudentName(student.getName());
            r.setDepartment(student.getDepartment());
            r.setSemester(student.getSemester());
            return registrations.save(r);
        }
    }

    @GetMapping("/registrations/mine")
    public List<Map<String, Object>> mine(@RequestHeader(value = "Authorization", required = false) String auth) {
        TokenService.Claims me = tokens.require(auth, "student");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Registration r : registrations.findByStudentIdOrderByCreatedAtDesc(me.id())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("registration", r);
            if (r.getKind().equals("EVENT")) {
                events.findById(r.getTargetId()).ifPresent(e -> {
                    row.put("title", e.getEventName());
                    row.put("date", e.getDate());
                    row.put("time", e.getTime());
                    row.put("venue", e.getVenue());
                    row.put("status", eventStatus(e));
                });
            } else {
                clubs.findById(r.getTargetId()).ifPresent(c -> row.put("title", c.getClubName()));
            }
            row.putIfAbsent("title", "(removed)");
            out.add(row);
        }
        return out;
    }

    @DeleteMapping("/registrations/{id}")
    public Map<String, String> cancel(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @PathVariable Long id) {
        TokenService.Claims me = tokens.verify(auth);
        Registration r = registrations.findById(id).orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Registration not found"));
        if ("student".equals(me.role()) && !r.getStudentId().equals(me.id())) {
            throw status(HttpStatus.FORBIDDEN, "This is not your registration");
        }
        registrations.delete(r);
        return Map.of("message", "Registration cancelled");
    }

    @GetMapping("/registrations/{kind}/{targetId}")
    public List<Registration> forTarget(@RequestHeader(value = "Authorization", required = false) String auth,
                                        @PathVariable String kind, @PathVariable Long targetId) {
        tokens.require(auth, "teacher");
        return registrations.findByKindAndTargetIdOrderByCreatedAtAsc(kind.toUpperCase(), targetId);
    }

    @PutMapping("/registrations/{id}/attendance")
    public Registration attendance(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        tokens.require(auth, "teacher");
        Registration r = registrations.findById(id).orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Registration not found"));
        r.setAttended(Boolean.TRUE.equals(body.get("attended")));
        return registrations.save(r);
    }

    private static String csv(Object v) {
        String s = v == null ? "" : v.toString();
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    @GetMapping("/registrations/{kind}/{targetId}/export.csv")
    public ResponseEntity<byte[]> export(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @PathVariable String kind, @PathVariable Long targetId) {
        tokens.require(auth, "teacher");
        StringBuilder sb = new StringBuilder("Student ID,Name,Department,Semester,Registered At,Attended\n");
        for (Registration r : registrations.findByKindAndTargetIdOrderByCreatedAtAsc(kind.toUpperCase(), targetId)) {
            sb.append(csv(r.getStudentId())).append(',').append(csv(r.getStudentName())).append(',')
                    .append(csv(r.getDepartment())).append(',').append(csv(r.getSemester())).append(',')
                    .append(csv(r.getCreatedAt())).append(',').append(r.isAttended() ? "Yes" : "No").append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + kind.toLowerCase() + "-" + targetId + "-registrations.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
