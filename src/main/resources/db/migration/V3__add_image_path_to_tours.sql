-- V3: додаємо поле image_path до таблиці tours
ALTER TABLE tours ADD COLUMN IF NOT EXISTS image_path TEXT;