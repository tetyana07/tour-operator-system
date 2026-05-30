-- V6: додаємо колонку departure (місто/аеропорт відльоту) до таблиці tours
ALTER TABLE tours ADD COLUMN IF NOT EXISTS departure TEXT DEFAULT NULL;