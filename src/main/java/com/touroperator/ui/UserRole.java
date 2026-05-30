package com.touroperator.ui;


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
