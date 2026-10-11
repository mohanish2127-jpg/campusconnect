package com.campusconnect.v2.controller;

import com.campusconnect.v2.model.Announcement;
import com.campusconnect.v2.repository.AnnouncementRepository;
import com.campusconnect.v2.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/announcements")
@CrossOrigin(origins = "*")
public class AnnouncementController {

    @Autowired private AnnouncementRepository repository;
    @Autowired private TokenService tokens;

    @GetMapping
    public List<Announcement> list() {
        return repository.findAllByOrderByPinnedDescCreatedAtDesc();
    }

    @PostMapping
    public Announcement create(@RequestHeader(value = "Authorization", required = false) String auth,
                               @RequestBody Map<String, Object> body) {
        TokenService.Claims me = tokens.require(auth, "teacher");
        String title = body.get("title") == null ? "" : body.get("title").toString().trim();
        String message = body.get("message") == null ? "" : body.get("message").toString().trim();
        if (title.isEmpty() || message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title and message are required");
        }
        if (title.length() > 200 || message.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title or message is too long");
        }
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setMessage(message);
        a.setPinned(Boolean.TRUE.equals(body.get("pinned")));
        a.setPostedBy(me.name());
        return repository.save(a);
    }

    @PutMapping("/{id}/pin")
    public Announcement pin(@RequestHeader(value = "Authorization", required = false) String auth,
                            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        tokens.require(auth, "teacher");
        Announcement a = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
        a.setPinned(Boolean.TRUE.equals(body.get("pinned")));
        return repository.save(a);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @PathVariable Long id) {
        tokens.require(auth, "teacher");
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found");
        }
        repository.deleteById(id);
        return Map.of("message", "Deleted");
    }
}
