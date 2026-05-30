-- V9__add_booking_audit_log.sql
-- Таблиця аудиту змін бронювань
-- Зберігає: хто змінив, що змінив, коли, попередній і новий статус

CREATE TABLE IF NOT EXISTS booking_audit_log (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id   UUID         NOT NULL,
    changed_by   VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    action       VARCHAR(50)  NOT NULL,
    old_status   VARCHAR(30),
    new_status   VARCHAR(30),
    details      TEXT,
    changed_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_booking_id ON booking_audit_log(booking_id);
CREATE INDEX IF NOT EXISTS idx_audit_changed_at  ON booking_audit_log(changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_changed_by  ON booking_audit_log(changed_by);
