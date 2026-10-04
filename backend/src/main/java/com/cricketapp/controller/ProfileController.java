package com.cricketapp.controller;

import com.cricketapp.dto.ProfileResponseDto;
import com.cricketapp.dto.UpdateProfileRequestDto;
import com.cricketapp.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponseDto> getMyProfile(Principal principal) {
        ProfileResponseDto profile = profileService.getProfile(principal.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponseDto> updateMyProfile(
            Principal principal,
            @RequestBody UpdateProfileRequestDto request) {
        ProfileResponseDto updated = profileService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/me/photo")
    public ResponseEntity<ProfileResponseDto> uploadProfilePhoto(
            Principal principal,
            @RequestParam("file") MultipartFile file) {
        ProfileResponseDto updated = profileService.uploadProfilePhoto(principal.getName(), file);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/me/photo")
    public ResponseEntity<ProfileResponseDto> deleteProfilePhoto(Principal principal) {
        ProfileResponseDto updated = profileService.deleteProfilePhoto(principal.getName());
        return ResponseEntity.ok(updated);
    }
}
