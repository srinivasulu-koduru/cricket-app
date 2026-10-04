package com.cricketapp.controller;

import com.cricketapp.dto.*;
import com.cricketapp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/request-otp")
    public ResponseEntity<MessageResponse> requestRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        MessageResponse response = authService.requestRegistrationOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<MessageResponse> verifyRegistrationOtp(@Valid @RequestBody VerifyOtpRequest request) {
        MessageResponse response = authService.verifyRegistrationOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<MessageResponse> resendRegistrationOtp(@Valid @RequestBody ResendOtpRequest request) {
        MessageResponse response = authService.resendRegistrationOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        // Stateless JWT logout acknowledgment
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        UserResponse response = authService.getCurrentUser(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<MessageResponse> requestForgotPasswordOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = authService.requestForgotPasswordOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<MessageResponse> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequest request) {
        MessageResponse response = authService.verifyForgotPasswordOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}
