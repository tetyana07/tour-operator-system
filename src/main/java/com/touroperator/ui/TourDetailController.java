package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.TourStatus;
import com.touroperator.service.TourService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.UUID;

public class TourDetailController {

    @FXML private StackPane overlay;
    @FXML private StackPane heroPane;
    @FXML private Label heroCountry, heroName, heroPrice;
    @FXML private Label chipDates, chipDuration, chipDepart, chipHotel, chipFood, chipInsurance;
    @FXML private VBox  chipDatesBox, chipDurationBox, chipDepartBox, chipHotelBox, chipFoodBox, chipInsuranceBox;
    @FXML private javafx.scene.layout.FlowPane includedFlow;
    @FXML private Label weatherIcon, weatherCity, weatherTemp, weatherDesc;
    @FXML private Label weatherHumidity, weatherWind, weatherFeels;
    @FXML private VBox  weatherCard;
    @FXML private javafx.scene.layout.StackPane weatherCardWrapper;

    // Створюємо програмно — не через FXML ін'єкцію
    private javafx.scene.image.ImageView weatherBgImage;
    @FXML private Label quotaLabel;
    @FXML private ProgressBar quotaBar;
    @FXML private Button cancelTourBtn;
    @FXML private Button archiveTourBtn;
    @FXML private Button bookBtn;
    @FXML private javafx.scene.control.Label closeBtn;

    private Runnable onCloseCallback;
    private Runnable onBookCallback;
    private Runnable onStatusChangedCallback;
    private UUID tourId;
    private TourStatus tourStatus;
    private UserRole currentRole = UserRole.CLIENT;

    public void setRole(UserRole role) {
        this.currentRole = role;
    }

    @FXML
    public void initialize() {
        // FontIcon хрестик
        FontIcon xIcon = new FontIcon("fas-times");
        xIcon.setIconSize(13);
        xIcon.setStyle("-fx-icon-color: white;");
        // closeBtn is now a Label — text "✕" set in FXML

        // Заокруглення героя — clip щоб зображення не виходило за межі
        javafx.scene.shape.Rectangle heroClip = new javafx.scene.shape.Rectangle();
        heroClip.setArcWidth(56); heroClip.setArcHeight(56);
        heroPane.layoutBoundsProperty().addListener((o, ov, nv) -> {
            heroClip.setWidth(nv.getWidth());
            heroClip.setHeight(nv.getHeight() + 28); // +28 щоб знизу не обрізало
        });
        heroPane.setClip(heroClip);

        // Створюємо ImageView для фону погоди програмно
        weatherBgImage = new javafx.scene.image.ImageView();
        weatherBgImage.setPreserveRatio(false);
        weatherBgImage.setSmooth(true);
        weatherBgImage.setVisible(false);
        // Прив'язуємо розмір до weatherCard
        weatherCard.layoutBoundsProperty().addListener((o, ov, nv) -> {
            weatherBgImage.setFitWidth(nv.getWidth());
            weatherBgImage.setFitHeight(nv.getHeight());
        });
        if (weatherCardWrapper != null) {
            weatherCardWrapper.getChildren().add(0, weatherBgImage);
            weatherCardWrapper.setMaxWidth(Double.MAX_VALUE);
        } else {
            javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane();
            wrapper.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.Node parent = weatherCard.getParent();
            if (parent instanceof javafx.scene.layout.VBox vb) {
                int idx = vb.getChildren().indexOf(weatherCard);
                if (idx >= 0) {
                    vb.getChildren().remove(weatherCard);
                    wrapper.getChildren().addAll(weatherBgImage, weatherCard);
                    javafx.scene.layout.VBox.setVgrow(wrapper, javafx.scene.layout.Priority.NEVER);
                    vb.getChildren().add(idx, wrapper);
                    weatherCardWrapper = wrapper;
                }
            }
        }
    }

