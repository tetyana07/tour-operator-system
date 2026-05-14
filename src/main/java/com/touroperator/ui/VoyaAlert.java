package com.touroperator.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Кастомний стилізований діалог для додатку VOYA.
 * Замінює стандартний JavaFX Alert, відповідаючи дизайну програми.
 */
public class VoyaAlert {

    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    /**
     * Показати інформаційне / успішне повідомлення.
     */
    public static void info(String message) {
        show(Type.INFO, message, null);
    }

    public static void info(String message, Window owner) {
        show(Type.INFO, message, owner);
    }

    public static void success(String message) {
        show(Type.SUCCESS, message, null);
    }

    public static void success(String message, Window owner) {
        show(Type.SUCCESS, message, owner);
    }

    public static void warning(String message) {
        show(Type.WARNING, message, null);
    }

    public static void warning(String message, Window owner) {
        show(Type.WARNING, message, owner);
    }

    public static void error(String message) {
        show(Type.ERROR, message, null);
    }

    public static void error(String message, Window owner) {
        show(Type.ERROR, message, owner);
    }

    /**
     * Діалог підтвердження з кнопками "Так" / "Скасувати".
     * onConfirm виконується якщо користувач натиснув "Так".
     */
    public static void confirm(String message, Runnable onConfirm) {
        showConfirm(message, null, onConfirm);
    }

    public static void confirm(String message, Window owner, Runnable onConfirm) {
        showConfirm(message, owner, onConfirm);
    }

    private static void showConfirm(String message, Window owner, Runnable onConfirm) {
        // Знаходимо активне вікно якщо owner не передано
        Window effectiveOwner = owner;
        if (effectiveOwner == null) {
            effectiveOwner = javafx.stage.Window.getWindows().stream()
                  .filter(w -> w instanceof Stage && w.isShowing() && w.isFocused())
                  .findFirst()
                  .orElse(javafx.stage.Window.getWindows().stream()
                        .filter(w -> w instanceof Stage && w.isShowing())
                        .findFirst().orElse(null));
        }
        final Window finalOwner = effectiveOwner;

        // UNDECORATED — надійно отримує фокус на Windows; rounded corners через CSS картки
        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (finalOwner != null) stage.initOwner(finalOwner);

        Label titleLabel = new Label("Підтвердження");
        titleLabel.setStyle("-fx-font-family: 'Syne'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #173404;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6b7280; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-color: transparent; -fx-padding: 2 6 2 6;");
        closeBtn.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(new HBox(titleLabel), spacer, closeBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 16, 12, 16));
        topBar.setStyle("-fx-border-color: transparent transparent rgba(99,153,34,0.12) transparent; -fx-border-width: 0 0 1 0;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 13.5px; -fx-text-fill: #374151; -fx-wrap-text: true; -fx-line-spacing: 3;");
        msgLabel.setMaxWidth(360);
        msgLabel.setWrapText(true);
        VBox msgArea = new VBox(msgLabel);
        msgArea.setPadding(new Insets(16, 20, 8, 20));

        Button cancelBtn = new Button("Скасувати");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-background-radius: 10; -fx-padding: 9 20 9 20; -fx-cursor: hand; -fx-border-color: rgba(99,153,34,0.25); -fx-border-radius: 10; -fx-border-width: 1.5;");
        cancelBtn.setOnAction(e -> stage.close());

        Button confirmBtn = new Button("Так, продовжити");
        confirmBtn.setStyle("-fx-background-color: #b83c2a; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 9 20 9 20; -fx-cursor: hand; -fx-border-color: transparent;");
        confirmBtn.setOnAction(e -> { stage.close(); if (onConfirm != null) onConfirm.run(); });

