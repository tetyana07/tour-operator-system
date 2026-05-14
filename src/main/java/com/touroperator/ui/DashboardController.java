package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Booking;
import com.touroperator.domain.Tour;
import com.touroperator.service.BookingService;
import com.touroperator.service.TourService;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Контролер дашборду — показує реальну статистику з БД.
 */
public class DashboardController {

    @FXML private Label statRevenue;
    @FXML private Label statBookings;
    @FXML private Label statTours;
    @FXML private Label statFillRate;
    @FXML private GridPane toursGrid;
    @FXML private HBox chartBars;

    private static final String[] MONTHS = {"Січ","Лют","Бер","Кві","Тра","Чер","Лип","Сер","Вер","Жов","Лис","Гру"};
    private static final String[] GRADIENTS = {
          "linear-gradient(from 0% 0% to 100% 100%,#1d5a3a,#639922,#c0dd97)",
          "linear-gradient(from 0% 0% to 100% 100%,#1a2030,#2a4060,#7eb8d4)",
          "linear-gradient(from 0% 0% to 100% 100%,#2a1a2a,#4a2a4a,#c08080)",
          "linear-gradient(from 0% 0% to 100% 100%,#1a2030,#2a3850,#7ab0d0)",
          "linear-gradient(from 0% 0% to 100% 100%,#2a1a0a,#504020,#c08040)",
          "linear-gradient(from 0% 0% to 100% 100%,#0a1a2a,#1a3050,#6090c0)",
    };

    @FXML
    public void initialize() {
        try {
            TourService tourService = SpringContext.getBean(TourService.class);
            BookingService bookingService = SpringContext.getBean(BookingService.class);

            List<Tour> tours = tourService.findAll();
            List<Booking> bookings = bookingService.findAll();

            loadStats(tours, bookings);
            buildTourCards(tours);
            buildChart(bookings);

            // Перемальовуємо при зміні валюти
            ProfilePanelController.CurrencySession.addListener(
                  () -> javafx.application.Platform.runLater(() -> {
                      loadStats(tours, bookings);
                      buildTourCards(tours);
                  })
            );
        } catch (Exception e) {
            showFallback();
        }
    }

    private void loadStats(List<Tour> tours, List<Booking> bookings) {
        // Виручка — сума всіх не скасованих бронювань
        BigDecimal revenue = bookings.stream()
              .filter(b -> !"CANCELLED".equals(b.getStatus()))
              .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeBookings = bookings.stream()
              .filter(b -> !Set.of("CANCELLED","COMPLETED").contains(b.getStatus()))
              .count();

        long activeTours = tours.stream()
              .filter(t -> t.getAvailableSeats() > 0)
              .count();

        double avgFill = tours.isEmpty() ? 0 :
              tours.stream().mapToDouble(Tour::getFillRate).average().orElse(0);

        if (statRevenue != null)
            statRevenue.setText(ProfilePanelController.CurrencySession.format(java.math.BigDecimal.valueOf(revenue.longValue())));
        if (statBookings != null)
            statBookings.setText(String.valueOf(activeBookings));
        if (statTours != null)
            statTours.setText(String.valueOf(activeTours));
        if (statFillRate != null)
            statFillRate.setText(String.format("%.0f%%", avgFill * 100));
    }

