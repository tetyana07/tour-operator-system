package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Booking;
import com.touroperator.dto.TourReport;
import com.touroperator.service.BookingService;
import com.touroperator.service.ReportService;
import com.touroperator.service.report.ReportParams;
import com.touroperator.service.report.RevenueExcelReport;
import javafx.animation.*;
import javafx.application.Platform;
import org.springframework.jdbc.core.JdbcTemplate;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class ReportsController {

    @FXML private HBox reportChartBars;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;

    // live stat labels
    @FXML private Label statRevenue;
    @FXML private Label statBookings;
    @FXML private Label statAvgCheck;
    @FXML private Label statFill;

    // dynamic tour quota table rows
    @FXML private VBox tourQuotaRows;
    @FXML private VBox directionsBox;

    private static final String[] MONTHS =
          {"Січ","Лют","Бер","Кві","Тра","Чер","Лип","Сер","Вер","Жов","Лис","Гру"};

    private boolean initialized = false;
    private JdbcTemplate jdbc;

    @FXML
    public void initialize() {
        if (initialized) return;
        initialized = true;

        if (dateFrom != null) dateFrom.setValue(LocalDate.now().withDayOfMonth(1));
        if (dateTo   != null) dateTo.setValue(LocalDate.now());

        try {
            BookingService bookingSvc = SpringContext.getBean(BookingService.class);
            ReportService  reportSvc  = SpringContext.getBean(ReportService.class);
            jdbc = SpringContext.getBean(JdbcTemplate.class);

            List<Booking> bookings = bookingSvc.findAll();
            buildChart(bookings);
            updateStats(reportSvc, bookings);
            buildTourQuotaTable(reportSvc);
            buildDirections(reportSvc);

            // Оновлення валюти в реальному часі
            ProfilePanelController.CurrencySession.addListener(() -> {
                try {
                    ReportService rs = SpringContext.getBean(ReportService.class);
                    BookingService bs = SpringContext.getBean(BookingService.class);
                    List<Booking> bks = bs.findAll();
                    Platform.runLater(() -> updateStats(rs, bks));
                } catch (Exception ignored) {}
            });

        } catch (Exception e) {
            buildFallbackChart();
        }
    }

    private void updateStats(ReportService reportSvc, List<Booking> bookings) {
        try {
            BigDecimal revenue = reportSvc.getTotalRevenue();

            // Активні = CONFIRMED + CREATED (не скасовані і не завершені)
            long activeBookings = bookings.stream()
                  .filter(b -> "CONFIRMED".equals(b.getStatus()) || "CREATED".equals(b.getStatus()))
                  .count();

            // Середній чек = виручка / кількість оплачених бронювань
            long paidCount = bookings.stream()
                  .filter(b -> "PAID".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus()))
                  .count();
            BigDecimal avgCheck = paidCount > 0
                  ? revenue.divide(BigDecimal.valueOf(paidCount), 0, java.math.RoundingMode.HALF_UP)
                  : BigDecimal.ZERO;

            List<TourReport> reports = reportSvc.getTourReports();
            double avgFill = reports.stream()
                  .mapToDouble(TourReport::getFillPercent).average().orElse(0);

            if (statRevenue  != null) statRevenue.setText(ProfilePanelController.CurrencySession.format(revenue));
            if (statBookings != null) statBookings.setText(String.valueOf(activeBookings));
            if (statAvgCheck != null) statAvgCheck.setText(ProfilePanelController.CurrencySession.format(avgCheck));
            if (statFill     != null) statFill.setText(String.format("%.0f%%", avgFill));
        } catch (Exception ignored) {}
    }

    private void buildTourQuotaTable(ReportService reportSvc) {
        if (tourQuotaRows == null) return;
        tourQuotaRows.getChildren().clear();

        List<TourReport> reports = reportSvc.getTopTours(10);
        for (TourReport r : reports) {
            double fill    = r.getFillPercent() / 100.0;
            String fillPct = String.format("%.0f%%", r.getFillPercent());
            String color   = fill >= 0.8 ? "#c07020" : fill >= 0.5 ? "#3b6d11" : "#27500a";

            HBox row = new HBox(0);
            row.getStyleClass().add("table-row");
            row.setAlignment(Pos.CENTER_LEFT);

            VBox nameBox = new VBox(2);
            HBox.setHgrow(nameBox, Priority.ALWAYS);
            Label nameLbl = new Label(r.getTourName());
            nameLbl.getStyleClass().add("td-normal");
            Label subLbl  = new Label(r.getTotalBookings() + " бронювань · "
                  + r.getTotalTourists() + " туристів");
            subLbl.getStyleClass().add("td-muted");
            nameBox.getChildren().addAll(nameLbl, subLbl);

            Label revLbl = new Label(ProfilePanelController.CurrencySession.format(r.getTotalRevenue()));
            revLbl.getStyleClass().add("td-normal");
            revLbl.setPrefWidth(130);

            ProgressBar bar = new ProgressBar(0);
            bar.setPrefWidth(130);
            if (fill >= 0.8) bar.setStyle("-fx-accent:#c07020;");
            else bar.getStyleClass().add("quota-bar");
            new Timeline(new KeyFrame(Duration.millis(600),
                  new KeyValue(bar.progressProperty(), fill, Interpolator.EASE_OUT))).play();

            Label pctLbl = new Label(fillPct);
            pctLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + color
                  + ";-fx-font-weight:bold;");

            HBox barBox = new HBox(8, bar, pctLbl);
            barBox.setPrefWidth(200);
            barBox.setAlignment(Pos.CENTER_LEFT);

            row.getChildren().addAll(nameBox, revLbl, barBox);
            tourQuotaRows.getChildren().add(row);
        }

        if (reports.isEmpty()) {
            Label empty = new Label("Немає даних про тури");
            empty.getStyleClass().add("td-muted");
            empty.setStyle("-fx-padding:16;");
            tourQuotaRows.getChildren().add(empty);
        }
    }

    private void buildChart(List<Booking> bookings) {
        if (reportChartBars == null) return;
        reportChartBars.getChildren().clear();

        // Очікувана виручка — всі активні бронювання (не CANCELLED)
        Map<Integer, BigDecimal> byMonth      = new HashMap<>();
        // Оплачено — тільки PAID + COMPLETED
        Map<Integer, BigDecimal> paidByMonth  = new HashMap<>();
        Map<Integer, Long>       countByMonth = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            byMonth.put(i, BigDecimal.ZERO);
            paidByMonth.put(i, BigDecimal.ZERO);
            countByMonth.put(i, 0L);
        }

        bookings.stream()
              .filter(b -> !"CANCELLED".equals(b.getStatus()))
              .filter(b -> b.getBookingDate() != null && b.getTotalPrice() != null)
              .forEach(b -> {
                  int month = b.getBookingDate().getMonthValue();
                  byMonth.merge(month, b.getTotalPrice(), BigDecimal::add);
                  countByMonth.merge(month, 1L, Long::sum);
              });

        bookings.stream()
              .filter(b -> "PAID".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus()))
              .filter(b -> b.getBookingDate() != null && b.getTotalPrice() != null)
              .forEach(b -> {
                  int month = b.getBookingDate().getMonthValue();
                  paidByMonth.merge(month, b.getTotalPrice(), BigDecimal::add);
              });

        BigDecimal max = byMonth.values().stream()
              .max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.ONE;

        final String[] MONTH_NAMES_UA = {
              "Січень","Лютий","Березень","Квітень","Травень","Червень",
              "Липень","Серпень","Вересень","Жовтень","Листопад","Грудень"
        };

        final BigDecimal maxFinal = max;
        for (int i = 1; i <= 12; i++) {
            double ratio = byMonth.get(i)
                  .divide(maxFinal, 4, java.math.RoundingMode.HALF_UP).doubleValue();
            double targetH = Math.max(4, ratio * 74);
            boolean accent = byMonth.get(i).compareTo(maxFinal) == 0;

            final long       bookingCount  = countByMonth.get(i);
            final BigDecimal expectedRev   = byMonth.get(i);
            final BigDecimal paidRev       = paidByMonth.get(i);
            final boolean    isAccent      = accent;
            final String     fullMonthName = MONTH_NAMES_UA[i - 1];

            VBox col = new VBox(4);
            col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setPrefHeight(90);

            Rectangle bar = new Rectangle(0, 4);
            bar.setArcWidth(4); bar.setArcHeight(4);
            bar.widthProperty().bind(col.widthProperty().subtract(4));
            bar.setStyle(accent ? "-fx-fill:#639922;" : "-fx-fill:#c0dd97;");

            // Tooltip: Місяць / Бронювань / роздільник / Очікувана / Оплачено
            String divider     = "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500";
            String tooltipText = fullMonthName
                  + "\nБронювань: " + bookingCount
                  + "\n" + divider
                  + "\nОчікувана: \u20b4 " + String.format("%,.0f", expectedRev)
                  + "\nОплачено:  \u20b4 " + String.format("%,.0f", paidRev);
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setStyle(
                  "-fx-background-color:#27500a;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:12px;" +
                        "-fx-font-family:monospace;" +
                        "-fx-padding:10 14 10 14;" +
                        "-fx-background-radius:10;"
            );
            tooltip.setShowDelay(javafx.util.Duration.millis(60));
            Tooltip.install(col, tooltip);

            col.setOnMouseEntered(e -> bar.setStyle(
                  isAccent ? "-fx-fill:#4a7a18;" : "-fx-fill:#97c459;"));
            col.setOnMouseExited(e -> bar.setStyle(
                  isAccent ? "-fx-fill:#639922;" : "-fx-fill:#c0dd97;"));
            col.setStyle("-fx-cursor:hand;");

            new Timeline(new KeyFrame(
                  Duration.millis(500 + i * 45),
                  new KeyValue(bar.heightProperty(), targetH, Interpolator.EASE_OUT)
            )).play();

            Label lbl = new Label(MONTHS[i - 1]);
            lbl.getStyleClass().add("chart-bar-lbl");
            col.getChildren().addAll(bar, lbl);
            reportChartBars.getChildren().add(col);
        }
    }


    private static final String[] DIR_COLORS = {
          "#3b6d11", "#639922", "#97c459", "#c8dfa8", "#a8c87a"
    };

    private void buildDirections(ReportService reportSvc) {
        if (directionsBox == null) return;
        directionsBox.getChildren().clear();
        try {
            java.util.LinkedHashMap<String, Integer> countries = reportSvc.getTopCountries(4);
            if (countries.isEmpty()) {
                Label lbl = new Label("Немає даних");
                lbl.getStyleClass().add("td-muted");
                directionsBox.getChildren().add(lbl);
                return;
            }
            int colorIdx = 0;
            for (java.util.Map.Entry<String, Integer> entry : countries.entrySet()) {
                String color = DIR_COLORS[Math.min(colorIdx, DIR_COLORS.length - 1)];
                boolean isOther = "Інші".equals(entry.getKey());

                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                javafx.scene.layout.Region dot = new javafx.scene.layout.Region();
                dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:4;");
                dot.setPrefSize(12, 12);
                dot.setMinSize(12, 12);
                dot.setMaxSize(12, 12);

                Label nameLbl = new Label(entry.getKey());
                nameLbl.getStyleClass().add("td-normal");
                javafx.scene.layout.HBox.setHgrow(nameLbl, javafx.scene.layout.Priority.ALWAYS);

                Label pctLbl = new Label(entry.getValue() + "%");
                if (isOther) {
                    pctLbl.setStyle("-fx-font-family:Syne;-fx-font-weight:bold;-fx-text-fill:#8a9a85;");
                } else {
                    pctLbl.getStyleClass().add("dir-pct-bold");
                }

                // Small percentage bar
                javafx.scene.layout.StackPane barTrack = new javafx.scene.layout.StackPane();
                barTrack.setPrefSize(60, 5);
                barTrack.setMaxSize(60, 5);
                barTrack.setStyle("-fx-background-color:#e8f0e0;-fx-background-radius:3;");

                javafx.scene.layout.Region barFill = new javafx.scene.layout.Region();
                double pctVal = Math.min(entry.getValue() / 100.0, 1.0);
                barFill.setPrefSize(60 * pctVal, 5);
                barFill.setMaxSize(60 * pctVal, 5);
                barFill.setStyle("-fx-background-color:" + color + ";-fx-background-radius:3;");
                javafx.scene.layout.StackPane.setAlignment(barFill, javafx.geometry.Pos.CENTER_LEFT);
                barTrack.getChildren().add(barFill);

                row.getChildren().addAll(dot, nameLbl, barTrack, pctLbl);
                directionsBox.getChildren().add(row);
                colorIdx++;
            }
        } catch (Exception e) {
            Label lbl = new Label("Помилка завантаження");
            lbl.getStyleClass().add("td-muted");
            directionsBox.getChildren().add(lbl);
        }
    }

    private void buildFallbackChart() {
        if (reportChartBars == null) return;
        double[] heights = {0.35,0.50,0.62,0.78,1.00,0.86,0.70,0.57,0.42,0.27,0.22,0.46};
        int[]    bookingCounts = {3, 5, 6, 8, 16, 12, 9, 7, 5, 3, 2, 5};
        reportChartBars.getChildren().clear();
        for (int i = 0; i < MONTHS.length; i++) {
            VBox col = new VBox(4);
            col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setPrefHeight(90);
            double targetH = heights[i] * 74;
            final boolean isAccent = (i == 4);
            final int cnt = bookingCounts[i];

            Rectangle bar = new Rectangle(0, 4);
            bar.setArcWidth(4); bar.setArcHeight(4);
            bar.widthProperty().bind(col.widthProperty().subtract(4));
            bar.setStyle(isAccent ? "-fx-fill:#639922;" : "-fx-fill:#c0dd97;");

            Tooltip tooltip = new Tooltip(MONTHS[i] + "\nБронювань: " + cnt);
            tooltip.setStyle(
                  "-fx-background-color:#27500a;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:12px;" +
                        "-fx-padding:8 12 8 12;" +
                        "-fx-background-radius:8;"
            );
            tooltip.setShowDelay(javafx.util.Duration.millis(80));
            Tooltip.install(col, tooltip);

            col.setOnMouseEntered(e -> bar.setStyle(
                  isAccent ? "-fx-fill:#4a7a18;" : "-fx-fill:#97c459;"));
            col.setOnMouseExited(e -> bar.setStyle(
                  isAccent ? "-fx-fill:#639922;" : "-fx-fill:#c0dd97;"));
            col.setStyle("-fx-cursor:hand;");

            new Timeline(new KeyFrame(Duration.millis(500 + i * 45),
                  new KeyValue(bar.heightProperty(), targetH, Interpolator.EASE_OUT))).play();
            Label lbl = new Label(MONTHS[i]);
            lbl.getStyleClass().add("chart-bar-lbl");
            col.getChildren().addAll(bar, lbl);
            reportChartBars.getChildren().add(col);
        }
    }

    @FXML
    private void onApplyFilter() {
        try {
            BookingService bookingSvc = SpringContext.getBean(BookingService.class);
            ReportService  reportSvc  = SpringContext.getBean(ReportService.class);

            LocalDate from = dateFrom != null ? dateFrom.getValue() : null;
            LocalDate to   = dateTo   != null ? dateTo.getValue()   : null;

            List<Booking> bookings = bookingSvc.findAll().stream()
                  .filter(b -> {
                      if (b.getBookingDate() == null) return true;
                      if (from != null && b.getBookingDate().isBefore(from)) return false;
                      if (to   != null && b.getBookingDate().isAfter(to))   return false;
                      return true;
                  })
                  .collect(java.util.stream.Collectors.toList());

            buildChart(bookings);
            updateStats(reportSvc, bookings);
            buildTourQuotaTable(reportSvc);
            buildDirections(reportSvc);
        } catch (Exception e) {
            VoyaAlert.error("Помилка фільтрації:\n" + e.getMessage());
        }
    }

    @FXML
    private void onExportExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Зберегти Excel звіт");
        chooser.getExtensionFilters().add(
              new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        chooser.setInitialFileName("revenue_report.xlsx");

        File file = chooser.showSaveDialog(
              reportChartBars != null ? reportChartBars.getScene().getWindow() : null);
        if (file == null) return;

        try {
            RevenueExcelReport report = SpringContext.getBean(RevenueExcelReport.class);
            LocalDate from = dateFrom != null ? dateFrom.getValue() : null;
            LocalDate to   = dateTo   != null ? dateTo.getValue()   : null;
            report.generate(new ReportParams(from, to), file.getAbsolutePath());

            // Сповіщення про скачування
            try {
                com.touroperator.service.NotificationService notifSvc =
                      SpringContext.getBean(com.touroperator.service.NotificationService.class);
                notifSvc.notifyExcelExported(file.getName());
            } catch (Exception ignored) {}

            VoyaAlert.success("Звіт збережено:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            VoyaAlert.error("Помилка генерації звіту:\n" + e.getMessage());
        }
    }
}