        HBox btnRow = new HBox(10, cancelBtn, confirmBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 20, 16, 20));

        Region accent = new Region();
        accent.setPrefHeight(3);
        accent.setStyle("-fx-background-color: linear-gradient(to right, #e07060, #b83c2a); -fx-background-radius: 0 0 14 14;");

        VBox card = new VBox(topBar, msgArea, btnRow, accent);
        card.setStyle(
              "-fx-background-color: #fdfcf7;" +
                    "-fx-background-radius: 16;" +
                    "-fx-border-color: rgba(184,60,42,0.25);" +
                    "-fx-border-radius: 16;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );
        card.setMinWidth(380);
        card.setMaxWidth(460);
        card.setPrefWidth(420);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: #fdfcf7; -fx-background-radius: 16;");
        root.setPadding(new Insets(0));
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.web("#fdfcf7"));
        try {
            var cssUrl = VoyaAlert.class.getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.sizeToScene();
        // Центруємо відносно батьківського вікна після показу
        if (finalOwner != null) {
            stage.setOnShown(e -> {
                stage.setX(finalOwner.getX() + (finalOwner.getWidth()  - stage.getWidth())  / 2);
                stage.setY(finalOwner.getY() + (finalOwner.getHeight() - stage.getHeight()) / 2);
            });
        } else {
            stage.centerOnScreen();
        }
        stage.showAndWait();
    }

    private static void show(Type type, String message, Window owner) {
        Window effectiveOwner = owner;
        if (effectiveOwner == null) {
            effectiveOwner = javafx.stage.Window.getWindows().stream()
                  .filter(w -> w instanceof Stage && w.isShowing() && w.isFocused())
                  .findFirst()
                  .orElse(javafx.stage.Window.getWindows().stream()
                        .filter(w -> w instanceof Stage && w.isShowing())
                        .findFirst().orElse(null));
        }
        final Window finalOwner = effectiveOwner;

        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (finalOwner != null) stage.initOwner(finalOwner);

        // ── Іконка та акценти за типом ──
        String icon;
        String accentColor;
        String bgColor;
        String titleText;

        switch (type) {
            case SUCCESS -> {
                icon = "✓";
                accentColor = "#27500a";
                bgColor = "#eaf3de";
                titleText = "Успішно";
            }
            case WARNING -> {
                icon = "⚠";
                accentColor = "#8a6000";
                bgColor = "#fff8e1";
                titleText = "Увага";
            }
            case ERROR -> {
                icon = "✕";
                accentColor = "#b83c2a";
                bgColor = "#fdecea";
                titleText = "Помилка";
            }
            default -> { // INFO
                icon = "i";
                accentColor = "#27500a";
                bgColor = "#eaf3de";
                titleText = "Повідомлення";
            }
        }

        // ── Іконка ──
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
              "-fx-font-family: 'DM Sans';" +
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + accentColor + ";" +
                    "-fx-alignment: CENTER;" +
                    "-fx-min-width: 38; -fx-min-height: 38;" +
                    "-fx-max-width: 38; -fx-max-height: 38;" +
                    "-fx-background-color: " + bgColor + ";" +
                    "-fx-background-radius: 50;"
        );

        // ── Заголовок ──
        Label titleLabel = new Label(titleText);
        titleLabel.setStyle(
              "-fx-font-family: 'Syne';" +
                    "-fx-font-size: 15px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #173404;"
        );

        // ── Кнопка закриття ──
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
              "-fx-background-color: transparent;" +
                    "-fx-text-fill: #6b7280;" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: transparent;" +
                    "-fx-padding: 2 6 2 6;"
        );
        closeBtn.setOnAction(e -> stage.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
              "-fx-background-color: #f3f4f6;" +
                    "-fx-text-fill: #374151;" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: hand;" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: transparent;" +
                    "-fx-padding: 2 6 2 6;"
        ));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
              "-fx-background-color: transparent;" +
                    "-fx-text-fill: #6b7280;" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: transparent;" +
                    "-fx-padding: 2 6 2 6;"
        ));

        // ── Рядок заголовку ──
        HBox headerRow = new HBox(10, titleLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(headerRow, spacer, closeBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 16, 12, 16));
        topBar.setStyle(
              "-fx-border-color: transparent transparent rgba(99,153,34,0.12) transparent;" +
                    "-fx-border-width: 0 0 1 0;"
        );

        // ── Текст повідомлення ──
        Label msgLabel = new Label(message);
        msgLabel.setStyle(
              "-fx-font-family: 'DM Sans';" +
                    "-fx-font-size: 13.5px;" +
                    "-fx-text-fill: #374151;" +
                    "-fx-wrap-text: true;" +
                    "-fx-line-spacing: 3;"
        );
        msgLabel.setMaxWidth(340);
        msgLabel.setWrapText(true);

        VBox msgArea = new VBox(msgLabel);
        msgArea.setPadding(new Insets(16, 20, 8, 20));

        // ── Кнопка OK ──
        Button okBtn = new Button("OK");
        okBtn.setStyle(
              "-fx-background-color: #27500a;" +
                    "-fx-text-fill: #eaf3de;" +
                    "-fx-font-family: 'DM Sans';" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 9 28 9 28;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: transparent;"
        );
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(
              "-fx-background-color: #173404;" +
                    "-fx-text-fill: #eaf3de;" +
                    "-fx-font-family: 'DM Sans';" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 9 28 9 28;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: transparent;"
        ));
        okBtn.setOnMouseExited(e -> okBtn.setStyle(
              "-fx-background-color: #27500a;" +
                    "-fx-text-fill: #eaf3de;" +
                    "-fx-font-family: 'DM Sans';" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 9 28 9 28;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: transparent;"
        ));
        okBtn.setOnAction(e -> stage.close());

        // ── Кольоровий акцент знизу (смужка) ──
        Region accent = new Region();
        accent.setPrefHeight(3);
        accent.setStyle("-fx-background-color: linear-gradient(to right, #97c459, #27500a); -fx-background-radius: 0 0 14 14;");

        HBox btnRow = new HBox(okBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 20, 16, 20));

        // ── Основний контейнер ──
        VBox card = new VBox(topBar, msgArea, btnRow, accent);
        card.setStyle(
              "-fx-background-color: #fdfcf7;" +
                    "-fx-background-radius: 16;" +
                    "-fx-border-color: rgba(99,153,34,0.18);" +
                    "-fx-border-radius: 16;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );
        card.setMinWidth(360);
        card.setMaxWidth(420);
        card.setPrefWidth(400);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: #fdfcf7;");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.web("#fdfcf7"));

        try {
            var cssUrl = VoyaAlert.class.getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.sizeToScene();
        if (finalOwner != null) {
            stage.setOnShown(e -> {
                stage.setX(finalOwner.getX() + (finalOwner.getWidth()  - stage.getWidth())  / 2);
                stage.setY(finalOwner.getY() + (finalOwner.getHeight() - stage.getHeight()) / 2);
            });
        } else {
            stage.centerOnScreen();
        }
        stage.showAndWait();
    }
}