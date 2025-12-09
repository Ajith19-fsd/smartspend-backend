package com.smartspend.notification.service;

import com.smartspend.notification.model.Notification;
import com.smartspend.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Save & send notification (object)
    public Notification sendNotification(Notification notification) {
        if (notification.getUserId() == null) {
            throw new RuntimeException("UserId is required to send notification");
        }

        // Ensure createdAt is set
        notification.setCreatedAt(LocalDateTime.now());
        Notification saved = repository.save(notification);

        // Send real-time WebSocket alert to user-specific topic
        messagingTemplate.convertAndSend("/topic/alerts/" + notification.getUserId(), saved);

        return saved;
    }

    // Shortcut to send simple text notification
    public Notification sendNotification(String message, Long userId) {
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setUserId(userId);
        notification.setTitle("Alert");
        return sendNotification(notification);
    }

    // Get notifications for a user (latest first)
    public List<Notification> getNotifications(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Mark notification as read
    public Notification markAsRead(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        return repository.save(notification);
    }
}
