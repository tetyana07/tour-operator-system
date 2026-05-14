-- Додаємо поля вологості, швидкості вітру та відчутної температури
ALTER TABLE weather_forecasts
    ADD COLUMN IF NOT EXISTS humidity   INTEGER,
    ADD COLUMN IF NOT EXISTS wind_speed INTEGER,
    ADD COLUMN IF NOT EXISTS feels_like NUMERIC(5, 1);