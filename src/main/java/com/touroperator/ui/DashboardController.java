package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Booking;
import com.touroperator.domain.Tour;
import com.touroperator.service.BookingService;
import com.touroperator.service.ReportService;
import com.touroperator.service.TourService;
import com.touroperator.ui.util.AsyncDataLoader;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


public class DashboardController {

     
    @FXML private Label greetingLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label chartYearLabel;

     
    @FXML private Label statRevenue;
    @FXML private Label statBookings;
    @FXML private Label statTours;
    @FXML private Label statFillRate;
    @FXML private Label statRevenueChange;
    @FXML private Label statBookingsChange;
    @FXML private Label statToursChange;
    @FXML private Label statFillChange;

     
    @FXML private GridPane toursGrid;

     
    @FXML private HBox    chartBars;
    @FXML private VBox    pieChartContainer;
    @FXML private VBox    countriesContainer;

     
    @FXML private VBox recentBookingsContainer;

    private static final String[] MONTHS =
          {"Січ","Лют","Бер","Кві","Тра","Чер","Лип","Сер","Вер","Жов","Лис","Гру"};
    private static final String[] GRADIENTS = {
          "linear-gradient(from 0% 0% to 100% 100%,#1d5a3a,#639922,#c0dd97)",
          "linear-gradient(from 0% 0% to 100% 100%,#1a2030,#2a4060,#7eb8d4)",
          "linear-gradient(from 0% 0% to 100% 100%,#2a1a2a,#4a2a4a,#c08080)",
          "linear-gradient(from 0% 0% to 100% 100%,#1a2030,#2a3850,#7ab0d0)",
          "linear-gradient(from 0% 0% to 100% 100%,#2a1a0a,#504020,#c08040)",
          "linear-gradient(from 0% 0% to 100% 100%,#0a1a2a,#1a3050,#6090c0)",
    };

     
    private static final Map<String, Color> STATUS_COLORS = Map.of(
          "CREATED",   Color.web("#7eb8d4"),
          "CONFIRMED", Color.web("#639922"),
          "PAID",      Color.web("#c0dd97"),
          "COMPLETED", Color.web("#1d5a3a"),
          "CANCELLED", Color.web("#c08080")
    );

    @FXML
    public void initialize() {
         
        if (chartYearLabel != null)
            chartYearLabel.setText(String.valueOf(java.time.LocalDate.now().getYear()));

        if (greetingLabel != null) {
            String user = ProfilePanelController.SessionState.getDisplayName();
            greetingLabel.setText("Привіт, " + (user != null ? user : "адміне") + " 👋");
        }
        if (subtitleLabel != null) {
            String today = java.time.LocalDate.now().format(
                  java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy",
                        new java.util.Locale("uk")));
            subtitleLabel.setText("Сьогодні " + today);
        }

