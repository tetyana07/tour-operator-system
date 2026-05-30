package com.touroperator.ui;

import com.touroperator.ui.VoyaAlert;
import com.touroperator.config.SpringContext;
import com.touroperator.domain.Tour;
import com.touroperator.service.TourService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.util.StringConverter;

public class NewTourController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML private TextField          nameField;
    @FXML private TextField          countryField;
    @FXML private TextField          cityField;
    @FXML private DatePicker         startDatePicker;
    @FXML private DatePicker         endDatePicker;
    @FXML private TextField          priceField;
    @FXML private Spinner<Integer>   quotaSpinner;
    @FXML private TextField          hotelField;
    @FXML private Label              statusLabel;

    @FXML private TextField          departureField;

    // Що включено
    @FXML private ComboBox<String>   mealCombo;
    @FXML private CheckBox           checkFlight;
    @FXML private CheckBox           checkTransfer;
    @FXML private CheckBox           checkInsurance;
    @FXML private CheckBox           checkGuide;

    // Фото
    @FXML private StackPane          photoPane;
    @FXML private ImageView          photoPreview;
    @FXML private Label              photoHintLabel;

    private String selectedImagePath = null;

    private Runnable onCancelCallback;
    private Runnable onSavedCallback;
    private boolean saving = false;

    @FXML
    public void initialize() {
        quotaSpinner.setValueFactory(
              new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 20));

        // Харчування
        if (mealCombo != null) {
            mealCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                  "Сніданок", "Напівпансіон", "Повний пансіон", "Все включено"
            ));
            mealCombo.setPromptText("Не включено");
        }

        StringConverter<LocalDate> conv = new StringConverter<>() {
            public String toString(LocalDate d) {
                return d != null ? d.format(DATE_FMT) : "";
            }
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try { return LocalDate.parse(s, DATE_FMT); }
                catch (DateTimeParseException e) { return null; }
            }
        };
        startDatePicker.setConverter(conv);
        endDatePicker.setConverter(conv);
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(7));


    }

    public void setOnCancel(Runnable r) { this.onCancelCallback = r; }
    public void setOnSaved(Runnable r)  { this.onSavedCallback  = r; }

    @FXML private void onCancel() {
        if (onCancelCallback != null) onCancelCallback.run();
    }

    /** Відкрити вибір фото */
    @FXML
    private void onChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Оберіть фото для туру");
        chooser.getExtensionFilters().add(
              new FileChooser.ExtensionFilter("Зображення", "*.jpg", "*.jpeg", "*.png", "*.webp"));

        File file = chooser.showOpenDialog(
              photoPane != null ? photoPane.getScene().getWindow() : null);

        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            // Показуємо прев'ю
            if (photoPreview != null) {
                try {
                    Image img = new Image(file.toURI().toString(), 280, 120, true, true);
                    photoPreview.setImage(img);
                    photoPreview.setVisible(true);
                    photoPreview.setManaged(true);
                } catch (Exception ignored) {}
            }
            if (photoHintLabel != null) {
                photoHintLabel.setText("✔ " + file.getName());
                photoHintLabel.setStyle("-fx-text-fill:#8fcba3;-fx-font-size:11px;");
            }
        }
    }

    @FXML
    private void onSave() {
        hideError();
        String name    = nameField.getText().trim();
        String country = countryField.getText().trim();
        String city    = cityField.getText().trim();

        if (name.isEmpty() || country.isEmpty() || city.isEmpty()) {
            showError("Заповніть усі текстові поля (назва, країна, місто).");
            return;
        }

        LocalDate startDate = getDateSafe(startDatePicker);
        LocalDate endDate   = getDateSafe(endDatePicker);

        if (startDate == null || endDate == null) {
            showError("Оберіть дату початку і завершення туру (формат: дд.мм.рррр).");
            return;
        }
        if (!endDate.isAfter(startDate)) {
            showError("Дата завершення має бути пізніше дати початку.");
            return;
        }
        if (startDate.isBefore(java.time.LocalDate.now())) {
            showError("Дата початку туру не може бути в минулому.");
            return;
        }

        BigDecimal price;
        try {
            String raw = priceField.getText().trim().replace(" ", "").replace(",", ".");
            price = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            showError("Невірний формат ціни. Введіть число, наприклад: 34800");
            return;
        }

        int quota = quotaSpinner.getValue();

        // Якщо введено назву готелю — автоматично створюємо запис в hotels
        java.util.UUID hotelId = null;
        String hotelName = hotelField != null ? hotelField.getText().trim() : "";
        if (!hotelName.isBlank()) {
            try {
                com.touroperator.repository.HotelRepository hotelRepo =
                      SpringContext.getBean(com.touroperator.repository.HotelRepository.class);
                com.touroperator.domain.Hotel newHotel = new com.touroperator.domain.Hotel();
                newHotel.setId(java.util.UUID.randomUUID());
                newHotel.setName(hotelName);
                newHotel.setStars(0);
                newHotel.setPricePerNight(java.math.BigDecimal.ZERO);
                hotelRepo.save(newHotel);
                hotelId = newHotel.getId();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Tour tour = new Tour(name, country, city, startDate, endDate, price, quota, hotelId);
        tour.setImagePath(selectedImagePath);
        // Що включено
        tour.setDeparture(departureField != null ? departureField.getText().trim() : null);
        tour.setMealType(mealCombo != null ? mealCombo.getValue() : null);
        tour.setHasFlight(checkFlight != null && checkFlight.isSelected());
        tour.setHasTransfer(checkTransfer != null && checkTransfer.isSelected());
        tour.setHasInsurance(checkInsurance != null && checkInsurance.isSelected());
        tour.setHasGuide(checkGuide != null && checkGuide.isSelected());

        try {
            TourService svc = SpringContext.getBean(TourService.class);
            svc.createTour(tour);
            if (onSavedCallback != null) onSavedCallback.run();
            if (onCancelCallback != null) onCancelCallback.run();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Помилка збереження: " + e.getMessage());
        }
    }

    private LocalDate getDateSafe(DatePicker dp) {
        if (dp.getValue() != null) return dp.getValue();
        String text = dp.getEditor().getText().trim();
        if (text.isBlank()) return null;
        try { return LocalDate.parse(text, DATE_FMT); } catch (DateTimeParseException e) { return null; }
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        // Прокручуємо форму вгору щоб помилка була видима
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Parent p = statusLabel.getParent();
            while (p != null) {
                if (p instanceof javafx.scene.control.ScrollPane sp) {
                    sp.setVvalue(0);
                    break;
                }
                p = p.getParent();
            }
        });
    }

    private void hideError() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
}