package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.dto.CurrencyRate;
import com.touroperator.service.CurrencyService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BottomStripController {

    @FXML private Label clockLabel;
    @FXML private Label lblUsdUah;
    @FXML private Label lblUsdEur;
    @FXML private Label lblCacheHint;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {

        updateClock();
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();


        Thread rateThread = new Thread(() -> {
            try {
                CurrencyService svc = SpringContext.getBean(CurrencyService.class);
                CurrencyRate rate = svc.getCurrentRates();
                Platform.runLater(() -> applyRates(rate));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblUsdUah != null) lblUsdUah.setText("курс недоступний");
                });
            }
        }, "currency-fetch");
        rateThread.setDaemon(true);
        rateThread.start();


        Timeline rateRefresh = new Timeline(
              new KeyFrame(Duration.hours(1), e -> refreshRates()));
        rateRefresh.setCycleCount(Animation.INDEFINITE);
        rateRefresh.play();
    }

    private void applyRates(CurrencyRate rate) {
        if (lblUsdUah != null)
            lblUsdUah.setText("$1 = ₴" + rate.getUsdToUah().toPlainString());
        if (lblUsdEur != null)
            lblUsdEur.setText("$1 = €" + rate.getUsdToEur().toPlainString());
        if (lblCacheHint != null)
            lblCacheHint.setText(rate.isFromCache() ? "(офлайн)" : "");
    }

    private void refreshRates() {
        Thread t = new Thread(() -> {
            try {
                CurrencyService svc = SpringContext.getBean(CurrencyService.class);
                CurrencyRate rate = svc.getCurrentRates();
                Platform.runLater(() -> applyRates(rate));
            } catch (Exception ignored) {}
        }, "currency-refresh");
        t.setDaemon(true);
        t.start();
    }

    private void updateClock() {
        if (clockLabel != null)
            clockLabel.setText(LocalTime.now().format(FMT));
    }
}