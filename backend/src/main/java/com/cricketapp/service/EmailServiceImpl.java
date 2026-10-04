package com.cricketapp.service;

import com.cricketapp.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final boolean devLogEnabled;
    private final String mailUsername;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.email.enabled:true}") boolean emailEnabled,
            @Value("${app.otp.dev-log-enabled:false}") boolean devLogEnabled,
            @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
        this.devLogEnabled = devLogEnabled;
        this.mailUsername = mailUsername;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp, String purposeText, int expirationMinutes) {
        String subject = "Cricket App - Email Verification OTP";
        String body = String.format(
                "Hello,%n%n" +
                "Your Cricket App %s OTP is:%n%n" +
                "%s%n%n" +
                "This OTP is valid for %d minutes.%n%n" +
                "Please do not share this OTP with anyone.%n%n" +
                "If you did not request this verification, you can safely ignore this email.%n%n" +
                "Regards,%n" +
                "Cricket App Team",
                purposeText.toLowerCase(), otp, expirationMinutes
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] To: {}", toEmail);
            logger.info("[DEV LOG ONLY] Subject: {}", subject);
            logger.info("[DEV LOG ONLY] OTP: {}", otp);
            logger.info("==================================================================");
        }

        if (emailEnabled) {
            if (!StringUtils.hasText(mailUsername)) {
                logger.error("SMTP mail username is not configured (MAIL_USERNAME is empty)");
                throw new AuthException("Unable to send verification email. Please configure SMTP credentials (MAIL_USERNAME and MAIL_PASSWORD).");
            }

            // Offload slow SMTP network handshake to background thread for instant frontend response (<50ms)
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(mailUsername);
                    message.setTo(toEmail);
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                    logger.info("Real OTP email sent successfully via SMTP to {}", toEmail);
                } catch (Exception e) {
                    logger.error("Failed to send real SMTP email to {}: {}", toEmail, e.getMessage());
                }
            });
        }
    }

    @Override
    public void sendWelcomeEmail(String recipientEmail, String recipientName, String cricketUserId) {
        String subject = "Welcome to Cricket App - Account Created";
        String body = String.format(
                "Hello %s,%n%n" +
                "Welcome to Cricket App!%n%n" +
                "Your account has been successfully created.%n%n" +
                "Your Cricket User ID:%n" +
                "%s%n%n" +
                "Registered Email:%n" +
                "%s%n%n" +
                "You can use your registered email address and the password you created during registration to log in.%n%n" +
                "For security reasons, your password is not included in this email.%n%n" +
                "Please keep your Cricket User ID safe. You may use it when joining teams, participating in matches, or being added to cricket teams.%n%n" +
                "Regards,%n" +
                "Cricket App Team",
                recipientName, cricketUserId, recipientEmail
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] Welcome Email To: {}", recipientEmail);
            logger.info("[DEV LOG ONLY] Subject: {}", subject);
            logger.info("[DEV LOG ONLY] User ID: {}", cricketUserId);
            logger.info("==================================================================");
        }

        if (emailEnabled) {
            if (!StringUtils.hasText(mailUsername)) {
                logger.error("Welcome email could not be sent to registered user. SMTP mail username is not configured.");
                return;
            }

            // Offload slow SMTP network handshake to background thread for instant frontend response (<50ms)
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(mailUsername);
                    message.setTo(recipientEmail);
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                    logger.info("Real Welcome email sent successfully via SMTP to {}", recipientEmail);
                } catch (Exception e) {
                    logger.error("Welcome email could not be sent to registered user {}: {}", recipientEmail, e.getMessage());
                }
            });
        }
    }

    @Override
    public void sendTeamInvitationEmail(
            String recipientEmail,
            String recipientName,
            String teamName,
            String teamId,
            String captainName,
            String teamDescription,
            String joinToken) {
        String subject = String.format("Cricket App - Invitation to join team \"%s\"", teamName);
        String descText = StringUtils.hasText(teamDescription) ? teamDescription : "No description provided";
        String joinUrl = "http://localhost:5500/pages/join-team.html?token=" + joinToken;

        String body = String.format(
                "Hello %s,%n%n" +
                "You have received a Cricket Team Invitation on Cricket App!%n%n" +
                "%s has invited you to join their cricket team.%n%n" +
                "--- Team Details ---%n" +
                "Team Name: %s%n" +
                "Team ID: %s%n" +
                "Captain / Manager: %s%n" +
                "Description: %s%n%n" +
                "--- How to Respond ---%n" +
                "Option 1: Open Cricket App in your browser, sign in, go to 'My Teams', and click 'Pending Invitations' to ACCEPT or REJECT this invitation inside the app.%n%n" +
                "Option 2: Use the Direct Shareable Team Join Link:%n" +
                "%s%n%n" +
                "Regards,%n" +
                "Cricket App Team",
                recipientName, captainName, teamName, teamId, captainName, descText, joinUrl
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] Team Invitation Email To: {}", recipientEmail);
            logger.info("[DEV LOG ONLY] Subject: {}", subject);
            logger.info("[DEV LOG ONLY] Team ID: {}", teamId);
            logger.info("[DEV LOG ONLY] Join URL: {}", joinUrl);
            logger.info("==================================================================");
        }

        if (emailEnabled) {
            if (!StringUtils.hasText(mailUsername)) {
                logger.error("Team invitation email could not be sent. SMTP mail username is not configured.");
                return;
            }

            // Offload slow SMTP network handshake to background thread for instant response (<50ms)
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(mailUsername);
                    message.setTo(recipientEmail);
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                    logger.info("Real Team Invitation email sent successfully via SMTP to {}", recipientEmail);
                } catch (Exception e) {
                    logger.error("Team invitation email could not be sent to {}: {}", recipientEmail, e.getMessage());
                }
            });
        }
    }

    @Override
    public void sendMatchInvitationEmail(
            String recipientEmail,
            String recipientName,
            String invitingCaptainName,
            String invitingTeamName,
            String opponentTeamName,
            String matchDate,
            String matchTime,
            String venue,
            String format,
            Integer overs) {
        String subject = String.format("Cricket Match Invitation — %s vs %s", invitingTeamName, opponentTeamName);
        String body = String.format(
                "Hello %s,%n%n" +
                "%s, captain of %s, has invited your team %s to a cricket match.%n%n" +
                "Match:%n" +
                "%s vs %s%n%n" +
                "Date:%n" +
                "%s%n%n" +
                "Time:%n" +
                "%s%n%n" +
                "Venue:%n" +
                "%s%n%n" +
                "Format:%n" +
                "%s — %d Overs%n%n" +
                "Please open the Cricket App to review this invitation.%n%n" +
                "You can Accept or Reject the match invitation.%n%n" +
                "Regards,%n" +
                "Cricket App Team",
                recipientName, invitingCaptainName, invitingTeamName, opponentTeamName,
                invitingTeamName, opponentTeamName, matchDate, matchTime, venue, format, overs
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] Match Invitation Email To: {}", recipientEmail);
            logger.info("[DEV LOG ONLY] Subject: {}", subject);
            logger.info("[DEV LOG ONLY] Match: {} vs {}", invitingTeamName, opponentTeamName);
            logger.info("==================================================================");
        }

        if (emailEnabled) {
            if (!StringUtils.hasText(mailUsername)) {
                logger.error("Match invitation email could not be sent. SMTP mail username is not configured.");
                return;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(mailUsername);
                    message.setTo(recipientEmail);
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                    logger.info("Real Match Invitation email sent successfully via SMTP to {}", recipientEmail);
                } catch (Exception e) {
                    logger.error("Match invitation email could not be sent to {}: {}", recipientEmail, e.getMessage());
                }
            });
        }
    }

    @Override
    public void sendNotificationEmail(String recipientEmail, String subject, String messageText) {
        String body = String.format(
                "Hello,%n%n" +
                "%s%n%n" +
                "Regards,%n" +
                "Cricket App Team",
                messageText
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] Notification Email To: {}", recipientEmail);
            logger.info("[DEV LOG ONLY] Subject: {}", subject);
            logger.info("[DEV LOG ONLY] Message: {}", messageText);
            logger.info("==================================================================");
        }

        if (emailEnabled && StringUtils.hasText(mailUsername)) {
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(mailUsername);
                    message.setTo(recipientEmail);
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                    logger.info("Notification email sent successfully to {}", recipientEmail);
                } catch (Exception e) {
                    logger.error("Notification email could not be sent to {}: {}", recipientEmail, e.getMessage());
                }
            });
        }
    }
}


