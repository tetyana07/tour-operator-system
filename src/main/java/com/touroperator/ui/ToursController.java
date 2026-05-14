package com.touroperator.ui;

import com.touroperator.ui.VoyaAlert;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Tour;
import com.touroperator.domain.TourStatus;
import com.touroperator.repository.ClientRepository;
import com.touroperator.repository.HotelRepository;
import com.touroperator.service.RecommendationService;
import com.touroperator.service.TourService;
import com.touroperator.service.WeatherService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ToursController implements RoleAware {

    @FXML private GridPane toursGrid;
    @FXML private Label    tourCountLabel;
    @FXML private Button   tabActive;
    @FXML private Button   tabArchived;
    @FXML private Button   tabCancelled;
    @FXML private Button   newTourBtn;

    private List<Tour> tours;
    private TourStatus currentFilter = TourStatus.ACTIVE;
    private UserRole currentRole  = UserRole.CLIENT;
    private String   currentEmail = "";

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
        loadAndRenderTours();
        // Підписуємось на зміну валюти — перемальовуємо картки одразу
        ProfilePanelController.CurrencySession.addListener(
              () -> javafx.application.Platform.runLater(this::renderCards)
        );
    }

    /** Викликається MainController одразу після завантаження сторінки */
    @Override
    public void setRole(UserRole role, String email) {
        this.currentRole  = role;
        this.currentEmail = email;
        boolean isAdmin = role == UserRole.ADMIN;
        // Архів і Скасовані — тільки для адміністратора
        if (tabArchived != null) {
            tabArchived.setVisible(isAdmin);
            tabArchived.setManaged(isAdmin);
        }
        if (tabCancelled != null) {
            tabCancelled.setVisible(isAdmin);
            tabCancelled.setManaged(isAdmin);
        }
        // Кнопка "+ Новий тур" — тільки для адміністратора
        if (newTourBtn != null) {
            newTourBtn.setVisible(isAdmin);
            newTourBtn.setManaged(isAdmin);
        }
    }

    // ── Завантажити тури з БД і перемалювати сітку ──────────────────────────
    private void loadAndRenderTours() {
        try {
            TourService svc = SpringContext.getBean(TourService.class);
            tours = svc.findByStatus(currentFilter);
            tours.sort(java.util.Comparator.comparing(t -> t.getId().toString()));

            // Для клієнта на вкладці ACTIVE — показуємо рекомендовані тури першими
            if (currentRole == UserRole.CLIENT && currentFilter == TourStatus.ACTIVE
                  && currentEmail != null && !currentEmail.isBlank()) {
                try {
                    ClientRepository clientRepo = SpringContext.getBean(ClientRepository.class);
                    clientRepo.findByEmail(currentEmail).ifPresent(client -> {
                        RecommendationService recSvc = SpringContext.getBean(RecommendationService.class);
                        List<Tour> recommended = recSvc.getRecommendations(client.getId(), 3);
                        if (!recommended.isEmpty()) {
                            // Рекомендовані — на початок, решта після
                            List<Tour> reordered = new java.util.ArrayList<>(recommended);
                            tours.stream()
                                  .filter(t -> recommended.stream().noneMatch(r -> r.getId().equals(t.getId())))
                                  .forEach(reordered::add);
                            tours = reordered;
                        }
                    });
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
            tours = java.util.Collections.emptyList();
        }
        updateTabStyles();
        renderCards();
    }

    private void updateTabStyles() {
        if (tabActive == null) return;
        tabActive.getStyleClass().setAll(currentFilter == TourStatus.ACTIVE ? "tab-btn-active" : "tab-btn-inactive");
        tabArchived.getStyleClass().setAll(currentFilter == TourStatus.ARCHIVED ? "tab-btn-active" : "tab-btn-inactive");
        tabCancelled.getStyleClass().setAll(currentFilter == TourStatus.CANCELLED ? "tab-btn-active" : "tab-btn-inactive");
    }

    @FXML private void onShowActive()    { currentFilter = TourStatus.ACTIVE;    loadAndRenderTours(); }
    @FXML private void onShowArchived()  { currentFilter = TourStatus.ARCHIVED;  loadAndRenderTours(); }
    @FXML private void onShowCancelled() { currentFilter = TourStatus.CANCELLED; loadAndRenderTours(); }

    // ── Побудувати картки динамічно ──────────────────────────────────────────
    private void renderCards() {
        toursGrid.getChildren().clear();
        toursGrid.getRowConstraints().clear();

        if (tours.isEmpty()) {
            Label empty = new Label("Немає активних турів. Натисніть «+ Новий тур» щоб додати.");
            empty.setStyle("-fx-text-fill:#8a8fa8;-fx-font-size:13px;");
            toursGrid.add(empty, 0, 0);
            tourCountLabel.setText("0 " + filterLabel() + " напрямків");
            return;
        }

        tourCountLabel.setText(tours.size() + " активних напрямків");

        int totalRows = (tours.size() + 2) / 3;
        for (int r = 0; r < totalRows; r++) {
            javafx.scene.layout.RowConstraints rc = new javafx.scene.layout.RowConstraints();
            rc.setVgrow(Priority.NEVER);
            rc.setFillHeight(false);
            toursGrid.getRowConstraints().add(rc);
        }

        for (int i = 0; i < tours.size(); i++) {
            Tour tour = tours.get(i);
            int col = i % 3;
            int row = i / 3;
            VBox card = buildCard(tour, i);
            toursGrid.add(card, col, row);
        }
    }

    // ── Побудувати одну картку з реальними даними туру ──────────────────────
    private VBox buildCard(Tour tour, int index) {
        String gradient = GRADIENTS[index % GRADIENTS.length];

        // ── Верхня частина (фото або кольоровий банер) ──────────────────────
        StackPane imgPane = new StackPane();
        imgPane.getStyleClass().add("tour-img-pane");
        imgPane.setPrefHeight(140);
        imgPane.setMinHeight(140);
        imgPane.setMaxHeight(140);

        // Заокруглений кліп — щоб фото обрізалось по кутах картки
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(34);
        clip.setArcHeight(34);
        clip.widthProperty().bind(imgPane.widthProperty());
        clip.heightProperty().bind(imgPane.heightProperty());
        imgPane.setClip(clip);

        boolean hasPhoto = tour.getImagePath() != null && !tour.getImagePath().isBlank();
        if (hasPhoto) {
            try {
                java.io.File imgFile = new java.io.File(tour.getImagePath());
                if (imgFile.exists()) {
                    String uriStr = imgFile.toURI().toURL().toExternalForm();
                    Image img = new Image(uriStr, true);
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(false);
                    iv.setSmooth(true);
                    iv.fitWidthProperty().bind(imgPane.widthProperty());
                    iv.fitHeightProperty().bind(imgPane.heightProperty());
                    imgPane.getChildren().add(iv);
                } else {
                    imgPane.setStyle("-fx-background-color:" + gradient + ";");
                }
            } catch (Exception e) {
                imgPane.setStyle("-fx-background-color:" + gradient + ";");
            }
        } else {
            imgPane.setStyle("-fx-background-color:" + gradient + ";");
        }

        // тіньовий оверлей
        Region shadow = new Region();
        shadow.getStyleClass().add("tour-img-shadow");
        shadow.setMaxWidth(Double.MAX_VALUE);
        shadow.setMaxHeight(Double.MAX_VALUE);

        // бейдж статусу
        Label badge = buildBadge(tour);
        StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(10, 10, 0, 0));

        // назва міста
        Label destLbl = new Label(tour.getCountry() + ", " + tour.getCity());
        destLbl.getStyleClass().add("tour-dest-lbl");
        StackPane.setAlignment(destLbl, javafx.geometry.Pos.BOTTOM_LEFT);
        StackPane.setMargin(destLbl, new Insets(0, 0, 13, 14));

        imgPane.getChildren().addAll(shadow, badge, destLbl);

        // ── Нижня частина (тіло картки) ─────────────────────────────────────
        VBox bodyPane = new VBox();
        bodyPane.getStyleClass().add("tour-body-pane");

        // рядок: назва + ціна
        HBox nameRow = new HBox(4);
        nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label nameLbl = new Label(tour.getName());
        nameLbl.getStyleClass().add("tour-name-lbl");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        Label priceLbl = new Label(ProfilePanelController.CurrencySession.format(tour.getBasePrice()));
        priceLbl.getStyleClass().add("tour-price-lbl");
        nameRow.getChildren().addAll(nameLbl, priceLbl);

        // рядок: дати
        HBox metaRow = new HBox(12);
        metaRow.setStyle("-fx-padding:2 0 4 0;");
        Label dateLbl = new Label("📅 " + formatDate(tour.getStartDate()) + " – " + formatDate(tour.getEndDate()));
        dateLbl.getStyleClass().add("tour-meta-lbl");
        metaRow.getChildren().add(dateLbl);

        // рядок: місця + прогрес-бар (реальні дані!)
        int booked = tour.getBookedSeats();
        int total  = tour.getQuota();
        double fill = total > 0 ? (double) booked / total : 0.0;

        VBox quotaBox = new VBox(4);
        HBox quotaRow = new HBox();
        Label quotaTitleLbl = new Label("Місця");
        quotaTitleLbl.getStyleClass().add("quota-lbl");
        HBox.setHgrow(quotaTitleLbl, Priority.ALWAYS);
        Label quotaNumLbl = new Label(booked + "/" + total);
        quotaNumLbl.getStyleClass().add("quota-lbl");
        quotaRow.getChildren().addAll(quotaTitleLbl, quotaNumLbl);

        ProgressBar bar = new ProgressBar(fill);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(4);
        // колір бара: >85% — жовтогарячий, інакше — зелений
        if (fill >= 0.85) {
            bar.setStyle("-fx-accent:#c07020;");
        } else {
            bar.getStyleClass().add("quota-bar");
        }
        quotaBox.getChildren().addAll(quotaRow, bar);

        bodyPane.getChildren().addAll(nameRow, metaRow, quotaBox);

        // ── Збираємо картку ─────────────────────────────────────────────────
        VBox card = new VBox();
        card.getStyleClass().add("tour-card");
        card.getChildren().addAll(imgPane, bodyPane);
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setVgrow(card, Priority.NEVER);
        GridPane.setFillHeight(card, false);

        // клік → відкрити деталі
        final Tour finalTour = tour;
        final int  finalIdx  = index;
        card.setOnMouseClicked(e -> openDetail(finalTour, finalIdx));

        return card;
    }

    // ── Бейдж статусу ────────────────────────────────────────────────────────
    private Label buildBadge(Tour tour) {
        double fill = tour.getQuota() > 0 ? (double) tour.getBookedSeats() / tour.getQuota() : 0;
        Label badge = new Label();
        if (fill >= 1.0) {
            badge.setText("Повний");
            badge.getStyleClass().add("tour-badge-full");
        } else if (fill >= 0.85) {
            badge.setText("Майже повний");
            badge.getStyleClass().add("tour-badge-full");
        } else if (tour.getStartDate() != null &&
              tour.getStartDate().isAfter(java.time.LocalDate.now().plusDays(30))) {
            badge.setText("Незабаром");
            badge.getStyleClass().add("tour-badge-soon");
        } else {
            badge.setText("Відкрито");
            badge.getStyleClass().add("tour-badge-open");
        }
        return badge;
    }

    // ── Відкрити детальне вікно туру ─────────────────────────────────────────
    private void openDetail(Tour tour, int index) {
        String weatherTemp = "+0°C";
        String weatherDesc = "Немає даних (офлайн)";
        String weatherIcon = "fas-cloud";
        String humidity    = "—";
        String wind        = "—";
        String feelsLike   = "—";

        try {
            WeatherService ws = SpringContext.getBean(WeatherService.class);
            var weather = ws.getWeather(tour.getCity());
            weatherTemp = weather.getFormattedTemp();
            weatherDesc = weather.getDescription();
            weatherIcon = descToIconLiteral(weatherDesc, weather.getTemperature());
            humidity    = weather.getFormattedHumidity();
            wind        = weather.getFormattedWind();
            feelsLike   = weather.getFormattedFeelsLike();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Реальні значення bookedSeats і quota з об'єкта туру
        com.touroperator.domain.TourStatus tourStatusVal =
              tour.getStatus() != null ? tour.getStatus() : com.touroperator.domain.TourStatus.ACTIVE;

        // Реальна назва готелю з БД
        String hotelName = "Без готелю";
        if (tour.getHotelId() != null) {
            try {
                HotelRepository hotelRepo = SpringContext.getBean(HotelRepository.class);
                hotelName = hotelRepo.findById(tour.getHotelId())
                      .map(h -> h.getName() + " " + "★".repeat(Math.max(0, h.getStars())))
                      .orElse("Готель не знайдено");
            } catch (Exception ignored) {}
        }

        TourDetailController.TourData data = new TourDetailController.TourData(
              tour.getId(), tourStatusVal,
              tour.getName(),
              tour.getCountry() + ", " + tour.getCity(),
              ProfilePanelController.CurrencySession.format(tour.getBasePrice()),
              tour.getBasePrice(),
              GRADIENTS[index % GRADIENTS.length],
              formatDate(tour.getStartDate()) + " – " + formatDate(tour.getEndDate()),
              daysBetween(tour) + " днів",
              tour.getDeparture() != null && !tour.getDeparture().isBlank()
                    ? tour.getDeparture() : "Не вказано",
              hotelName, tour.getMealType(),
              weatherIcon, weatherTemp, weatherDesc,
              humidity, wind, feelsLike,
              tour.getBookedSeats(), tour.getQuota(),
              tour.getImagePath(),
              tour.isHasFlight(), tour.isHasInsurance(), tour.isHasTransfer(), tour.isHasGuide()
        );

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/TourDetailDialog.fxml"));
            StackPane overlay = loader.load();
            TourDetailController ctrl = loader.getController();
            ctrl.setRole(currentRole);
            ctrl.setTourData(data);

            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            dialog.initOwner(toursGrid.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(overlay, 700, 650);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // Blur на головне вікно при відкритті
            javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(12);
            javafx.scene.Node mainRoot = toursGrid.getScene().getRoot();
            mainRoot.setEffect(blur);

            Runnable removeBlur = () -> mainRoot.setEffect(null);

            ctrl.setOnClose(() -> { removeBlur.run(); dialog.close(); });
            ctrl.setOnBook(() -> { removeBlur.run(); dialog.close(); openNewBookingFor(tour); });
            ctrl.setOnStatusChanged(() -> loadAndRenderTours());

            dialog.setOnHidden(e -> removeBlur.run());
            dialog.setScene(scene);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Відкрити форму нового туру ───────────────────────────────────────────
    @FXML
    private void onNewTour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/NewTourDialog.fxml"));
            StackPane overlay = loader.load();
            NewTourController ctrl = loader.getController();

            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            dialog.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(overlay, 640, 580);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            ctrl.setOnCancel(dialog::close);
            ctrl.setOnSaved(() -> {
                dialog.close();
                // Перезавантажуємо і перемальовуємо картки з БД
                loadAndRenderTours();
            });

            dialog.setScene(scene);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Не вдалося відкрити форму нового туру: " + e.getMessage());
        }
    }

    private void openNewBookingFor(Tour tour) {
        // Для клієнта — перевіряємо дублікат до відкриття діалогу
        if (currentRole == UserRole.CLIENT && currentEmail != null && !currentEmail.isBlank()) {
            try {
                com.touroperator.repository.ClientRepository clientRepo =
                      SpringContext.getBean(com.touroperator.repository.ClientRepository.class);
                com.touroperator.repository.BookingRepository bookingRepo =
                      SpringContext.getBean(com.touroperator.repository.BookingRepository.class);
                java.util.Optional<com.touroperator.domain.Client> clientOpt =
                      clientRepo.findByEmail(currentEmail);
                if (clientOpt.isPresent() &&
                      bookingRepo.existsActiveByClientAndTour(clientOpt.get().getId(), tour.getId())) {
                    VoyaAlert.error("Ви вже маєте активне бронювання на цей тур.\n" +
                          "Скасуйте попереднє бронювання перш ніж створювати нове.");
                    return;
                }
            } catch (Exception ignored) {}
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/NewBookingDialog.fxml"));
            StackPane overlay = loader.load();
            NewBookingController ctrl = loader.getController();
            ctrl.preselectTour(tour);
            ctrl.setClientContext(currentRole, currentEmail);

            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            dialog.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(overlay, 620, 700);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            ctrl.setOnClose(dialog::close);
            // Після бронювання — оновлюємо картки щоб лінія змінилась
            ctrl.setOnSaved(() -> loadAndRenderTours());
            dialog.setScene(scene);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Не вдалося відкрити форму бронювання: " + e.getMessage());
        }
    }

    // ── Допоміжні методи ────────────────────────────────────────────────────
    private String descToIconLiteral(String desc, BigDecimal temp) {
        if (desc == null) return "fas-cloud";
        String d = desc.toLowerCase();
        if (d.contains("гроза") || d.contains("thunder") || d.contains("storm")) return "fas-bolt";
        if (d.contains("сніг")  || d.contains("snow")    || d.contains("blizzard")) return "fas-snowflake";
        if (d.contains("дощ")   || d.contains("rain")    || d.contains("drizzle"))  return "fas-cloud-rain";
        if (d.contains("туман") || d.contains("fog")     || d.contains("mist"))     return "fas-smog";
        if (d.contains("похмуро") || d.contains("overcast") || d.contains("хмарно")) return "fas-cloud";
        if (d.contains("мінливо") || d.contains("partly"))   return "fas-cloud-sun";
        if (d.contains("сонячно") || d.contains("sunny")  || d.contains("ясно") || d.contains("clear")) return "fas-sun";
        if (temp != null) {
            int t = temp.intValue();
            if (t <= 0)  return "fas-snowflake";
            if (t <= 10) return "fas-cloud";
            if (t <= 20) return "fas-cloud-sun";
            return "fas-sun";
        }
        return "fas-cloud-sun";
    }

    private static final DateTimeFormatter DATE_FMT =
          DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private String formatDate(java.time.LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "—";
    }

    private String daysBetween(Tour t) {
        if (t.getStartDate() == null || t.getEndDate() == null) return "?";
        return String.valueOf(ChronoUnit.DAYS.between(t.getStartDate(), t.getEndDate()));
    }

    private String filterLabel() {
        return switch (currentFilter) {
            case ACTIVE -> "активних";
            case ARCHIVED -> "архівних";
            case CANCELLED -> "скасованих";
            default -> "";
        };
    }

    private void showError(String msg) { VoyaAlert.error(msg); }
}