        AsyncDataLoader.load(
              () -> {
                  TourService    tourSvc    = SpringContext.getBean(TourService.class);
                  BookingService bookingSvc = SpringContext.getBean(BookingService.class);
                  ReportService  reportSvc  = SpringContext.getBean(ReportService.class);
                  return new DashboardData(
                        tourSvc.findAll(),
                        bookingSvc.findAll(),
                        reportSvc.getBookingStatusStats(),
                        reportSvc.getTopCountries(5)
                  );
              },
              data -> {
                  loadStats(data.tours, data.bookings);
                  buildTourCards(data.tours);
                  buildRevenueChart(data.bookings);
                  buildPieChart(data.statusStats);
                  buildCountriesChart(data.topCountries);
                  buildRecentBookings(data.bookings, data.tours);

                  ProfilePanelController.CurrencySession.addListener(
                        () -> javafx.application.Platform.runLater(() -> {
                            loadStats(data.tours, data.bookings);
                            buildTourCards(data.tours);
                        })
                  );
              },
              error -> showFallback()
        );
    }

     

    private void loadStats(List<Tour> tours, List<Booking> bookings) {
        BigDecimal revenue = bookings.stream()
              .filter(b -> !"CANCELLED".equals(b.getStatus()))
              .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeBookings = bookings.stream()
              .filter(b -> !Set.of("CANCELLED","COMPLETED").contains(b.getStatus()))
              .count();

        long activeTours = tours.stream()
              .filter(t -> t.getAvailableSeats() > 0).count();

        double avgFill = tours.isEmpty() ? 0 :
              tours.stream().mapToDouble(Tour::getFillRate).average().orElse(0);

        if (statRevenue  != null) statRevenue.setText(
              ProfilePanelController.CurrencySession.format(revenue));
        if (statBookings != null) statBookings.setText(String.valueOf(activeBookings));
        if (statTours    != null) statTours.setText(String.valueOf(activeTours));
        if (statFillRate != null) statFillRate.setText(String.format("%.0f%%", avgFill * 100));

         
        if (statBookingsChange != null)
            statBookingsChange.setText("Активних: " + activeBookings);
        if (statToursChange != null)
            statToursChange.setText("Доступних місць: " +
                  tours.stream().mapToInt(Tour::getAvailableSeats).sum());
        if (statFillChange != null)
            statFillChange.setText(avgFill >= 0.8 ? "↑ Високе заповнення" :
                  avgFill >= 0.5 ? "→ Середнє заповнення" : "↓ Низьке заповнення");
        if (statRevenueChange != null)
            statRevenueChange.setText("Разом: " + bookings.stream()
                  .filter(b -> !Set.of("CANCELLED").contains(b.getStatus()))
                  .count() + " бронювань");
    }

     

    private void buildTourCards(List<Tour> tours) {
        if (toursGrid == null) return;
        toursGrid.getChildren().clear();

        List<Tour> display = tours.stream().limit(6).collect(Collectors.toList());
        String[] emojis = {"🏖️","🗼","🌸","🏔️","🏜️","🌅"};

        for (int i = 0; i < display.size(); i++) {
            Tour t = display.get(i);
            VBox card = buildCard(t, i < emojis.length ? emojis[i] : "✈️", GRADIENTS[i % GRADIENTS.length]);
            card.setOpacity(0); card.setTranslateY(10);
            int delay = 80 + i * 60;
            FadeTransition ft = new FadeTransition(Duration.millis(380), card);
            ft.setDelay(Duration.millis(delay)); ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(380), card);
            tt.setDelay(Duration.millis(delay)); tt.setFromY(10); tt.setToY(0);
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
        StackPane.setMargin(dest, new Insets(0,0,8,10));

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
        StackPane.setMargin(badge, new Insets(8,8,0,0));
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

     

    private void buildRevenueChart(List<Booking> bookings) {
        if (chartBars == null) return;
        chartBars.getChildren().clear();

         
        Map<Integer, BigDecimal> byMonth = new HashMap<>();
        for (int i = 1; i <= 12; i++) byMonth.put(i, BigDecimal.ZERO);
        bookings.stream()
              .filter(b -> !"CANCELLED".equals(b.getStatus()))
              .filter(b -> b.getBookingDate() != null && b.getTotalPrice() != null)
              .forEach(b -> byMonth.merge(
                    b.getBookingDate().getMonthValue(), b.getTotalPrice(), BigDecimal::add));

        BigDecimal max = byMonth.values().stream()
              .max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.ONE;
        final BigDecimal maxVal = max;

         
        int chartW = 520, chartH = 130;
        Canvas canvas = new Canvas(chartW, chartH);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        int barCount = 12;
        double barW   = (double)(chartW - 40) / barCount - 6;
        double startX = 28;
        double chartAreaH = chartH - 28; // залишаємо місце для підписів

         
        gc.setStroke(Color.web("#2a3a2a", 0.4));
        gc.setLineWidth(0.5);
        for (int row = 1; row <= 4; row++) {
            double y = chartAreaH - (chartAreaH * row / 4.0);
            gc.strokeLine(startX, y, chartW - 10, y);
        }

         
        double[] targetHeights = new double[12];
        double[] xPositions    = new double[12];
        boolean[] isMax        = new boolean[12];

        for (int i = 1; i <= 12; i++) {
            double ratio = byMonth.get(i)
                  .divide(maxVal, 6, RoundingMode.HALF_UP).doubleValue();
            targetHeights[i-1] = Math.max(3, ratio * (chartAreaH - 8));
            xPositions[i-1]    = startX + (i - 1) * ((double)(chartW - 38) / 12);
            isMax[i-1]         = byMonth.get(i).compareTo(maxVal) == 0;
        }

         
        double[] currentH = new double[12];
        Timeline tl = new Timeline();
        tl.setCycleCount(20);
        tl.setRate(1);

        KeyFrame kf = new KeyFrame(Duration.millis(25), e -> {
            gc.clearRect(0, 0, chartW, chartH);

             
            gc.setStroke(Color.web("#2a3a2a", 0.35));
            gc.setLineWidth(0.5);
            for (int row = 1; row <= 4; row++) {
                double y = chartAreaH - (chartAreaH * row / 4.0);
                gc.strokeLine(startX, y, chartW - 10, y);
            }

            for (int i = 0; i < 12; i++) {
                currentH[i] = Math.min(currentH[i] + targetHeights[i] / 18.0, targetHeights[i]);
                double x = xPositions[i];
                double h = currentH[i];
                double y = chartAreaH - h;

                 
                if (isMax[i]) {
                    gc.setFill(Color.web("#639922"));
                } else {
                    gc.setFill(Color.web("#c0dd97", 0.75));
                }
                 
                double r = Math.min(4, barW / 2);
                gc.fillRoundRect(x, y, barW, h + r, r * 2, r * 2);
                gc.fillRect(x, y + r, barW, h - r + 1); // прямокутна нижня частина

                 
                gc.setFill(Color.web("#8a9a85"));
                gc.setFont(Font.font("Arial", 9));
                gc.fillText(MONTHS[i], x + barW/2 - 7, chartH - 4);

                 
                if (isMax[i] && h > 20) {
                    gc.setFill(Color.web("#c0dd97"));
                    gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
                    String label = formatShort(byMonth.get(i + 1));
                    gc.fillText(label, x, y - 4);
                }
            }
        });
        tl.getKeyFrames().add(kf);
        tl.play();

        chartBars.getChildren().add(canvas);
        HBox.setHgrow(canvas, Priority.ALWAYS);
    }

     

    private void buildPieChart(Map<String, Long> statusStats) {
        if (pieChartContainer == null) return;
        pieChartContainer.getChildren().clear();

        if (statusStats.isEmpty()) return;

        long total = statusStats.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return;

        int size = 130;
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = size / 2.0, cy = size / 2.0, r = size / 2.0 - 4;

         
        List<Map.Entry<String, Long>> entries = statusStats.entrySet().stream()
              .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
              .collect(Collectors.toList());

         
        double[] targetAngles = new double[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            targetAngles[i] = (double) entries.get(i).getValue() / total * 360.0;
        }

        double[] currentAngles = new double[entries.size()];
        double[] startAngles   = new double[entries.size()];

         
        double cumulative = -90;
        for (int i = 0; i < entries.size(); i++) {
            startAngles[i] = cumulative;
            cumulative += targetAngles[i];
        }

        Timeline tl = new Timeline();
        tl.setCycleCount(20);
        double[] progress = {0};

        KeyFrame kf = new KeyFrame(Duration.millis(25), e -> {
            progress[0] = Math.min(progress[0] + 0.055, 1.0);
            gc.clearRect(0, 0, size, size);

            double angle = -90;
            for (int i = 0; i < entries.size(); i++) {
                currentAngles[i] = targetAngles[i] * progress[0];
                Color c = STATUS_COLORS.getOrDefault(entries.get(i).getKey(), Color.web("#888"));
                gc.setFill(c);
                gc.fillArc(4, 4, (size-8), (size-8), -angle, -currentAngles[i],
                      javafx.scene.shape.ArcType.ROUND);
                angle += currentAngles[i];
            }

             
            gc.setFill(Color.web("#12211a"));
            gc.fillOval(cx - r*0.52, cy - r*0.52, r*1.04, r*1.04);

             
            if (progress[0] > 0.8) {
                gc.setFill(Color.web("#c0dd97"));
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
                gc.fillText(String.valueOf(total), cx - 12, cy + 6);
                gc.setFill(Color.web("#8a9a85"));
                gc.setFont(Font.font("Arial", 9));
                gc.fillText("бронювань", cx - 28, cy + 18);
            }
        });
        tl.getKeyFrames().add(kf);
        tl.play();

         
        VBox legend = new VBox(4);
        String[] statusLabels = {"CREATED","CONFIRMED","PAID","COMPLETED","CANCELLED"};
        String[] statusNames  = {"Нові","Підтверджені","Оплачені","Завершені","Скасовані"};

        for (int i = 0; i < statusLabels.length; i++) {
            Long count = statusStats.get(statusLabels[i]);
            if (count == null || count == 0) continue;
            Color c = STATUS_COLORS.get(statusLabels[i]);

            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);
            Rectangle dot = new Rectangle(10, 10);
            dot.setArcWidth(3); dot.setArcHeight(3);
            dot.setFill(c);
            Label lbl = new Label(statusNames[i] + ": " + count);
            lbl.setStyle("-fx-font-size:10px;-fx-text-fill:#c0ccc0;");
            row.getChildren().addAll(dot, lbl);
            legend.getChildren().add(row);
        }

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(canvas, legend);
        pieChartContainer.getChildren().add(row);
    }

     

    private void buildCountriesChart(LinkedHashMap<String, Integer> topCountries) {
        if (countriesContainer == null) return;
        countriesContainer.getChildren().clear();

        if (topCountries.isEmpty()) {
            Label empty = new Label("Немає даних");
            empty.setStyle("-fx-text-fill:#8a9a85;-fx-font-size:11px;");
            countriesContainer.getChildren().add(empty);
            return;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(topCountries.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            String country = entries.get(i).getKey();
            int    pct     = entries.get(i).getValue();

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);

             
            Label nameLbl = new Label(country);
            nameLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#c0ccc0;");
            nameLbl.setMinWidth(90);
            nameLbl.setPrefWidth(90);

             
            StackPane barContainer = new StackPane();
            HBox.setHgrow(barContainer, Priority.ALWAYS);
            barContainer.setMaxWidth(Double.MAX_VALUE);
            barContainer.setStyle("-fx-background-color:#1a2a1a;-fx-background-radius:4;");
            barContainer.setPrefHeight(16);

            Rectangle bar = new Rectangle(0, 14);
            bar.setArcWidth(4); bar.setArcHeight(4);
            String barColor = i == 0 ? "#639922" : i == 1 ? "#4a7a1a" : "#2a5a0a";
            bar.setFill(Color.web(barColor));
            StackPane.setAlignment(bar, Pos.CENTER_LEFT);

            barContainer.getChildren().add(bar);
            final int delayMs = 500 + i * 80;
            barContainer.widthProperty().addListener((obs, oldW, newW) -> {
                double targetW = newW.doubleValue() * pct / 100.0;
                Timeline tl = new Timeline(new KeyFrame(
                      Duration.millis(delayMs),
                      new KeyValue(bar.widthProperty(), targetW, Interpolator.EASE_OUT)));
                tl.play();
            });

             
            Label pctLbl = new Label(pct + "%");
            pctLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#639922;-fx-font-weight:bold;");
            pctLbl.setMinWidth(28);

            row.getChildren().addAll(nameLbl, barContainer, pctLbl);
            countriesContainer.getChildren().add(row);
        }
    }

     

    private void buildRecentBookings(List<Booking> bookings, List<Tour> tours) {
        if (recentBookingsContainer == null) return;
        recentBookingsContainer.getChildren().clear();

        Map<java.util.UUID, Tour> tourMap = tours.stream()
              .collect(Collectors.toMap(Tour::getId, t -> t, (a, b) -> a));

         
        Map<java.util.UUID, com.touroperator.domain.Client> clientMap = new HashMap<>();
        try {
            com.touroperator.service.ClientService cs =
                  SpringContext.getBean(com.touroperator.service.ClientService.class);
            cs.findAll().forEach(c -> clientMap.put(c.getId(), c));
        } catch (Exception ignored) {}

        List<Booking> recent = bookings.stream()
              .filter(b -> b.getBookingDate() != null)
              .sorted(Comparator.comparing(Booking::getBookingDate).reversed())
              .limit(5)
              .collect(Collectors.toList());

        if (recent.isEmpty()) {
            Label empty = new Label("Немає бронювань");
            empty.setStyle("-fx-text-fill:#8a9a85;-fx-font-size:11px;");
            recentBookingsContainer.getChildren().add(empty);
            return;
        }

        for (Booking b : recent) {
            Tour tour = tourMap.get(b.getTourId());
            String tourName = tour != null ? tour.getCountry() + " · " +
                  b.getBookingDate().format(java.time.format.DateTimeFormatter.ofPattern("d MMM",
                        new java.util.Locale("uk"))) : "—";

             
            com.touroperator.domain.Client client = clientMap.get(b.getClientId());
            String initials = "?";
            if (client != null && client.getName() != null && !client.getName().isBlank()) {
                String[] parts = client.getName().trim().split("\\s+");
                initials = parts.length >= 2
                      ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                      : client.getName().substring(0, Math.min(2, client.getName().length())).toUpperCase();
            }

            String[] avaClasses = {"ca-a","ca-b","ca-c","ca-d"};
            String avaClass = avaClasses[Math.abs(b.getId().hashCode()) % avaClasses.length];

            HBox row = new HBox(10);
            row.getStyleClass().add("table-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label ava = new Label(initials);
            ava.getStyleClass().addAll("c-ava", avaClass);

            VBox info = new VBox(1);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label id = new Label("#" + b.getId().toString().substring(0, 8));
            id.getStyleClass().add("td-normal");
            Label dest = new Label(tourName);
            dest.getStyleClass().add("td-muted");
            info.getChildren().addAll(id, dest);

            Label statusLbl = new Label(localizeStatus(b.getStatus()));
            String pillClass = switch (b.getStatus()) {
                case "CONFIRMED"         -> "pill-confirmed";
                case "PAID"              -> "pill-paid";
                case "CANCELLED"         -> "pill-cancelled";
                case "COMPLETED"         -> "pill-paid";
                default                  -> "pill-pending";
            };
            statusLbl.getStyleClass().add(pillClass);

            row.getChildren().addAll(ava, info, statusLbl);
            recentBookingsContainer.getChildren().add(row);
        }
    }

     

    private void showFallback() {
        if (statRevenue  != null) statRevenue.setText("—");
        if (statBookings != null) statBookings.setText("—");
        if (statTours    != null) statTours.setText("—");
        if (statFillRate != null) statFillRate.setText("—");
    }

    private String formatShort(BigDecimal val) {
        if (val == null) return "0";
        long v = val.longValue();
        if (v >= 1_000_000) return (v / 1_000_000) + "M";
        if (v >= 1_000)     return (v / 1_000) + "k";
        return String.valueOf(v);
    }

    private String localizeStatus(String status) {
        return switch (status) {
            case "CREATED"   -> "Нове";
            case "CONFIRMED" -> "Підтверджено";
            case "PAID"      -> "Сплачено";
            case "COMPLETED" -> "Завершено";
            case "CANCELLED" -> "Скасовано";
            default          -> status;
        };
    }

     

    private record DashboardData(
          List<Tour>                  tours,
          List<Booking>               bookings,
          Map<String, Long>           statusStats,
          LinkedHashMap<String, Integer> topCountries
    ) {}
}