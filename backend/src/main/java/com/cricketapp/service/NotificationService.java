package com.cricketapp.service;

import com.cricketapp.dto.NotificationDto;
import com.cricketapp.entity.Notification;
import com.cricketapp.entity.User;
import com.cricketapp.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Notification createNotification(User recipient, String title, String message, String type, String referenceId) {
        Notification notification = new Notification(recipient, title, message, type, referenceId);
        Notification saved = notificationRepository.save(notification);

        logger.info("Created in-app notification [{}] for user {}", title, recipient.getEmail());

        // Send email asynchronously/safely if recipient has email
        try {
            if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
                String emailSubject = "Cricket App - " + title;
                emailService.sendNotificationEmail(recipient.getEmail(), emailSubject, message);
            }
        } catch (Exception e) {
            logger.warn("Could not send notification email to {}: {}", recipient.getEmail(), e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(User user) {
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        return list.stream()
                .map(n -> new NotificationDto(
                        n.getId(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getType(),
                        n.getReferenceId(),
                        n.isRead(),
                        n.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(user.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        for (Notification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }
}
