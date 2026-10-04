package com.cricketapp.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp, String purposeText, int expirationMinutes);
    void sendWelcomeEmail(String recipientEmail, String recipientName, String cricketUserId);
    void sendTeamInvitationEmail(
            String recipientEmail,
            String recipientName,
            String teamName,
            String teamId,
            String captainName,
            String teamDescription,
            String joinToken
    );
    void sendMatchInvitationEmail(
            String recipientEmail,
            String recipientName,
            String invitingCaptainName,
            String invitingTeamName,
            String opponentTeamName,
            String matchDate,
            String matchTime,
            String venue,
            String format,
            Integer overs
    );
    void sendNotificationEmail(String recipientEmail, String subject, String messageText);
}


