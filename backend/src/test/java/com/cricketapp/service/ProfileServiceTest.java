package com.cricketapp.service;

import com.cricketapp.dto.ProfileResponseDto;
import com.cricketapp.dto.UpdateProfileRequestDto;
import com.cricketapp.entity.*;
import com.cricketapp.exception.AuthException;
import com.cricketapp.repository.PlayerProfileRepository;
import com.cricketapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProfileServiceTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    public void setup() {
        profileRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user (Stage 2 setup)
        testUser = new User("CRK102847", "Srinivasulu", "srinivas@example.com", passwordEncoder.encode("Password123"), true);
        testUser = userRepository.save(testUser);
    }

    @Test
    public void test1_AuthenticatedUserCanRetrieveProfile() {
        ProfileResponseDto profile = profileService.getProfile("srinivas@example.com");

        assertNotNull(profile);
        assertEquals("CRK102847", profile.getUserId());
        assertEquals("Srinivasulu", profile.getName());
        assertEquals("srinivas@example.com", profile.getEmail());
    }

    @Test
    public void test2_UnauthenticatedUserCannotRetrieveProfile() {
        assertThrows(AuthException.class, () -> profileService.getProfile("nonexistent@example.com"));
    }

    @Test
    public void test3_AuthenticatedUserCanUpdateProfile() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "Srinivas K",
                LocalDate.of(2000, 5, 15),
                Gender.MALE,
                "Nellore, AP",
                PlayingRole.ALL_ROUNDER,
                BattingStyle.RIGHT_HANDED,
                BowlingStyle.RIGHT_ARM_MEDIUM,
                "Passionate cricketer and all-rounder."
        );

        ProfileResponseDto updated = profileService.updateProfile("srinivas@example.com", request);

        assertEquals("Srinivas K", updated.getName());
        assertEquals(LocalDate.of(2000, 5, 15), updated.getDateOfBirth());
        assertEquals(Gender.MALE, updated.getGender());
        assertEquals("Nellore, AP", updated.getLocation());
        assertEquals(PlayingRole.ALL_ROUNDER, updated.getPlayingRole());
        assertEquals(BattingStyle.RIGHT_HANDED, updated.getBattingStyle());
        assertEquals(BowlingStyle.RIGHT_ARM_MEDIUM, updated.getBowlingStyle());
        assertEquals("Passionate cricketer and all-rounder.", updated.getBio());
    }

    @Test
    public void test4_UserCannotChangeCricketUserIdThroughProfileUpdate() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "Srinivas K", LocalDate.of(2000, 1, 1), Gender.MALE, "Location",
                PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.NONE, "Bio"
        );

        ProfileResponseDto updated = profileService.updateProfile("srinivas@example.com", request);

        // Permanent CRK102847 must remain unchanged
        assertEquals("CRK102847", updated.getUserId());
    }

    @Test
    public void test5_UserCannotChangeEmailThroughProfileUpdate() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "Srinivas K", LocalDate.of(2000, 1, 1), Gender.MALE, "Location",
                PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.NONE, "Bio"
        );

        ProfileResponseDto updated = profileService.updateProfile("srinivas@example.com", request);

        // Email must remain srinivas@example.com
        assertEquals("srinivas@example.com", updated.getEmail());
    }

    @Test
    public void test6_InvalidDateOfBirthIsRejected() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "Srinivas K",
                LocalDate.now().plusDays(1), // Future date!
                Gender.MALE, "Location",
                PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.NONE, "Bio"
        );

        AuthException exception = assertThrows(AuthException.class, () ->
                profileService.updateProfile("srinivas@example.com", request));

        assertTrue(exception.getMessage().contains("future date"));
    }

    @Test
    public void test7_BlankNameIsRejected() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "", // Blank name!
                LocalDate.of(2000, 1, 1), Gender.MALE, "Location",
                PlayingRole.BATTER, BattingStyle.RIGHT_HANDED, BowlingStyle.NONE, "Bio"
        );

        AuthException exception = assertThrows(AuthException.class, () ->
                profileService.updateProfile("srinivas@example.com", request));

        assertTrue(exception.getMessage().contains("Full name cannot be blank"));
    }

    @Test
    public void test8_OversizedOrInvalidProfileImageIsRejected() {
        // Invalid type (text/plain)
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());
        AuthException typeException = assertThrows(AuthException.class, () ->
                profileService.uploadProfilePhoto("srinivas@example.com", txtFile));
        assertTrue(typeException.getMessage().contains("Invalid file type"));

        // Oversized file (> 5 MB)
        byte[] largeBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);
        AuthException sizeException = assertThrows(AuthException.class, () ->
                profileService.uploadProfilePhoto("srinivas@example.com", largeFile));
        assertTrue(sizeException.getMessage().contains("exceeds maximum limit"));
    }

    @Test
    public void test9_ValidImageUploadSucceeds() {
        byte[] imgBytes = "fake-png-content".getBytes();
        MockMultipartFile pngFile = new MockMultipartFile("file", "avatar.png", "image/png", imgBytes);

        ProfileResponseDto response = profileService.uploadProfilePhoto("srinivas@example.com", pngFile);

        assertNotNull(response.getProfilePhotoUrl());
        assertTrue(response.getProfilePhotoUrl().startsWith("/uploads/profiles/"));
    }

    @Test
    public void test10_ExistingRegisteredUsersCanAccessProfileWhenFieldsIncomplete() {
        // User created without profile row yet
        ProfileResponseDto profile = profileService.getProfile("srinivas@example.com");

        assertNotNull(profile);
        assertEquals("CRK102847", profile.getUserId());
        assertNull(profile.getDateOfBirth());
        assertNull(profile.getPlayingRole());
        assertNull(profile.getBio());
    }
}