    private void buildTourCards(List<Tour> tours) {
        if (toursGrid == null) return;
        toursGrid.getChildren().clear();

        List<Tour> display = tours.stream().limit(6).collect(Collectors.toList());
        String[] emojis = {"🏖️","🗼","🌸","🏔️","🏜️","🌅"};

        for (int i = 0; i < display.size(); i++) {
            Tour t = display.get(i);
            String emoji = i < emojis.length ? emojis[i] : "✈️";
            String gradient = GRADIENTS[i % GRADIENTS.length];
            VBox card = buildCard(t, emoji, gradient);

            card.setOpacity(0); card.setTranslateY(10);
            int delay = 80 + i * 60;
            FadeTransition ft = new FadeTransition(Duration.millis(380), card);
            ft.setDelay(Duration.millis(delay));
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(380), card);
            tt.setDelay(Duration.millis(delay));
            tt.setFromY(10); tt.setToY(0);
            new ParallelTransition(ft, tt).play();

            toursGrid.add(card, i % 3, i / 3);
        }
    }

    private VBox buildCard(Tour t, String emoji, String gradient) {
        StackPane img = new StackPane();
        img.setPrefHeight(100); img.setMinHeight(100); img.setMaxHeight(100);
        img.setStyle("-fx-background-color:" + gradient + ";-fx-background-radius:18 18 0 0;");

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size:34px;");

        Label dest = new Label(t.getCountry() + ", " + t.getCity());
        dest.getStyleClass().add("tour-dest-lbl");
        StackPane.setAlignment(dest, Pos.BOTTOM_LEFT);
        StackPane.setMargin(dest, new Insets(0, 0, 8, 10));

        double fill = t.getFillRate();
        String badgeClass = fill >= 0.9 ? "tour-badge-full" :
              t.getStartDate().isAfter(java.time.LocalDate.now().plusMonths(2)) ?
                    "tour-badge-soon" : "tour-badge-open";
        String badgeText = fill >= 0.9 ? "Майже повний" :
              t.getStartDate().isAfter(java.time.LocalDate.now().plusMonths(2)) ?
                    "Незабаром" : "Відкрито";

        Label badge = new Label(badgeText);
        badge.getStyleClass().add(badgeClass);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(8, 8, 0, 0));

        img.getChildren().addAll(emojiLbl, dest, badge);

        VBox body = new VBox(5);
        body.getStyleClass().add("tour-body-pane");

        HBox row1 = new HBox();
        row1.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(t.getName());
        name.getStyleClass().add("tour-name-lbl");
        HBox.setHgrow(name, Priority.ALWAYS);
        Label price = new Label(ProfilePanelController.CurrencySession.format(t.getBasePrice()));
        price.getStyleClass().add("tour-price-lbl");
        row1.getChildren().addAll(name, price);

        HBox meta = new HBox(8);
        Label dates = new Label("📅 " + t.getStartDate() + "–" + t.getEndDate());
        dates.getStyleClass().add("tour-meta-lbl");
        meta.getChildren().add(dates);

        HBox quotaHead = new HBox();
        Label ql = new Label("Місця");
        ql.getStyleClass().add("quota-lbl");
        HBox.setHgrow(ql, Priority.ALWAYS);
        Label qr = new Label(t.getBookedSeats() + "/" + t.getQuota());
        qr.getStyleClass().add("quota-lbl");
        quotaHead.getChildren().addAll(ql, qr);

        ProgressBar bar = new ProgressBar(fill);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("quota-bar");

        body.getChildren().addAll(row1, meta, quotaHead, bar);

        VBox card = new VBox(img, body);
        card.getStyleClass().add("tour-card");
        return card;
    }

    private void buildChart(List<Booking> bookings) {
        if (chartBars == null) return;
        chartBars.getChildren().clear();

        // Реальна виручка по місяцях
        Map<Integer, BigDecimal> byMonth = new HashMap<>();
        for (int i = 1; i <= 12; i++) byMonth.put(i, BigDecimal.ZERO);

        bookings.stream()
              .filter(b -> !"CANCELLED".equals(b.getStatus()))
              .filter(b -> b.getBookingDate() != null && b.getTotalPrice() != null)
              .forEach(b -> {
                  int month = b.getBookingDate().getMonthValue();
                  byMonth.merge(month, b.getTotalPrice(), BigDecimal::add);
              });

        BigDecimal max = byMonth.values().stream()
              .max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.ONE;

        for (int i = 1; i <= 12; i++) {
            double ratio = byMonth.get(i).divide(max, 4, java.math.RoundingMode.HALF_UP).doubleValue();
            double targetH = Math.max(4, ratio * 64);
            boolean accent = byMonth.get(i).equals(max);

            VBox col = new VBox(4);
            col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setPrefHeight(80);

            Rectangle bar = new Rectangle(0, 4);
            bar.setArcWidth(4); bar.setArcHeight(4);
            bar.widthProperty().bind(col.widthProperty().subtract(4));
            bar.setStyle(accent ? "-fx-fill:#639922;" : "-fx-fill:#c0dd97;");

            new Timeline(new KeyFrame(
                  Duration.millis(500 + i * 45),
                  new KeyValue(bar.heightProperty(), targetH, Interpolator.EASE_OUT)
            )).play();

            Label lbl = new Label(MONTHS[i - 1]);
            lbl.getStyleClass().add("chart-bar-lbl");
            col.getChildren().addAll(bar, lbl);
            chartBars.getChildren().add(col);
        }
    }

    private void showFallback() {
        if (statRevenue != null) statRevenue.setText("—");
        if (statBookings != null) statBookings.setText("—");
        if (statTours != null) statTours.setText("—");
        if (statFillRate != null) statFillRate.setText("—");
    }

    private String formatNumber(long n) {
        return String.format("%,d", n).replace(',', '\u00A0');
    }
}