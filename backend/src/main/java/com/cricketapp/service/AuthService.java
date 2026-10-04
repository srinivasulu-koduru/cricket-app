package com.cricketapp.service;

import com.cricketapp.dto.*;
import com.cricketapp.entity.OtpPurpose;
import com.cricketapp.entity.PendingRegistration;
import com.cricketapp.entity.User;
import com.cricketapp.exception.AuthException;
import com.cricketapp.repository.PendingRegistrationRepository;
import com.cricketapp.repository.UserRepository;
import com.cricketapp.util.JwtUtil;
import com.cricketapp.util.UserIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserIdGenerator userIdGenerator;

    public AuthService(
            UserRepository userRepository,
            PendingRegistrationRepository pendingRepository,
            OtpService otpService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            UserIdGenerator userIdGenerator) {
        this.userRepository = userRepository;
        this.pendingRepository = pendingRepository;
        this.otpService = otpService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userIdGenerator = userIdGenerator;
    }

    @Transactional
    public MessageResponse requestRegistrationOtp(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("Passwords do not match");
        }

        validatePasswordPolicy(request.getPassword());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AuthException("Email is already registered. Please login instead.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        // Save or update pending registration
        Optional<PendingRegistration> existingPending = pendingRepository.findByEmail(normalizedEmail);
        PendingRegistration pending;
        if (existingPending.isPresent()) {
            pending = existingPending.get();
            pending.setName(request.getName().trim());
            pending.setPasswordHash(hashedPassword);
            pending.setExpiresAt(expiresAt);
        } else {
            pending = new PendingRegistration(normalizedEmail, request.getName().trim(), hashedPassword, expiresAt);
        }
        pendingRepository.save(pending);

        // Generate and send OTP
        otpService.generateAndSendOtp(normalizedEmail, OtpPurpose.REGISTRATION);

        return new MessageResponse("Verification OTP sent to " + normalizedEmail);
    }

    @Transactional
    public MessageResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // 1. Verify OTP first
        otpService.verifyOtp(normalizedEmail, request.getOtp().trim(), OtpPurpose.REGISTRATION);

        // 2. Find pending registration data
        PendingRegistration pending = pendingRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException("Registration session expired. Please register again."));

        if (LocalDateTime.now().isAfter(pending.getExpiresAt())) {
            pendingRepository.deleteByEmail(normalizedEmail);
            throw new AuthException("Registration session expired. Please register again.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            pendingRepository.deleteByEmail(normalizedEmail);
            throw new AuthException("Email is already registered. Please login.");
        }

        // 3. IMPORTANT USER ID RULE: Generate permanent User ID ONLY AFTER successful OTP verification & account creation
        String userId = userIdGenerator.generateUniqueUserId();

        // 4. Create and save active User
        User user = new User(userId, pending.getName(), normalizedEmail, pending.getPasswordHash(), true);
        userRepository.save(user);

        // 5. Clean up pending registration
        pendingRepository.deleteByEmail(normalizedEmail);

        // 6. Send Welcome Email (Non-blocking failure: account remains created even if email fails)
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName(), user.getUserId());
        } catch (Exception e) {
            logger.error("Welcome email could not be sent to registered user {}: {}", normalizedEmail, e.getMessage());
        }

        return new MessageResponse("Account created successfully", userId);
    }

    @Transactional
    public MessageResponse resendRegistrationOtp(ResendOtpRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        PendingRegistration pending = pendingRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException("No pending registration found for this email. Please register."));

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AuthException("Email is already registered. Please login.");
        }

        otpService.generateAndSendOtp(normalizedEmail, OtpPurpose.REGISTRATION);

        return new MessageResponse("A new OTP has been sent to " + normalizedEmail);
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!user.isEmailVerified()) {
            throw new AuthException("Email is not verified. Please verify your email first.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getUserId());
        UserResponse userResponse = new UserResponse(user.getUserId(), user.getName(), user.getEmail(), user.isEmailVerified());

        return new LoginResponse("Login successful", token, userResponse);
    }

    @Transactional
    public MessageResponse requestForgotPasswordOtp(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);
        if (userOptional.isPresent()) {
            otpService.generateAndSendOtp(normalizedEmail, OtpPurpose.PASSWORD_RESET);
        }

        // Always return generic response to prevent account enumeration
        return new MessageResponse("If an account exists for this email, a password reset OTP has been sent.");
    }

    @Transactional
    public MessageResponse verifyForgotPasswordOtp(VerifyOtpRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        otpService.verifyOtp(normalizedEmail, request.getOtp().trim(), OtpPurpose.PASSWORD_RESET);
        return new MessageResponse("OTP verified successfully. You may now reset your password.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("Passwords do not match");
        }

        validatePasswordPolicy(request.getNewPassword());

        // Verify OTP
        otpService.verifyOtp(normalizedEmail, request.getOtp().trim(), OtpPurpose.PASSWORD_RESET);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException("User account not found"));

        // Update password hash. User ID remains unchanged!
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new MessageResponse("Password reset successfully. You may now login with your new password.");
    }

    public UserResponse getCurrentUser(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException("User not found"));

        return new UserResponse(user.getUserId(), user.getName(), user.getEmail(), user.isEmailVerified());
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new AuthException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number.");
        }
    }
}
