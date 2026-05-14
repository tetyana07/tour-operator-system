package com.touroperator.domain;

/**
 * Статуси бронювання.
 * Lifecycle: CREATED → CONFIRMED → PAID → COMPLETED
 *                              ↘ CANCELLED (з будь-якого стану)
 */
public enum BookingStatus {
    CREATED,
    CONFIRMED,
    PAID,
    COMPLETED,
    CANCELLED
}
