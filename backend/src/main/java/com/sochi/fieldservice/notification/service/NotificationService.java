package com.sochi.fieldservice.notification.service;

import com.sochi.fieldservice.notification.dto.NotificationResponse;
import com.sochi.fieldservice.notification.dto.UnreadCountResponse;
import com.sochi.fieldservice.notification.entity.Notification;
import com.sochi.fieldservice.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications() {
        return notificationRepository.findAllWithIncident()
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        long count = notificationRepository.countByReadFalse();
        return new UnreadCountResponse(count);
    }

    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findByIdWithIncident(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: " + id));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    public int markAllAsRead() {
        return notificationRepository.markAllAsRead();
    }
}
