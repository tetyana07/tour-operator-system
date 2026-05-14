package com.touroperator.domain;

/**
 * Статуси туру.
 * ACTIVE  — тур активний, доступне бронювання
 * FULL    — всі місця зайняті
 * CANCELLED — тур скасовано
 * ARCHIVED  — архів (минулий тур)
 */
public enum TourStatus {
    ACTIVE,
    FULL,
    CANCELLED,
    ARCHIVED
}
