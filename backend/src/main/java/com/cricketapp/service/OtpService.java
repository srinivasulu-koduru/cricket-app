package com.cricketapp.service;

import com.cricketapp.entity.OtpPurpose;
import com.cricketapp.entity.OtpVerification;
import com.cricketapp.exception.AuthException;
import com.cricketapp.repository.OtpVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    private final int expirationMinutes;
    private final int maxAttempts;
    private final int resendCooldownSeconds;

    public OtpService(
            OtpVerificationRepository otpRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            @Value("${app.otp.expiration-minutes:5}") int expirationMinutes,
            @Value("${app.otp.max-attempts:5}") int maxAttempts,
            @Value("${app.otp.resend-cooldown-seconds:60}") int resendCooldownSeconds) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    @Transactional
    public void generateAndSendOtp(String email, OtpPurpose purpose) {
        String normalizedEmail = email.toLowerCase().trim();

        Optional<OtpVerification> latestOpt = otpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(normalizedEmail, purpose);
        if (latestOpt.isPresent()) {
            OtpVerification lastOtp = latestOpt.get();
            long secondsSinceLast = Duration.between(lastOtp.getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < resendCooldownSeconds) {
                long waitSeconds = resendCooldownSeconds - secondsSinceLast;
                throw new AuthException("Please wait " + waitSeconds + " seconds before requesting a new OTP.");
            }
        }

        String rawOtp = String.format("%06d", random.nextInt(1_000_000));
        String hashedOtp = passwordEncoder.encode(rawOtp);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        OtpVerification newOtp = new OtpVerification(normalizedEmail, hashedOtp, purpose, expiresAt);
        otpRepository.save(newOtp);

        String purposeText = (purpose == OtpPurpose.REGISTRATION) ? "Registration" : "Password Reset";
        emailService.sendOtpEmail(normalizedEmail, rawOtp, purposeText, expirationMinutes);
    }

    @Transactional
    public boolean verifyOtp(String email, String rawOtp, OtpPurpose purpose) {
        String normalizedEmail = email.toLowerCase().trim();

        Optional<OtpVerification> latestOpt = otpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(normalizedEmail, purpose);
        if (latestOpt.isEmpty()) {
            throw new AuthException("No active OTP found. Please request a new OTP.");
        }

        OtpVerification otpVerification = latestOpt.get();

        if (LocalDateTime.now().isAfter(otpVerification.getExpiresAt())) {
            throw new AuthException("OTP has expired. Please request a new OTP.");
        }

        if (otpVerification.getAttempts() >= maxAttempts) {
            throw new AuthException("Maximum OTP verification attempts reached. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(rawOtp, otpVerification.getOtpHash())) {
            otpVerification.incrementAttempts();
            otpRepository.save(otpVerification);
            int remaining = maxAttempts - otpVerification.getAttempts();
            if (remaining <= 0) {
                throw new AuthException("Maximum OTP verification attempts reached. Please request a new OTP.");
            }
            throw new AuthException("Invalid OTP. " + remaining + " attempts remaining.");
        }

        otpVerification.setVerified(true);
        otpRepository.save(otpVerification);
        return true;
    }
}
