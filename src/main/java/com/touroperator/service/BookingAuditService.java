package com.touroperator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сервіс аудиту змін бронювань.
 *
 * Зберігає в таблицю booking_audit_log записи про кожну зміну:
 * хто змінив, що змінив, коли, попереднє і нове значення.
 *
 * SQL для створення таблиці (додайте до Flyway міграції):
 * <pre>
 * CREATE TABLE IF NOT EXISTS booking_audit_log (
 *     id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 *     booking_id   UUID        NOT NULL,
 *     changed_by   VARCHAR(100) NOT NULL,   -- email або 'SYSTEM'
 *     action       VARCHAR(50)  NOT NULL,   -- CONFIRM, CANCEL, COMPLETE, CREATE
 *     old_status   VARCHAR(30),
 *     new_status   VARCHAR(30),
 *     details      TEXT,
 *     changed_at   TIMESTAMP   NOT NULL DEFAULT NOW()
 * );
 * CREATE INDEX idx_audit_booking_id ON booking_audit_log(booking_id);
 * CREATE INDEX idx_audit_changed_at ON booking_audit_log(changed_at);
 * </pre>
 *
 * Використання у BookingService:
 * <pre>
 *      
 *   auditService.log(bookingId, currentUser, "CONFIRM", "CREATED", "CONFIRMED", null);
 * </pre>
 */
@Service
public class BookingAuditService {

    private static final Logger log = LoggerFactory.getLogger(BookingAuditService.class);

    private final JdbcTemplate jdbc;

    public BookingAuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Записує подію до аудит-лога.
     *
     * @param bookingId  ID бронювання
     * @param changedBy  Email користувача або "SYSTEM"
     * @param action     Тип дії: CREATE, CONFIRM, CANCEL, COMPLETE, PAY
     * @param oldStatus  Попередній статус (може бути null при створенні)
     * @param newStatus  Новий статус
     * @param details    Додаткова інформація (причина скасування тощо)
     */
    public void log(UUID bookingId,
                    String changedBy,
                    String action,
                    String oldStatus,
                    String newStatus,
                    String details) {
        try {
            jdbc.update("""
                    INSERT INTO booking_audit_log
                        (id, booking_id, changed_by, action, old_status, new_status, details, changed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    bookingId,
                    changedBy != null ? changedBy : "SYSTEM",
                    action,
                    oldStatus,
                    newStatus,
                    details,
                    LocalDateTime.now());
        } catch (Exception e) {
               
            log.error("Помилка запису до аудит-лога: booking={}, action={}, error={}",
                    bookingId, action, e.getMessage());
        }
    }

    /**
     * Повертає всі записи аудиту для конкретного бронювання (новіші — першими).
     */
    public List<Map<String, Object>> getHistory(UUID bookingId) {
        try {
            return jdbc.queryForList(
                    """
                    SELECT changed_at, changed_by, action, old_status, new_status, details
                    FROM booking_audit_log
                    WHERE booking_id = ?
                    ORDER BY changed_at DESC
                    """,
                    bookingId);
        } catch (Exception e) {
            log.warn("Помилка читання аудит-лога: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Повертає останні N записів по всіх бронюваннях — для сторінки адміністратора.
     */
    public List<Map<String, Object>> getRecentActivity(int limit) {
        try {
            return jdbc.queryForList(
                    """
                    SELECT al.changed_at, al.changed_by, al.action,
                           al.old_status, al.new_status, al.booking_id, al.details
                    FROM booking_audit_log al
                    ORDER BY al.changed_at DESC
                    LIMIT ?
                    """,
                    limit);
        } catch (Exception e) {
            log.warn("Помилка читання аудит-лога: {}", e.getMessage());
            return List.of();
        }
    }
}
