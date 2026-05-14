package com.touroperator.ui;

/**
 * Клас-обгортка для запуску JavaFX-додатку через Fat JAR.
 *
 * Не успадковується від Application — це критично для коректного
 * завантаження модулів JavaFX через classpath (не module-path).
 *
 * Розміщення файлу:
 *   src/main/java/com/touroperator/ui/Launcher.java
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}