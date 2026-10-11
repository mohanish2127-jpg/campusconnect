package com.campusconnect.controller;

import com.campusconnect.model.Club;
import com.campusconnect.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@CrossOrigin(origins = "*")
public class ClubController {

    @Autowired
    private ClubRepository clubRepository;

    @PostMapping
    public Club addClub(@RequestBody Club club) {
        return clubRepository.save(club);
    }

    @GetMapping
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Club> getClubById(@PathVariable Long id) {
        return clubRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Club> updateClub(@PathVariable Long id, @RequestBody Club updated) {
        return clubRepository.findById(id).map(club -> {
            club.setClubName(updated.getClubName());
            club.setDescription(updated.getDescription());
            club.setDepartment(updated.getDepartment());
            club.setTeacherCoordinator(updated.getTeacherCoordinator());
            club.setGoogleFormLink(updated.getGoogleFormLink());
            return ResponseEntity.ok(clubRepository.save(club));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClub(@PathVariable Long id) {
        if (!clubRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        clubRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
