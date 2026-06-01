-- ============================================================
-- V1__init_schema.sql
-- Повна схема БД для системи туроператора
-- ============================================================

-- Розширення для UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── CLIENTS ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS clients (
                                       id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    phone      VARCHAR(50),
    birth_date DATE
    );

-- ─── HOTELS ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hotels (
                                      id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    stars            INT CHECK (stars BETWEEN 1 AND 5),
    price_per_night  NUMERIC(10, 2) NOT NULL
    );

-- ─── TOURS ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tours (
                                     id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    country      VARCHAR(100) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    base_price   NUMERIC(10, 2) NOT NULL CHECK (base_price > 0),
    quota        INT NOT NULL CHECK (quota > 0),
    booked_seats INT NOT NULL DEFAULT 0 CHECK (booked_seats >= 0),
    hotel_id     UUID REFERENCES hotels(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE','FULL','CANCELLED','ARCHIVED')),
    description  TEXT,
    CONSTRAINT chk_dates CHECK (end_date > start_date),
    CONSTRAINT chk_booked CHECK (booked_seats <= quota)
    );

-- ─── EXCURSIONS ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS excursions (
                                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    price           NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    excursion_date  DATE
    );

-- Зв'язок тур ↔ екскурсії (які екскурсії доступні в даному турі)
CREATE TABLE IF NOT EXISTS tour_excursions (
                                               tour_id      UUID NOT NULL REFERENCES tours(id),
    excursion_id UUID NOT NULL REFERENCES excursions(id),
    PRIMARY KEY (tour_id, excursion_id)
    );

-- ─── INSURANCE ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS insurances (
                                          id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0)
    );

-- ─── TRANSFERS ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transfers (
                                         id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type  VARCHAR(100) NOT NULL,
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0)
    );

-- ─── PROMO CODES ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS promo_codes (
                                           id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50) NOT NULL UNIQUE,
    discount_percent INT NOT NULL CHECK (discount_percent BETWEEN 1 AND 100),
    valid_until      DATE NOT NULL
    );

-- ─── BOOKINGS ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookings (
                                        id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id      UUID NOT NULL REFERENCES clients(id),
    tour_id        UUID NOT NULL REFERENCES tours(id),
    promo_code_id  UUID REFERENCES promo_codes(id),
    tourist_count  INT NOT NULL CHECK (tourist_count > 0),
    booking_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'CREATED'
    CHECK (status IN ('CREATED','CONFIRMED','PAID','COMPLETED','CANCELLED')),
    total_price    NUMERIC(12, 2) NOT NULL CHECK (total_price >= 0),
    confirmed_at   TIMESTAMP,
    cancelled_at   TIMESTAMP,
    cancel_reason  TEXT
    );

-- Екскурсії обрані в бронюванні
CREATE TABLE IF NOT EXISTS booking_excursions (
                                                  booking_id   UUID NOT NULL REFERENCES bookings(id),
    excursion_id UUID NOT NULL REFERENCES excursions(id),
    PRIMARY KEY (booking_id, excursion_id)
    );

-- Страховка обрана в бронюванні
CREATE TABLE IF NOT EXISTS booking_insurances (
                                                  booking_id   UUID NOT NULL REFERENCES bookings(id),
    insurance_id UUID NOT NULL REFERENCES insurances(id),
    PRIMARY KEY (booking_id, insurance_id)
    );

-- Трансфер обраний в бронюванні
CREATE TABLE IF NOT EXISTS booking_transfers (
                                                 booking_id  UUID NOT NULL REFERENCES bookings(id),
    transfer_id UUID NOT NULL REFERENCES transfers(id),
    PRIMARY KEY (booking_id, transfer_id)
    );

-- ─── PAYMENTS ─────────────────────────────────────────────────────────────────
-- ВИПРАВЛЕННЯ:
--   1. Прибрано UNIQUE з booking_id — одне бронювання може мати кілька записів
--      (оригінальний платіж + рефанд при скасуванні).
--   2. CHECK (amount <> 0) замість amount > 0 — рефанд зберігається як від'ємна сума.
--   3. Додано 'REFUND' до дозволених статусів (BookingService використовує "REFUND",
--      а не "REFUNDED").
CREATE TABLE IF NOT EXISTS payments (
                                        id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id   UUID NOT NULL REFERENCES bookings(id),
    amount       NUMERIC(12, 2) NOT NULL CHECK (amount <> 0),
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED','REFUND')),
    method       VARCHAR(50)
    );

-- ─── WEATHER CACHE ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS weather_forecasts (
                                                 id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city          VARCHAR(100) NOT NULL,
    forecast_date DATE NOT NULL,
    temperature   NUMERIC(5, 1),
    description   VARCHAR(255),
    fetched_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (city, forecast_date)
    );