    /** Заповнює модальне вікно даними туру */
    public void setTourData(TourData data) {
        this.tourId     = data.tourId();
        this.tourStatus = data.status();

        // Підтягуємо актуальну квоту з БД (дані в data могли бути взяті до бронювань)
        int[] quota = { data.booked(), data.total() };
        if (data.tourId() != null) {
            try {
                com.touroperator.repository.TourRepository tourRepo =
                      com.touroperator.config.SpringContext.getBean(
                            com.touroperator.repository.TourRepository.class);
                tourRepo.findById(data.tourId()).ifPresent(t -> {
                    quota[0] = t.getBookedSeats();
                    quota[1] = t.getQuota();
                });
            } catch (Exception ignored) {}
        }
        final int freshBooked = quota[0];
        final int freshTotal  = quota[1];

        heroName.setText(data.name());
        heroCountry.setText(data.country());
        // Ціна — завжди через CurrencySession щоб відображати актуальну валюту
        java.math.BigDecimal rawPrice = data.rawPrice() != null ? data.rawPrice() : java.math.BigDecimal.ZERO;
        heroPrice.setText(ProfilePanelController.CurrencySession.format(rawPrice));
        // Оновлюємо ціну при зміні валюти (діалог може бути відкритий)
        ProfilePanelController.CurrencySession.addListener(
              () -> javafx.application.Platform.runLater(
                    () -> heroPrice.setText(ProfilePanelController.CurrencySession.format(rawPrice))
              )
        );
        heroPane.setStyle("-fx-background-color:" + data.gradient() + ";");

        // Показуємо фото якщо є
        System.out.println("[PHOTO] imagePath = '" + data.imagePath() + "'");
        if (data.imagePath() != null && !data.imagePath().isBlank()) {
            try {
                java.io.File imgFile = new java.io.File(data.imagePath());
                System.out.println("[PHOTO] file exists = " + imgFile.exists() + ", path = " + imgFile.getAbsolutePath());
                if (imgFile.exists()) {
                    // toURI() правильно кодує пробіли та спецсимволи у шляху
                    String uriStr = imgFile.toURI().toURL().toExternalForm();
                    System.out.println("[PHOTO] URI = " + uriStr);
                    Image img = new Image(uriStr, true);
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(false);
                    iv.setSmooth(true);
                    iv.fitWidthProperty().bind(heroPane.widthProperty());
                    iv.fitHeightProperty().bind(heroPane.heightProperty());
                    heroPane.getChildren().add(0, iv);
                    System.out.println("[PHOTO] OK — додано в heroPane");
                } else {
                    System.out.println("[PHOTO] ФАЙЛ НЕ ЗНАЙДЕНО: " + imgFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.out.println("[PHOTO] Exception: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[PHOTO] imagePath null/blank — градієнт");
        }

        setChip(chipDatesBox,     chipDates,     data.dates());
        setChip(chipDurationBox,  chipDuration,  data.duration());
        setChip(chipDepartBox,    chipDepart,    data.departure());
        setChip(chipHotelBox,     chipHotel,     data.hotel());
        setChip(chipFoodBox,      chipFood,       data.food());
        setChip(chipInsuranceBox, chipInsurance, data.hasInsurance() ? "Включено" : null);

        // Динамічна секція "Що включено"
        populateIncluded(data);

        // Визначаємо тип погоди і налаштовуємо картку
        String desc = data.weatherDesc() != null ? data.weatherDesc().toLowerCase() : "";
        String weatherType = resolveWeatherType(desc, data.weatherTemp());
        applyWeatherTheme(weatherType);

        // Іконка погоди прихована — фон картки вже передає погоду
        weatherIcon.setVisible(false);
        weatherIcon.setManaged(false);

        // Без іконки — тільки текст
        weatherCity.setGraphic(null);
        weatherCity.setText(data.country());

        weatherTemp.setText(data.weatherTemp());
        weatherDesc.setText(data.weatherDesc());
        weatherHumidity.setText(data.humidity());
        weatherWind.setText(data.wind());
        weatherFeels.setText(data.feelsLike());

        quotaLabel.setText(freshBooked + " / " + freshTotal + " місць");
        double progress = freshTotal > 0 ? (double) freshBooked / freshTotal : 0;
        quotaBar.setProgress(progress);

        // Налаштовуємо кнопки залежно від статусу туру
        updateButtonStates();
    }

    /** Приховує/показує кнопки залежно від поточного статусу */
    private void updateButtonStates() {
        boolean isArchived  = tourStatus == TourStatus.ARCHIVED;
        boolean isActive    = tourStatus == TourStatus.ACTIVE || tourStatus == TourStatus.FULL;
        boolean isAdmin     = currentRole == UserRole.ADMIN;

        // "Скасувати" — тільки адмін, тільки для активних
        if (cancelTourBtn != null) {
            cancelTourBtn.setVisible(isAdmin && isActive);
            cancelTourBtn.setManaged(isAdmin && isActive);
        }
        // "Архівувати" — тільки адмін
        if (archiveTourBtn != null) {
            archiveTourBtn.setVisible(isAdmin && !isArchived);
            archiveTourBtn.setManaged(isAdmin && !isArchived);
        }
        // "Забронювати" — тільки для активних (не повних, не скасованих, не архів)
        if (bookBtn != null) {
            bookBtn.setVisible(tourStatus == TourStatus.ACTIVE);
            bookBtn.setManaged(tourStatus == TourStatus.ACTIVE);
        }
    }

    @FXML
    private void onCancelTour() {
        if (tourId == null) return;
        VoyaAlert.confirm(
              "Скасувати тур \"" + heroName.getText() + "\"?\n" +
                    "Цю дію не можна скасувати.",
              () -> {
                  try {
                      TourService svc = SpringContext.getBean(TourService.class);
                      svc.cancelTour(tourId);
                      tourStatus = TourStatus.CANCELLED;
                      updateButtonStates();
                      if (onStatusChangedCallback != null) onStatusChangedCallback.run();
                      VoyaAlert.info("Тур скасовано.");
                  } catch (Exception e) {
                      VoyaAlert.error("Помилка: " + e.getMessage());
                  }
              }
        );
    }

    @FXML
    private void onArchiveTour() {
        if (tourId == null) return;
        try {
            TourService svc = SpringContext.getBean(TourService.class);
            svc.archiveTour(tourId);
            tourStatus = TourStatus.ARCHIVED;
            updateButtonStates();
            if (onStatusChangedCallback != null) onStatusChangedCallback.run();
            VoyaAlert.info("Тур переміщено в архів.");
        } catch (Exception e) {
            VoyaAlert.error("Помилка: " + e.getMessage());
        }
    }

    private String resolveWeatherIconLiteral(String raw) {
        if (raw == null || raw.isBlank()) return "fas-cloud";
        if (raw.matches("[a-z]+-[a-z\\-]+")) return raw;
        return "fas-cloud-sun";
    }

    public void setOnClose(Runnable r)         { this.onCloseCallback         = r; }
    public void setOnBook(Runnable r)          { this.onBookCallback          = r; }
    public void setOnStatusChanged(Runnable r) { this.onStatusChangedCallback = r; }

    @FXML private void onClose() { if (onCloseCallback != null) onCloseCallback.run(); }
    @FXML private void onBook()  { if (onBookCallback  != null) onBookCallback.run(); onClose(); }

    /** Простий record з даними для відображення */
    public record TourData(
          UUID tourId, TourStatus status,
          String name, String country, String price, java.math.BigDecimal rawPrice, String gradient,
          String dates, String duration, String departure,
          String hotel, String food,
          String weatherIcon, String weatherTemp, String weatherDesc,
          String humidity, String wind, String feelsLike,
          int booked, int total,
          String imagePath,
          // Що включено
          boolean hasFlight, boolean hasInsurance, boolean hasTransfer, boolean hasGuide
    ) {}

    // ── Кольорова іконка погоди через Canvas ────────────────────────────────
    private javafx.scene.Node buildWeatherIcon(String type) {
        javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(64, 64);
        javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
        g.setLineWidth(2.5);

        switch (type) {
            case "sunny" -> {
                // Сонце: жовте коло + промені
                g.setFill(javafx.scene.paint.Color.web("#FFD700"));
                g.fillOval(17, 17, 30, 30);
                g.setStroke(javafx.scene.paint.Color.web("#FFB300"));
                g.setLineWidth(2.5);
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    double x1 = 32 + Math.cos(angle) * 19;
                    double y1 = 32 + Math.sin(angle) * 19;
                    double x2 = 32 + Math.cos(angle) * 26;
                    double y2 = 32 + Math.sin(angle) * 26;
                    g.strokeLine(x1, y1, x2, y2);
                }
            }
            case "partly" -> {
                // Сонце зверху-зліва
                g.setFill(javafx.scene.paint.Color.web("#FFD700"));
                g.fillOval(4, 4, 24, 24);
                g.setStroke(javafx.scene.paint.Color.web("#FFB300"));
                g.setLineWidth(2);
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    double x1 = 16 + Math.cos(angle) * 14;
                    double y1 = 16 + Math.sin(angle) * 14;
                    double x2 = 16 + Math.cos(angle) * 18;
                    double y2 = 16 + Math.sin(angle) * 18;
                    g.strokeLine(x1, y1, x2, y2);
                }
                // Хмара поверх
                drawCloud(g, 18, 32, javafx.scene.paint.Color.web("#e8eef5"), javafx.scene.paint.Color.web("#c8d8e8"));
            }
            case "cloudy" -> {
                drawCloud(g, 8, 22, javafx.scene.paint.Color.web("#c8d8e8"), javafx.scene.paint.Color.web("#a8b8c8"));
                drawCloud(g, 14, 36, javafx.scene.paint.Color.web("#e8eef5"), javafx.scene.paint.Color.web("#c0ccd8"));
            }
            case "rain" -> {
                drawCloud(g, 10, 18, javafx.scene.paint.Color.web("#8090a8"), javafx.scene.paint.Color.web("#607080"));
                // Краплі дощу
                g.setStroke(javafx.scene.paint.Color.web("#5090d0"));
                g.setLineWidth(2);
                g.strokeLine(18, 42, 15, 52);
                g.strokeLine(26, 42, 23, 52);
                g.strokeLine(34, 42, 31, 52);
                g.strokeLine(42, 42, 39, 52);
            }
            case "storm" -> {
                // Темна хмара з тінню
                drawCloud(g, 4, 8, javafx.scene.paint.Color.web("#384858"), javafx.scene.paint.Color.web("#20303e"));
                drawCloud(g, 12, 20, javafx.scene.paint.Color.web("#4a5c6e"), javafx.scene.paint.Color.web("#2e3e50"));
                // Блискавка — класична стрілка вниз
                g.setFill(javafx.scene.paint.Color.web("#FFE033"));
                // Верхня частина блискавки (широка)
                double[] tx = {28, 34, 31, 36};
                double[] ty = {38, 38, 48, 48};
                g.fillPolygon(tx, ty, 4);
                // Нижня частина (вузька)
                double[] bx2 = {31, 36, 32, 37};
                double[] by2 = {48, 48, 60, 60};
                g.fillPolygon(bx2, by2, 4);
                // Ефект свічення
                g.setFill(javafx.scene.paint.Color.web("#FFEE88", 0.3));
                g.fillOval(25, 36, 16, 26);
            }
            case "snow" -> {
                drawCloud(g, 10, 18, javafx.scene.paint.Color.web("#b0c8e0"), javafx.scene.paint.Color.web("#90a8c0"));
                // Сніжинка
                g.setStroke(javafx.scene.paint.Color.web("#a0d0ff"));
                g.setLineWidth(2);
                g.strokeLine(32, 42, 32, 58);
                g.strokeLine(24, 46, 40, 54);
                g.strokeLine(24, 54, 40, 46);
                // Кінці сніжинки
                for (double[] p : new double[][]{{32,42},{32,58},{24,46},{40,54},{24,54},{40,46}}) {
                    g.strokeLine(p[0]-3, p[1]-3, p[0]+3, p[1]+3);
                    g.strokeLine(p[0]+3, p[1]-3, p[0]-3, p[1]+3);
                }
            }
            case "fog" -> {
                // Хвилясті лінії туману
                g.setStroke(javafx.scene.paint.Color.web("#b0c0c8"));
                g.setLineWidth(3);
                for (int row = 0; row < 4; row++) {
                    double y = 20 + row * 10;
                    g.strokeLine(10, y, 54, y);
                }
            }
            default -> {
                // Хмара + сонце (default)
                g.setFill(javafx.scene.paint.Color.web("#FFD700"));
                g.fillOval(10, 8, 22, 22);
                drawCloud(g, 18, 30, javafx.scene.paint.Color.web("#e8eef5"), javafx.scene.paint.Color.web("#c8d8e8"));
            }
        }
        return c;
    }

    // ── Малює хмару у заданій позиції ───────────────────────────────────────
    private void drawCloud(javafx.scene.canvas.GraphicsContext g,
          double x, double y,
          javafx.scene.paint.Color fill,
          javafx.scene.paint.Color stroke) {
        g.setFill(fill);
        g.setStroke(stroke);
        g.setLineWidth(1.5);
        // три кола що утворюють хмару
        g.fillOval(x,      y + 8,  22, 18);
        g.fillOval(x + 14, y + 12, 18, 14);
        g.fillOval(x + 6,  y,      20, 20);
        g.strokeOval(x,      y + 8,  22, 18);
        g.strokeOval(x + 14, y + 12, 18, 14);
        g.strokeOval(x + 6,  y,      20, 20);
        // закриваємо низ
        g.fillRect(x + 1,  y + 14, 32, 13);
        g.setStroke(fill);
        g.setLineWidth(2);
        g.strokeLine(x + 1, y + 27, x + 33, y + 27);
    }

    // ── Визначає тип погоди за описом ───────────────────────────────────────
    private String resolveWeatherType(String desc, String tempStr) {
        if (desc.contains("гроза") || desc.contains("thunder") || desc.contains("storm")) return "storm";
        if (desc.contains("сніг")  || desc.contains("snow")    || desc.contains("blizzard")) return "snow";
        if (desc.contains("дощ")   || desc.contains("rain")    || desc.contains("drizzle"))  return "rain";
        if (desc.contains("туман") || desc.contains("fog")     || desc.contains("mist"))     return "fog";
        if (desc.contains("похмуро") || desc.contains("overcast") || desc.contains("хмарно")) return "cloudy";
        if (desc.contains("мінливо") || desc.contains("partly"))   return "partly";
        if (desc.contains("сонячно") || desc.contains("sunny")  || desc.contains("ясно") || desc.contains("clear")) return "sunny";
        // fallback по температурі
        try {
            int t = Integer.parseInt(tempStr.replaceAll("[^\\-0-9]", ""));
            if (t <= 0)  return "snow";
            if (t >= 28) return "sunny";
        } catch (Exception ignored) {}
        return "partly";
    }

    // ── Емодзі для типу погоди ───────────────────────────────────────────────

    // ── Динамічний фон картки залежно від погоди ─────────────────────────────
    private void applyWeatherTheme(String type) {
        String imageName = switch (type) {
            case "sunny"            -> "Sunny.jpg";
            case "cloudy", "partly" -> "Cloudy.jpg";
            case "rain"             -> "Rain.jpg";
            case "snow"             -> "Snow.jpg";
            case "storm"            -> "Thunderstorm.jpg";
            default                 -> null;
        };

        boolean photoLoaded = false;
        if (imageName != null) {
            try {
                java.net.URL url = getClass().getResource("/images/" + imageName);
                if (url != null) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(
                          url.toExternalForm(), true);
                    weatherBgImage.setImage(img);
                    weatherBgImage.setVisible(true);
                    // Clip щоб фото не виходило за rounded corners
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
                    clip.setArcWidth(36); clip.setArcHeight(36);
                    weatherCardWrapper.layoutBoundsProperty().addListener((o, ov, nv) -> {
                        clip.setWidth(nv.getWidth());
                        clip.setHeight(nv.getHeight());
                    });
                    weatherCardWrapper.setClip(clip);
                    // Ховаємо Canvas іконку і globe — на фото вже є погода
                    weatherIcon.setVisible(false);
                    weatherIcon.setManaged(false);
                    weatherCity.setGraphic(null);
                    photoLoaded = true;
                }
            } catch (Exception ignored) {}
        }

        if (!photoLoaded) {
            weatherBgImage.setVisible(false);
            weatherBgImage.setImage(null);
            weatherIcon.setVisible(false);
            weatherIcon.setManaged(false);
            // Fallback градієнт через CSS
            weatherCard.setStyle(
                  "-fx-background-color: " + fallbackGradient(type) + "; " +
                        "-fx-background-radius: 18; -fx-padding: 18 22 18 22;");
        } else {
            weatherCard.setStyle("-fx-background-color: transparent; -fx-padding: 18 22 18 22;");
        }

        // Колір тексту — темний для світлого неба, білий для темних тем
        // Темні фото (гроза, дощ, хмарно) — білий текст; світлі (сонце, сніг) — темний
        boolean lightBg = !photoLoaded
              || type.equals("sunny") || type.equals("snow");
        String tc = lightBg ? "#1a2a3a" : "white";
        String sc = lightBg ? "rgba(20,40,70,0.75)" : "rgba(255,255,255,0.90)";

        String kc = lightBg ? "rgba(30,50,80,0.60)" : "rgba(255,255,255,0.60)";

        weatherTemp.setStyle("-fx-font-family:'Syne'; -fx-font-size:44px; -fx-font-weight:bold; -fx-text-fill:" + tc + ";");
        weatherDesc.setStyle("-fx-font-size:14px; -fx-text-fill:" + sc + ";");
        weatherCity.setStyle("-fx-font-family:'Syne'; -fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + sc + ";");
        weatherHumidity.setStyle("-fx-font-family:'Syne'; -fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + tc + "; -fx-alignment:CENTER;");
        weatherWind.setStyle    ("-fx-font-family:'Syne'; -fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + tc + "; -fx-alignment:CENTER;");
        weatherFeels.setStyle   ("-fx-font-family:'Syne'; -fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + tc + "; -fx-alignment:CENTER;");

        // Стилізуємо підписи статистики (Вологість/Вітер/Відчувається)
        try {
            javafx.scene.layout.HBox statRow = (javafx.scene.layout.HBox)
                  weatherCard.getChildren().get(weatherCard.getChildren().size() - 1);
            for (javafx.scene.Node node : statRow.getChildren()) {
                if (node instanceof javafx.scene.layout.VBox vbox) {
                    for (javafx.scene.Node child : vbox.getChildren()) {
                        if (child instanceof javafx.scene.control.Label lbl) {
                            String txt = lbl.getText();
                            if (txt != null && (txt.equals("Вологість") || txt.equals("Вітер") || txt.equals("Відчувається"))) {
                                lbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + kc + "; -fx-alignment:CENTER;");
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean imageName_exists(String name) { return name != null; }

    private String fallbackGradient(String type) {
        return switch (type) {
            case "sunny"  -> "linear-gradient(from 0% 0% to 100% 100%, #e8a020, #f5c842, #ffdf70)";
            case "partly" -> "linear-gradient(from 0% 0% to 100% 100%, #4a90d9, #70aae8, #f5c842)";
            case "cloudy" -> "linear-gradient(from 0% 0% to 100% 100%, #6a7a8a, #8a9aaa, #b0bec5)";
            case "rain"   -> "linear-gradient(from 0% 0% to 100% 100%, #2a4a6a, #3a6a9a, #5a8aba)";
            case "storm"  -> "linear-gradient(from 0% 0% to 100% 100%, #1a1a2e, #2a2a4e, #4a4a6e)";
            case "snow"   -> "linear-gradient(from 0% 0% to 100% 100%, #8ab0d0, #b0cce0, #ddeeff)";
            case "fog"    -> "linear-gradient(from 0% 0% to 100% 100%, #7a8a8a, #9aaaaa, #bcc8c8)";
            default       -> "linear-gradient(from 0% 0% to 100% 100%, #27500a, #3b6d11, #639922)";
        };
    }

    // ── Динамічно заповнює секцію "Що включено" ─────────────────────────────
    private void populateIncluded(TourData data) {
        if (includedFlow == null) return;
        includedFlow.getChildren().clear();

        // Готель завжди показується якщо він є (не "Без готелю")
        String hotel = data.hotel();
        boolean hasHotel = hotel != null && !hotel.isBlank()
              && !hotel.equals("—") && !hotel.equals("-") && !hotel.equals("Без готелю");

        if (data.hasFlight())    addIncludedChip("Авіаперельот");
        if (hasHotel)            addIncludedChip("Готель");
        if (data.food() != null && !data.food().isBlank() && !data.food().equals("—"))
            addIncludedChip("Харчування");
        if (data.hasTransfer())  addIncludedChip("Трансфер");
        if (data.hasInsurance()) addIncludedChip("Страхування");
        if (data.hasGuide())     addIncludedChip("Гід");
    }

    private void addIncludedChip(String text) {
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(text);
        lbl.getStyleClass().add("tour-chip-plain");
        includedFlow.getChildren().add(lbl);
    }

    // ── Показує чіп тільки якщо значення непорожнє ──────────────────────────
    private void setChip(VBox box, Label lbl, String value) {
        boolean hasValue = value != null && !value.isBlank()
              && !value.equals("—") && !value.equals("-");
        lbl.setText(hasValue ? value : "");
        box.setVisible(hasValue);
        box.setManaged(hasValue);
    }
}