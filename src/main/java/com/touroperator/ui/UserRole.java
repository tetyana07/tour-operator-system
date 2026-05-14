package com.touroperator.ui;

/**
 * Ролі користувачів TourVision.
 *
 * ADMIN   – повний доступ: тури, бронювання, клієнти, платежі, промо, звіти, налаштування
 * MANAGER – тури, бронювання, клієнти, платежі, промо
 * CLIENT  – перегляд турів, особисті бронювання, профіль
 */
public enum UserRole {
    ADMIN("Адміністратор"),
    CLIENT("Клієнт");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
