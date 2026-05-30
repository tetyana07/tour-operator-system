package com.touroperator.config;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Глобальний обробник непійманих виключень для JavaFX-додатку.
 *
 * Реєструється один раз у MainApp.start() або Launcher.main():
 * <pre>
 *   GlobalExceptionHandler.register();
 * </pre>
 *
 * Перехоплює:
 *  - виключення у JavaFX Application Thread
 *  - виключення у будь-якому іншому потоці
 *
 * Поведінка:
 *  - логує помилку з повним stack trace
 *  - показує VoyaAlert.error() тільки для критичних помилок
 *  - НЕ крашає додаток при відновлюваних помилках
 */
public final class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private GlobalExceptionHandler() {}

    public static void register() {
        // Обробник для JavaFX Application Thread
        Thread.currentThread().setUncaughtExceptionHandler(
              GlobalExceptionHandler::handleException);

        // Обробник для всіх інших потоків (background tasks, тощо)
        Thread.setDefaultUncaughtExceptionHandler(
              GlobalExceptionHandler::handleException);

        log.info("Глобальний обробник виключень зареєстровано.");
    }

    private static void handleException(Thread thread, Throwable ex) {
        log.error("Непіймане виключення у потоці '{}': {}", thread.getName(), ex.getMessage(), ex);

        // Показуємо повідомлення тільки для критичних помилок
        if (isCritical(ex)) {
            String userMessage = buildUserMessage(ex);
            if (Platform.isFxApplicationThread()) {
                showAlert(userMessage);
            } else {
                Platform.runLater(() -> showAlert(userMessage));
            }
        }
    }

    private static boolean isCritical(Throwable ex) {
        // Ігноруємо дрібні помилки, які вже обробляються на рівні сервісів
        if (ex.getClass().getName().equals("java.lang.ThreadDeath")) return false;
        if (ex instanceof InterruptedException) return false;
        // Помилки БД — критичні, показуємо
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        return msg.contains("connection") || msg.contains("sql")
              || ex instanceof OutOfMemoryError
              || ex instanceof StackOverflowError
              || ex instanceof Error;
    }

    private static String buildUserMessage(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null) msg = ex.getClass().getSimpleName();
        String lower = msg.toLowerCase();

        if (lower.contains("connection refused") || lower.contains("could not connect")) {
            return "Немає з'єднання з базою даних.\n" +
                  "Переконайтесь, що PostgreSQL запущено.";
        }
        if (lower.contains("out of memory")) {
            return "Недостатньо пам'яті. Перезапустіть додаток.";
        }
        if (ex instanceof Error) {
            return "Критична системна помилка. Додаток потребує перезапуску.\n" + shortMsg(msg);
        }
        return "Неочікувана помилка:\n" + shortMsg(msg);
    }

    private static String shortMsg(String msg) {
        return msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
    }

    private static void showAlert(String message) {
        try {
            // Використовуємо існуючий VoyaAlert
            com.touroperator.ui.VoyaAlert.error(message);
        } catch (Exception e) {
            // Якщо VoyaAlert недоступний — logуємо
            log.error("Не вдалося показати alert: {}", message);
        }
    }
}