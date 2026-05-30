package com.touroperator.ui.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Утилітний клас для асинхронного завантаження даних із БД у JavaFX.
 *
 * Вирішує проблему: без нього всі запити до БД виконуються в JavaFX Application Thread
 * і заморожують UI при повільних запитах.
 *
 * Використання у контролері:
 * <pre>
 *   AsyncDataLoader.load(
 *       bookingService::findAll,          // викликається в background thread
 *       bookings -> {                     // викликається в UI thread
 *           allBookings.setAll(bookings);
 *           updateCountLabel();
 *       },
 *       error -> VoyaAlert.error(error),  // обробник помилок
 *       loadingSpinner                    // показується під час завантаження
 *   );
 * </pre>
 */
public class AsyncDataLoader {

    private static final Executor POOL =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "voya-data-loader");
                t.setDaemon(true); // не блокує завершення додатку
                return t;
            });

    /**
     * Завантажує дані асинхронно.
     *
     * @param supplier  Постачальник даних (виконується у фоновому потоці)
     * @param onSuccess Обробник успіху (виконується в JavaFX Application Thread)
     * @param onError   Обробник помилки (виконується в JavaFX Application Thread)
     * @param spinner   Необов'язковий ProgressIndicator — показується під час завантаження
     */
    public static <T> void load(Supplier<T> supplier,
                                 Consumer<T> onSuccess,
                                 Consumer<String> onError,
                                 ProgressIndicator spinner) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return supplier.get();
            }
        };

        task.setOnSucceeded(e -> {
            hideSpinner(spinner);
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            hideSpinner(spinner);
            Throwable ex = task.getException();
            String msg = buildUserMessage(ex);
            onError.accept(msg);
        });

        showSpinner(spinner);
        POOL.execute(task);
    }

    /** Спрощена версія без spinner */
    public static <T> void load(Supplier<T> supplier,
                                 Consumer<T> onSuccess,
                                 Consumer<String> onError) {
        load(supplier, onSuccess, onError, null);
    }

    /**
     * Виконує дію (без повернення значення) асинхронно.
     * Зручно для save/delete операцій.
     */
    public static void run(Runnable action,
                            Runnable onSuccess,
                            Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                action.run();
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(onSuccess));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> onError.accept(buildUserMessage(ex)));
        });

        POOL.execute(task);
    }

    // ── Offline-режим: людські повідомлення про помилки БД ───────────────

    private static String buildUserMessage(Throwable ex) {
        if (ex == null) return "Невідома помилка";
        String msg = ex.getMessage();
        if (msg == null) msg = ex.getClass().getSimpleName();

        // Визначаємо тип помилки і показуємо зрозуміле повідомлення
        if (isConnectionError(msg)) {
            return "Немає з'єднання з базою даних.\n" +
                   "Перевірте, чи запущено PostgreSQL, і спробуйте ще раз.\n\n" +
                   "Технічні деталі: " + shortMessage(msg);
        }
        if (isTimeoutError(msg)) {
            return "Сервер бази даних не відповідає (timeout).\n" +
                   "Можливо, сервер перевантажений. Спробуйте через кілька секунд.";
        }
        if (isAuthError(msg)) {
            return "Помилка авторизації бази даних.\n" +
                   "Зверніться до адміністратора системи.";
        }

        // Загальна помилка — показуємо скорочено
        return "Помилка при роботі з даними:\n" + shortMessage(msg);
    }

    private static boolean isConnectionError(String msg) {
        String lower = msg.toLowerCase();
        return lower.contains("connection refused")
            || lower.contains("could not connect")
            || lower.contains("communications link failure")
            || lower.contains("unable to acquire jdbc")
            || lower.contains("connection is not available");
    }

    private static boolean isTimeoutError(String msg) {
        String lower = msg.toLowerCase();
        return lower.contains("timeout") || lower.contains("timed out");
    }

    private static boolean isAuthError(String msg) {
        String lower = msg.toLowerCase();
        return lower.contains("password authentication failed")
            || lower.contains("access denied for user");
    }

    private static String shortMessage(String msg) {
        return msg.length() > 120 ? msg.substring(0, 120) + "..." : msg;
    }

    private static void showSpinner(ProgressIndicator spinner) {
        if (spinner != null) Platform.runLater(() -> {
            spinner.setVisible(true);
            spinner.setManaged(true);
        });
    }

    private static void hideSpinner(ProgressIndicator spinner) {
        if (spinner != null) Platform.runLater(() -> {
            spinner.setVisible(false);
            spinner.setManaged(false);
        });
    }
}
