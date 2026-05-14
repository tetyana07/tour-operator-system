package com.touroperator.ui;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

/**
 * Сплеш-скрін AYVO — точна копія HTML-оригіналу.
 * Анімовані блоби, сітка, частинки, ring-pulse, прогрес-бар.
 */
public class SplashController {

    @FXML private Pane  rootPane;
    @FXML private Pane  particlesPane;
    @FXML private Pane  progressFill;
    @FXML private Pane  progressTrack;
    @FXML private Label loadingLabel;

    private Stage stage;

    @FXML
    public void initialize() {
        spawnParticles();
        animateProgress();
    }

    public void setStage(Stage stage) { this.stage = stage; }

    // ── 20 частинок (як у HTML) ──────────────────────────────────────────────
    private void spawnParticles() {
        Random rng = new Random();
        for (int i = 0; i < 20; i++) {
            double size  = 3 + rng.nextDouble() * 5;
            double x     = rng.nextDouble() * 900;
            double y     = 180 + rng.nextDouble() * 372; // 30%–90% of 620
            double dur   = 2500 + rng.nextDouble() * 3000;
            double delay = rng.nextDouble() * 3000;

            Circle c = new Circle(size / 2, Color.web("#97c459", 0));
            c.setCenterX(x);
            c.setCenterY(y);
            particlesPane.getChildren().add(c);

            Timeline tl = new Timeline(
                  new KeyFrame(Duration.ZERO,
                        new KeyValue(c.opacityProperty(), 0),
                        new KeyValue(c.centerYProperty(), y)),
                  new KeyFrame(Duration.millis(dur * 0.20),
                        new KeyValue(c.opacityProperty(), 0.7)),
                  new KeyFrame(Duration.millis(dur * 0.80),
                        new KeyValue(c.opacityProperty(), 0.3)),
                  new KeyFrame(Duration.millis(dur),
                        new KeyValue(c.opacityProperty(), 0),
                        new KeyValue(c.centerYProperty(), y - 120))
            );
            tl.setDelay(Duration.millis(delay));
            tl.setCycleCount(Animation.INDEFINITE);
            tl.play();
        }
    }

    // ── Реалістичне завантаження з паузами + dismiss після завершення
    private void animateProgress() {
        // Чекаємо поки JavaFX зробить layout і ми знатимемо реальну ширину треку
        progressTrack.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            double trackWidth = newVal.getWidth();
            if (trackWidth <= 0) return;

            // Слухаємо тільки один раз
            progressTrack.layoutBoundsProperty().removeListener((o, ov, nv) -> {});

            Timeline fill = new Timeline(
                  new KeyFrame(Duration.ZERO,
                        new KeyValue(progressFill.prefWidthProperty(), 0)),
                  new KeyFrame(Duration.millis(600),
                        new KeyValue(progressFill.prefWidthProperty(), trackWidth * 0.35,
                              Interpolator.SPLINE(0.4, 0, 0.2, 1))),
                  new KeyFrame(Duration.millis(1400),
                        new KeyValue(progressFill.prefWidthProperty(), trackWidth * 0.60,
                              Interpolator.SPLINE(0.1, 0, 0.3, 1))),
                  new KeyFrame(Duration.millis(2200),
                        new KeyValue(progressFill.prefWidthProperty(), trackWidth * 0.80,
                              Interpolator.SPLINE(0.2, 0, 0.4, 1))),
                  new KeyFrame(Duration.millis(2900),
                        new KeyValue(progressFill.prefWidthProperty(), trackWidth,
                              Interpolator.SPLINE(0.4, 0, 0.2, 1)))
            );
            fill.setDelay(Duration.millis(400));
            fill.play();
        });

        // Dismiss після завершення анімації + невелика пауза
        PauseTransition wait = new PauseTransition(Duration.millis(3600));
        wait.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(650), rootPane);
            fade.setToValue(0);
            fade.setOnFinished(ev -> openLogin());
            fade.play();
        });
        wait.play();
    }

    private void openLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                  getClass().getResource("/ui/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 900, 650);
            scene.getStylesheets().add(
                  getClass().getResource("/css/style.css").toExternalForm());
            LoginController ctrl = loader.getController();
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.setTitle("AYVO — Вхід");
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}