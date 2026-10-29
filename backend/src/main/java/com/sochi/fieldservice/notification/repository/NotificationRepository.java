package com.sochi.fieldservice.notification.repository;

import com.sochi.fieldservice.notification.entity.Notification;
import com.sochi.fieldservice.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n JOIN FETCH n.incident i LEFT JOIN FETCH i.assignedEngineer ORDER BY n.createdAt DESC")
    List<Notification> findAllWithIncident();

    @Query(value = "SELECT n FROM Notification n JOIN FETCH n.incident i LEFT JOIN FETCH i.assignedEngineer ORDER BY n.createdAt DESC",
           countQuery = "SELECT COUNT(n) FROM Notification n")
    Page<Notification> findAllWithIncident(Pageable pageable);

    @Query("SELECT n FROM Notification n JOIN FETCH n.incident i LEFT JOIN FETCH i.assignedEngineer WHERE n.id = :id")
    Optional<Notification> findByIdWithIncident(@Param("id") Long id);

    long countByReadFalse();

    boolean existsByIncidentIdAndType(Long incidentId, NotificationType type);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.read = false")
    int markAllAsRead();
}
