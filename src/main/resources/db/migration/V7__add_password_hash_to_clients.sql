-- Додаємо колонку password_hash до таблиці clients
ALTER TABLE clients ADD COLUMN IF NOT EXISTS password_hash VARCHAR(64);-- Додаємо колонку password_hash до таблиці clients
