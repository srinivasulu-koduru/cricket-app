package com.cricketapp.controller;

import com.cricketapp.dto.PublicPlayerProfileResponseDto;
import com.cricketapp.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final ProfileService profileService;

    public PlayerController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<PublicPlayerProfileResponseDto>> searchPlayers(@RequestParam(value = "q", required = false) String query) {
        List<PublicPlayerProfileResponseDto> results = profileService.searchPlayers(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getPublicPlayerProfile(
            @PathVariable("userId") String userId,
            @RequestParam(value = "q", required = false) String query) {

        if ("search".equalsIgnoreCase(userId)) {
            List<PublicPlayerProfileResponseDto> results = profileService.searchPlayers(query);
            return ResponseEntity.ok(results);
        }

        PublicPlayerProfileResponseDto publicProfile = profileService.getPublicPlayerProfile(userId);
        return ResponseEntity.ok(publicProfile);
    }

    @GetMapping("/{userId}/public-profile")
    public ResponseEntity<PublicPlayerProfileResponseDto> getPublicProfileExplicit(@PathVariable("userId") String userId) {
        PublicPlayerProfileResponseDto publicProfile = profileService.getPublicPlayerProfile(userId);
        return ResponseEntity.ok(publicProfile);
    }
}
