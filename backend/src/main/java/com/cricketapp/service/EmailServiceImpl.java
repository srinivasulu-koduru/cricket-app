package com.cricketapp.service;

import com.cricketapp.exception.AuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Brevo HTTPS Transactional Email API Implementation.
 * Replaces legacy SMTP delivery to bypass cloud/Railway outbound SMTP port blocks.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String brevoApiKey;
    private final String senderName;
    private final String senderEmail;
    private final boolean emailEnabled;
    private final boolean devLogEnabled;

    @Autowired
    public EmailServiceImpl(
            @Autowired(required = false) ObjectMapper objectMapper,
            @Value("${app.brevo.api-key:${BREVO_API_KEY:}}") String brevoApiKey,
            @Value("${app.brevo.sender-name:SRIT}") String senderName,
            @Value("${app.brevo.sender-email:sritcricket@gmail.com}") String senderEmail,
            @Value("${app.email.enabled:true}") boolean emailEnabled,
            @Value("${app.otp.dev-log-enabled:false}") boolean devLogEnabled) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.brevoApiKey = (brevoApiKey != null) ? brevoApiKey.trim() : "";
        this.senderName = StringUtils.hasText(senderName) ? senderName.trim() : "SRIT";
        this.senderEmail = StringUtils.hasText(senderEmail) ? senderEmail.trim() : "sritcricket@gmail.com";
        this.emailEnabled = emailEnabled;
        this.devLogEnabled = devLogEnabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
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

        String htmlBody = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "<h2 style=\"color: #072b1a; margin-top: 0; font-size: 22px;\">Cricket App</h2>" +
                "<p style=\"color: #334155; font-size: 15px;\">Hello,</p>" +
                "<p style=\"color: #334155; font-size: 15px;\">Your Cricket App <strong>%s</strong> verification code is:</p>" +
                "<div style=\"background-color: #f1f5f9; padding: 18px; border-radius: 8px; text-align: center; margin: 24px 0;\">" +
                "<span style=\"font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #10b981;\">%s</span>" +
                "</div>" +
                "<p style=\"color: #475569; font-size: 14px;\">This OTP is valid for <strong>%d minutes</strong>.</p>" +
                "<p style=\"color: #64748b; font-size: 13px;\">Please do not share this OTP with anyone. If you did not request this verification, you can safely ignore this email.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px; margin-bottom: 0;\">Regards,<br/><strong>Cricket App Team</strong></p>" +
                "</div>",
                purposeText, otp, expirationMinutes
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] OTP email prepared for recipient: {}", toEmail);
            logger.info("[DEV LOG ONLY] Purpose: {}", purposeText);
            logger.info("==================================================================");
        }

        if (emailEnabled) {
            if (!StringUtils.hasText(brevoApiKey)) {
                logger.error("Brevo API key is not configured (BREVO_API_KEY is empty)");
                throw new AuthException("Unable to send verification email. Email service API key is not configured.");
            }

            try {
                sendBrevoEmail(toEmail, null, subject, body, htmlBody);
                logger.info("Real OTP email sent successfully via Brevo HTTPS API to {}", toEmail);
            } catch (AuthException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Failed to send real OTP email via Brevo to {}: {}", toEmail, e.getMessage());
                throw new AuthException("Failed to deliver verification email. Please try again.");
            }
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

        String htmlBody = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "<h2 style=\"color: #072b1a; margin-top: 0; font-size: 22px;\">Welcome to Cricket App!</h2>" +
                "<p style=\"color: #334155; font-size: 15px;\">Hello <strong>%s</strong>,</p>" +
                "<p style=\"color: #334155; font-size: 15px;\">Your account has been successfully created.</p>" +
                "<div style=\"background-color: #f1f5f9; padding: 18px; border-radius: 8px; margin: 20px 0;\">" +
                "<p style=\"margin: 4px 0; color: #475569;\"><strong>Cricket User ID:</strong> <span style=\"color: #10b981; font-weight: bold;\">%s</span></p>" +
                "<p style=\"margin: 4px 0; color: #475569;\"><strong>Registered Email:</strong> %s</p>" +
                "</div>" +
                "<p style=\"color: #64748b; font-size: 13px;\">Please keep your Cricket User ID safe. You may use it when joining teams or being added to squads.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px; margin-bottom: 0;\">Regards,<br/><strong>Cricket App Team</strong></p>" +
                "</div>",
                recipientName, cricketUserId, recipientEmail
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] Welcome Email prepared for recipient: {}", recipientEmail);
            logger.info("[DEV LOG ONLY] User ID: {}", cricketUserId);
            logger.info("==================================================================");
        }

        if (emailEnabled && StringUtils.hasText(brevoApiKey)) {
            CompletableFuture.runAsync(() -> {
                try {
                    sendBrevoEmail(recipientEmail, recipientName, subject, body, htmlBody);
                    logger.info("Real Welcome email sent successfully via Brevo HTTPS API to {}", recipientEmail);
                } catch (Exception e) {
                    logger.error("Welcome email could not be sent to {}: {}", recipientEmail, e.getMessage());
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
        String joinUrl = "https://incandescent-fairy-f8751a.netlify.app/pages/join-team.html?token=" + joinToken;

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

        String htmlBody = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "<h2 style=\"color: #072b1a; margin-top: 0; font-size: 22px;\">Team Invitation</h2>" +
                "<p style=\"color: #334155; font-size: 15px;\">Hello <strong>%s</strong>,</p>" +
                "<p style=\"color: #334155; font-size: 15px;\"><strong>%s</strong> has invited you to join their cricket team on Cricket App!</p>" +
                "<div style=\"background-color: #f1f5f9; padding: 18px; border-radius: 8px; margin: 20px 0;\">" +
                "<p style=\"margin: 4px 0;\"><strong>Team Name:</strong> %s</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Team ID:</strong> %s</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Captain:</strong> %s</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Description:</strong> %s</p>" +
                "</div>" +
                "<p><a href=\"%s\" style=\"display: inline-block; background-color: #10b981; color: #ffffff; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: bold;\">Accept / View Invitation</a></p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px; margin-bottom: 0;\">Regards,<br/><strong>Cricket App Team</strong></p>" +
                "</div>",
                recipientName, captainName, teamName, teamId, captainName, descText, joinUrl
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] Team Invitation prepared for recipient: {}", recipientEmail);
            logger.info("[DEV LOG ONLY] Team ID: {}", teamId);
            logger.info("==================================================================");
        }

        if (emailEnabled && StringUtils.hasText(brevoApiKey)) {
            CompletableFuture.runAsync(() -> {
                try {
                    sendBrevoEmail(recipientEmail, recipientName, subject, body, htmlBody);
                    logger.info("Team Invitation email sent successfully via Brevo to {}", recipientEmail);
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

        String htmlBody = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "<h2 style=\"color: #072b1a; margin-top: 0; font-size: 22px;\">Cricket Match Invitation</h2>" +
                "<p style=\"color: #334155; font-size: 15px;\">Hello <strong>%s</strong>,</p>" +
                "<p style=\"color: #334155; font-size: 15px;\"><strong>%s</strong>, captain of <strong>%s</strong>, has invited your team <strong>%s</strong> to a cricket match.</p>" +
                "<div style=\"background-color: #f1f5f9; padding: 18px; border-radius: 8px; margin: 20px 0;\">" +
                "<p style=\"margin: 4px 0;\"><strong>Match:</strong> %s vs %s</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Date & Time:</strong> %s %s</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Venue:</strong> %s</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Format:</strong> %s (%d Overs)</p>" +
                "</div>" +
                "<p style=\"color: #475569;\">Please open Cricket App to review and Accept or Reject this invitation.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px; margin-bottom: 0;\">Regards,<br/><strong>Cricket App Team</strong></p>" +
                "</div>",
                recipientName, invitingCaptainName, invitingTeamName, opponentTeamName,
                invitingTeamName, opponentTeamName, matchDate, matchTime, venue, format, overs
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] Match Invitation prepared for recipient: {}", recipientEmail);
            logger.info("[DEV LOG ONLY] Match: {} vs {}", invitingTeamName, opponentTeamName);
            logger.info("==================================================================");
        }

        if (emailEnabled && StringUtils.hasText(brevoApiKey)) {
            CompletableFuture.runAsync(() -> {
                try {
                    sendBrevoEmail(recipientEmail, recipientName, subject, body, htmlBody);
                    logger.info("Match Invitation email sent successfully via Brevo to {}", recipientEmail);
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

        String htmlBody = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "<h2 style=\"color: #072b1a; margin-top: 0; font-size: 22px;\">Cricket App Notification</h2>" +
                "<p style=\"color: #334155; font-size: 15px;\">%s</p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px; margin-bottom: 0;\">Regards,<br/><strong>Cricket App Team</strong></p>" +
                "</div>",
                messageText
        );

        if (devLogEnabled) {
            logger.info("==================================================================");
            logger.info("[DEV LOG ONLY] Notification Email prepared for recipient: {}", recipientEmail);
            logger.info("[DEV LOG ONLY] Subject: {}", subject);
            logger.info("==================================================================");
        }

        if (emailEnabled && StringUtils.hasText(brevoApiKey)) {
            CompletableFuture.runAsync(() -> {
                try {
                    sendBrevoEmail(recipientEmail, null, subject, body, htmlBody);
                    logger.info("Notification email sent successfully via Brevo to {}", recipientEmail);
                } catch (Exception e) {
                    logger.error("Notification email could not be sent to {}: {}", recipientEmail, e.getMessage());
                }
            });
        }
    }

    /**
     * Sends an email via Brevo's HTTPS REST API (POST https://api.brevo.com/v3/smtp/email).
     */
    private void sendBrevoEmail(String recipientEmail, String recipientName, String subject, String textBody, String htmlBody) {
        if (!emailEnabled) {
            logger.info("Email delivery is disabled (app.email.enabled=false). Skipping email to {}", recipientEmail);
            return;
        }

        if (!StringUtils.hasText(brevoApiKey)) {
            logger.error("Brevo API key is not configured (BREVO_API_KEY is empty)");
            throw new AuthException("Unable to send email. Email delivery API key is not configured.");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sender", Map.of("name", senderName, "email", senderEmail));

            Map<String, String> recipient = new LinkedHashMap<>();
            recipient.put("email", recipientEmail);
            if (StringUtils.hasText(recipientName)) {
                recipient.put("name", recipientName.trim());
            }
            payload.put("to", List.of(recipient));

            payload.put("subject", subject);
            if (StringUtils.hasText(textBody)) {
                payload.put("textContent", textBody);
            }
            if (StringUtils.hasText(htmlBody)) {
                payload.put("htmlContent", htmlBody);
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", brevoApiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Email '{}' dispatched successfully via Brevo HTTPS API to {}", subject, recipientEmail);
            } else {
                logger.error("Brevo HTTPS API failed with HTTP status code {}: {}", statusCode, response.body());
                throw new AuthException("Email delivery failed via Brevo (status " + statusCode + ").");
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error communicating with Brevo HTTPS API for recipient {}: {}", recipientEmail, e.getMessage());
            throw new AuthException("Failed to deliver email via Brevo HTTPS API. Please try again.");
        }
    }
}
