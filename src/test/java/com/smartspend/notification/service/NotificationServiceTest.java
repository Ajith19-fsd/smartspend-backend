package com.smartspend.notification.service;

import com.smartspend.notification.model.Notification;
import com.smartspend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;

    @BeforeEach
    void setup() {
        notification = new Notification();
        notification.setId(1L);
        notification.setUserId(10L);
        notification.setTitle("Alert");
        notification.setMessage("Budget exceeded");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
    }

    // -------------------------
    // sendNotification(Notification)
    // -------------------------
    @Test
    void sendNotification_success() {

        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(messagingTemplate)
                .convertAndSend((String) any(), (Object) any());

        Notification result = notificationService.sendNotification(notification);

        assertNotNull(result);
        assertEquals(10L, result.getUserId());

        verify(repository, times(1)).save(any(Notification.class));
    }

    @Test
    void sendNotification_missingUserId_throwsException() {
        notification.setUserId(null);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> notificationService.sendNotification(notification)
        );

        assertEquals("UserId is required to send notification", ex.getMessage());

        verify(repository, never()).save(any());
    }

    // -------------------------
    // sendNotification(String, Long)
    // -------------------------
    @Test
    void sendNotification_shortcut_success() {

        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(messagingTemplate)
                .convertAndSend((String) any(), (Object) any());

        Notification result =
                notificationService.sendNotification("Test message", 10L);

        assertNotNull(result);
        assertEquals("Alert", result.getTitle());
        assertEquals("Test message", result.getMessage());
        assertEquals(10L, result.getUserId());

        verify(repository, times(1)).save(any(Notification.class));
    }

    // -------------------------
    // getNotifications(Long)
    // -------------------------
    @Test
    void getNotifications_success() {

        when(repository.findByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(notification));

        List<Notification> result =
                notificationService.getNotifications(10L);

        assertEquals(1, result.size());
        assertEquals("Budget exceeded", result.get(0).getMessage());

        verify(repository, times(1))
                .findByUserIdOrderByCreatedAtDesc(10L);
    }

    // -------------------------
    // markAsRead(Long)
    // -------------------------
    @Test
    void markAsRead_success() {

        when(repository.findById(1L)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.markAsRead(1L);

        assertTrue(result.isRead());

        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(notification);
    }

    @Test
    void markAsRead_notFound_throwsException() {

        when(repository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> notificationService.markAsRead(1L)
        );

        assertEquals("Notification not found", ex.getMessage());

        verify(repository, times(1)).findById(1L);
        verify(repository, never()).save(any());
    }
}
