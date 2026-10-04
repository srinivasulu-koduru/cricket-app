package com.cricketapp.service;

import com.cricketapp.dto.*;
import com.cricketapp.entity.User;
import com.cricketapp.exception.AuthException;
import com.cricketapp.repository.OtpVerificationRepository;
import com.cricketapp.repository.PendingRegistrationRepository;
import com.cricketapp.repository.UserRepository;
import com.cricketapp.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private PendingRegistrationRepository pendingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @SpyBean
    private EmailService emailService;

    private String lastCapturedOtp;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        otpRepository.deleteAll();
        pendingRepository.deleteAll();
        lastCapturedOtp = null;

        // Capture raw OTP passed to EmailService
        doAnswer(invocation -> {
            lastCapturedOtp = invocation.getArgument(1, String.class);
            return null;
        }).when(emailService).sendOtpEmail(anyString(), anyString(), anyString(), anyInt());

        doAnswer(invocation -> null).when(emailService).sendWelcomeEmail(anyString(), anyString(), anyString());
    }

    @Test
    public void testRegistrationAndUserIdGeneration() {
        RegisterRequest registerReq = new RegisterRequest("Rahul Kumar", "rahul@example.com", "Password123", "Password123");
        MessageResponse regResp = authService.requestRegistrationOtp(registerReq);
        assertNotNull(regResp);
        assertNotNull(lastCapturedOtp);

        // Verify OTP - User ID should be generated ONLY NOW
        VerifyOtpRequest verifyReq = new VerifyOtpRequest("rahul@example.com", lastCapturedOtp);
        MessageResponse verifyResp = authService.verifyRegistrationOtp(verifyReq);

        assertNotNull(verifyResp.getUserId());
        assertTrue(verifyResp.getUserId().startsWith("CRK"));
        assertEquals(9, verifyResp.getUserId().length()); // CRK + 6 digits = 9 chars

        // Verify User saved in DB
        User user = userRepository.findByEmail("rahul@example.com").orElseThrow();
        assertEquals(verifyResp.getUserId(), user.getUserId());
        assertEquals("Rahul Kumar", user.getName());
        assertTrue(user.isEmailVerified());
        assertTrue(passwordEncoder.matches("Password123", user.getPasswordHash()));

        // Verify Welcome Email attempt
        verify(emailService).sendWelcomeEmail(eq("rahul@example.com"), eq("Rahul Kumar"), eq(verifyResp.getUserId()));
    }

    @Test
    public void testUniqueUserIdGenerationAcrossUsers() {
        // Create User 1
        authService.requestRegistrationOtp(new RegisterRequest("User One", "user1@example.com", "Password123", "Password123"));
        String otp1 = lastCapturedOtp;
        MessageResponse resp1 = authService.verifyRegistrationOtp(new VerifyOtpRequest("user1@example.com", otp1));

        // Create User 2
        authService.requestRegistrationOtp(new RegisterRequest("User Two", "user2@example.com", "Password123", "Password123"));
        String otp2 = lastCapturedOtp;
        MessageResponse resp2 = authService.verifyRegistrationOtp(new VerifyOtpRequest("user2@example.com", otp2));

        assertNotEquals(resp1.getUserId(), resp2.getUserId());
        assertTrue(resp1.getUserId().startsWith("CRK"));
        assertTrue(resp2.getUserId().startsWith("CRK"));
    }

    @Test
    public void testLoginWithValidCredentials() {
        authService.requestRegistrationOtp(new RegisterRequest("Rahul Kumar", "rahul@example.com", "Password123", "Password123"));
        authService.verifyRegistrationOtp(new VerifyOtpRequest("rahul@example.com", lastCapturedOtp));

        LoginRequest loginReq = new LoginRequest("rahul@example.com", "Password123");
        LoginResponse loginResp = authService.login(loginReq);

        assertNotNull(loginResp.getToken());
        assertEquals("rahul@example.com", loginResp.getUser().getEmail());
        assertTrue(jwtUtil.validateToken(loginResp.getToken(), "rahul@example.com"));
    }

    @Test
    public void testLoginWithInvalidPasswordFails() {
        authService.requestRegistrationOtp(new RegisterRequest("Rahul Kumar", "rahul@example.com", "Password123", "Password123"));
        authService.verifyRegistrationOtp(new VerifyOtpRequest("rahul@example.com", lastCapturedOtp));

        LoginRequest loginReq = new LoginRequest("rahul@example.com", "WrongPassword");
        assertThrows(AuthException.class, () -> authService.login(loginReq));
    }

    @Test
    public void testForgotPasswordAndUserIdUnchanged() {
        // Register User
        authService.requestRegistrationOtp(new RegisterRequest("Rahul Kumar", "rahul@example.com", "Password123", "Password123"));
        MessageResponse regResp = authService.verifyRegistrationOtp(new VerifyOtpRequest("rahul@example.com", lastCapturedOtp));
        String originalUserId = regResp.getUserId();

        // Forgot password request
        authService.requestForgotPasswordOtp(new ForgotPasswordRequest("rahul@example.com"));
        String resetOtp = lastCapturedOtp;

        // Reset Password
        ResetPasswordRequest resetReq = new ResetPasswordRequest("rahul@example.com", resetOtp, "NewPassword123", "NewPassword123");
        MessageResponse resetResp = authService.resetPassword(resetReq);

        assertNotNull(resetResp);

        // Verify login works with new password
        LoginResponse loginResp = authService.login(new LoginRequest("rahul@example.com", "NewPassword123"));
        assertNotNull(loginResp.getToken());

        // Verify User ID is UNCHANGED
        User user = userRepository.findByEmail("rahul@example.com").orElseThrow();
        assertEquals(originalUserId, user.getUserId());
    }
}