-- ─── INDEXES ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_bookings_client   ON bookings(client_id);
CREATE INDEX IF NOT EXISTS idx_bookings_tour     ON bookings(tour_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status   ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_tours_status      ON tours(status);
CREATE INDEX IF NOT EXISTS idx_tours_dates       ON tours(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_weather_city_date ON weather_forecasts(city, forecast_date);

-- ─── DEMO DATA ────────────────────────────────────────────────────────────────

-- Готелі
INSERT INTO hotels (id, name, stars, price_per_night) VALUES
                                                          ('11111111-0000-0000-0000-000000000001', 'Grand Palace Hotel', 5, 350.00),
                                                          ('11111111-0000-0000-0000-000000000002', 'Sunset Beach Resort', 4, 220.00),
                                                          ('11111111-0000-0000-0000-000000000003', 'City Center Inn', 3, 90.00)
    ON CONFLICT DO NOTHING;

-- Страховки
INSERT INTO insurances (id, name, price) VALUES
                                             ('22222222-0000-0000-0000-000000000001', 'Базова страховка', 25.00),
                                             ('22222222-0000-0000-0000-000000000002', 'Розширена страховка', 75.00),
                                             ('22222222-0000-0000-0000-000000000003', 'VIP страховка', 150.00)
    ON CONFLICT DO NOTHING;

-- Трансфери
INSERT INTO transfers (id, type, price) VALUES
                                            ('33333333-0000-0000-0000-000000000001', 'Автобус', 15.00),
                                            ('33333333-0000-0000-0000-000000000002', 'Мінівен', 40.00),
                                            ('33333333-0000-0000-0000-000000000003', 'Приватне авто', 80.00)
    ON CONFLICT DO NOTHING;

-- Промокоди
INSERT INTO promo_codes (id, code, discount_percent, valid_until) VALUES
                                                                      ('44444444-0000-0000-0000-000000000001', 'WELCOME10', 10, '2026-12-31'),
                                                                      ('44444444-0000-0000-0000-000000000002', 'SUMMER15',  15, '2026-09-01'),
                                                                      ('44444444-0000-0000-0000-000000000003', 'VIP25',     25, '2026-06-30')
    ON CONFLICT DO NOTHING;

-- Тури
INSERT INTO tours (id, name, country, city, start_date, end_date, base_price, quota, booked_seats, hotel_id, status) VALUES
                                                                                                                         ('a7b5dce2-836f-4271-8b2a-02c1d19b4bd6', 'Єгипет — Шарм-ель-Шейх', 'Єгипет', 'Sharm el-Sheikh',
                                                                                                                          '2026-07-01', '2026-07-14', 1200.00, 20, 0, '11111111-0000-0000-0000-000000000002', 'ACTIVE'),
                                                                                                                         ('b8c6edf3-9471-5382-9c3b-13d2e20c5ce7', 'Туреччина — Анталія', 'Туреччина', 'Antalya',
                                                                                                                          '2026-08-01', '2026-08-10', 800.00, 30, 0, '11111111-0000-0000-0000-000000000001', 'ACTIVE'),
                                                                                                                         ('c9d7fef4-0581-6493-ad4c-24e3f31d6df8', 'Греція — Санторіні', 'Греція', 'Santorini',
                                                                                                                          '2026-09-05', '2026-09-15', 1500.00, 15, 0, '11111111-0000-0000-0000-000000000001', 'ACTIVE')
    ON CONFLICT DO NOTHING;

-- Екскурсії
INSERT INTO excursions (id, name, price, excursion_date) VALUES
                                                             ('55555555-0000-0000-0000-000000000001', 'Піраміди Гізи', 80.00, '2026-07-05'),
                                                             ('55555555-0000-0000-0000-000000000002', 'Снорклінг на рифі', 45.00, '2026-07-08'),
                                                             ('55555555-0000-0000-0000-000000000003', 'Ефес — давньогрецьке місто', 60.00, '2026-08-05'),
                                                             ('55555555-0000-0000-0000-000000000004', 'Острів Санторіні — кальдера', 90.00, '2026-09-10')
    ON CONFLICT DO NOTHING;

-- Прив'язка екскурсій до турів
INSERT INTO tour_excursions (tour_id, excursion_id) VALUES
                                                        ('a7b5dce2-836f-4271-8b2a-02c1d19b4bd6', '55555555-0000-0000-0000-000000000001'),
                                                        ('a7b5dce2-836f-4271-8b2a-02c1d19b4bd6', '55555555-0000-0000-0000-000000000002'),
                                                        ('b8c6edf3-9471-5382-9c3b-13d2e20c5ce7', '55555555-0000-0000-0000-000000000003'),
                                                        ('c9d7fef4-0581-6493-ad4c-24e3f31d6df8', '55555555-0000-0000-0000-000000000004')
    ON CONFLICT DO NOTHING;

-- Тестовий клієнт
INSERT INTO clients (id, name, email, phone, birth_date) VALUES
    ('66666666-0000-0000-0000-000000000001', 'Іванна Петренко', 'ivanna@test.com', '+380991234567', '1995-03-15')
    ON CONFLICT DO NOTHING;