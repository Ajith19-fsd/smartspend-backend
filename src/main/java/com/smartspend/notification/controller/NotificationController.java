package com.smartspend.notification.controller;

import com.smartspend.notification.model.Notification;
import com.smartspend.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin("*")
public class NotificationController {

    @Autowired
    private NotificationService service;

    // Send notification to a user (admin/test)
    @PostMapping("/send")
    public Notification send(@RequestParam String message,
                             @RequestParam Long userId) {
        return service.sendNotification(message, userId);
    }

    // Test endpoint: send simple test notification
    @PostMapping("/test/{userId}")
    public Notification sendTest(@PathVariable Long userId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle("Test Notification");
        n.setMessage("This is a test alert!");
        return service.sendNotification(n);
    }

    // Get notifications for user
    @GetMapping
    public List<Notification> getNotifications(@RequestParam Long userId) {
        return service.getNotifications(userId);
    }

    // Mark as read
    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }
}
