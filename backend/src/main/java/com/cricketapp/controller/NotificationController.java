package com.cricketapp.controller;

import com.cricketapp.dto.NotificationDto;
import com.cricketapp.entity.User;
import com.cricketapp.repository.UserRepository;
import com.cricketapp.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Unauthenticated user");
        }
        return userRepository.findByEmail(auth.getName().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(notificationService.getUserNotifications(user));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
