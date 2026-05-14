package com.touroperator.ui;

import com.touroperator.ui.VoyaAlert;
import com.touroperator.config.SpringContext;
import com.touroperator.domain.*;
import com.touroperator.dto.BookingRequest;
import com.touroperator.service.*;
import com.touroperator.repository.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;
import javafx.scene.layout.VBox;

/**
 * Контролер форми нового бронювання.
 * Завантажує готель, екскурсії (по туру), трансфер, страховку з БД.
 */
public class NewBookingController {

    @FXML private ComboBox<Client>    clientCombo;
    @FXML private ComboBox<Tour>      tourCombo;
    @FXML private Spinner<Integer>    paxSpinner;
    @FXML private Spinner<Integer>    childSpinner;
    @FXML private TextField           promoField;

    // Нові комбобокси
    @FXML private ComboBox<Hotel>     hotelCombo;
    @FXML private ComboBox<Transfer>  transferCombo;
    @FXML private ComboBox<Insurance> insuranceCombo;
    @FXML private ComboBox<Excursion> excursionCombo;
    @FXML private ComboBox<String>    discountCombo;

    @FXML private Label baseLine;
    @FXML private Label basePrice;
    @FXML private Label discountLine;
    @FXML private Label totalPrice;
    @FXML private Label dynamicLabel;
    @FXML private Label tourBadge;
    @FXML private VBox tourSection;
    @FXML private VBox clientSection;

    private Runnable onCloseCallback;
    private Runnable onSavedCallback;

    public void setOnClose(Runnable r)  { this.onCloseCallback  = r; }
    public void setOnSaved(Runnable r)  { this.onSavedCallback  = r; }

    @FXML private void onClose() { if (onCloseCallback != null) onCloseCallback.run(); }

    private ClientService    clientService;
    private TourService      tourService;
    private BookingService   bookingService;
    private PricingService   pricingService;
    private HotelRepository  hotelRepo;
    private InsuranceRepository insuranceRepo;
    private TransferRepository  transferRepo;
    private ExcursionRepository excursionRepo;

    @FXML
    public void initialize() {
        try {
            clientService  = SpringContext.getBean(ClientService.class);
            tourService    = SpringContext.getBean(TourService.class);
            bookingService = SpringContext.getBean(BookingService.class);
            pricingService = SpringContext.getBean(PricingService.class);
            hotelRepo      = SpringContext.getBean(HotelRepository.class);
            insuranceRepo  = SpringContext.getBean(InsuranceRepository.class);
            transferRepo   = SpringContext.getBean(TransferRepository.class);
            excursionRepo  = SpringContext.getBean(ExcursionRepository.class);

            // ── Клієнти ─────────────────────────────────────────────────────
            List<Client> clients = clientService.findAll();
            clientCombo.setItems(FXCollections.observableArrayList(clients));
            clientCombo.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Client c) { return c != null ? c.getName() : ""; }
                public Client fromString(String s) { return null; }
            });
            if (!clients.isEmpty()) clientCombo.getSelectionModel().selectFirst();

            // ── Тури ────────────────────────────────────────────────────────
            List<Tour> tours = tourService.findAll();
            tourCombo.setItems(FXCollections.observableArrayList(tours));
            tourCombo.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Tour t) {
                    return t != null ? t.getName() + " (" + t.getCountry() + ", " + t.getCity() + ")" : "";
                }
                public Tour fromString(String s) { return null; }
            });
            if (!tours.isEmpty()) tourCombo.getSelectionModel().selectFirst();

            // ── Спінери ─────────────────────────────────────────────────────
            paxSpinner.setValueFactory(
                  new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 2));
            if (childSpinner != null)
                childSpinner.setValueFactory(
                      new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));

            // ── Страховка ────────────────────────────────────────────────────
            if (insuranceCombo != null) {
                List<Insurance> ins = insuranceRepo.findAll();
                // Додаємо "Без страховки" як перший елемент
                insuranceCombo.setConverter(new javafx.util.StringConverter<>() {
                    public String toString(Insurance i) {
                        return i != null ? i.getName() + " (+₴" + String.format("%,.0f", i.getPrice()) + ")" : "Без страховки";
                    }
                    public Insurance fromString(String s) { return null; }
                });
                List<Insurance> insWithNull = new ArrayList<>();
                insWithNull.add(null);
                insWithNull.addAll(ins);
                insuranceCombo.setItems(FXCollections.observableArrayList(insWithNull));
                insuranceCombo.getSelectionModel().selectFirst();
            }

            // ── Трансфер ─────────────────────────────────────────────────────
            if (transferCombo != null) {
                List<Transfer> transfers = transferRepo.findAll();
                transferCombo.setConverter(new javafx.util.StringConverter<>() {
                    public String toString(Transfer t) {
                        return t != null ? t.getType() + " (+₴" + String.format("%,.0f", t.getPrice()) + ")" : "Без трансферу";
                    }
                    public Transfer fromString(String s) { return null; }
                });
                List<Transfer> transWithNull = new ArrayList<>();
                transWithNull.add(null);
                transWithNull.addAll(transfers);
                transferCombo.setItems(FXCollections.observableArrayList(transWithNull));
                transferCombo.getSelectionModel().selectFirst();
            }

            // ── Тип знижки (інфо-поле) ───────────────────────────────────────
            if (discountCombo != null) {
                discountCombo.setItems(FXCollections.observableArrayList(
                      "Без додаткової знижки", "Пенсіонер (-10%)", "Корпоратив (-15%)", "Ювілей (-20%)"));
                discountCombo.getSelectionModel().selectFirst();
            }

            // ── Слухачі для перерахунку ───────────────────────────────────────
            paxSpinner.valueProperty().addListener((o, ov, nv) -> updateTotal());
            if (childSpinner != null)
                childSpinner.valueProperty().addListener((o, ov, nv) -> updateTotal());
            tourCombo.valueProperty().addListener((o, ov, nv) -> { onTourChanged(); });
            if (insuranceCombo != null)
                insuranceCombo.valueProperty().addListener((o, ov, nv) -> updateTotal());
            if (transferCombo != null)
                transferCombo.valueProperty().addListener((o, ov, nv) -> updateTotal());
            if (excursionCombo != null)
                excursionCombo.valueProperty().addListener((o, ov, nv) -> updateTotal());

            // Ініціалізуємо дані для першого туру
            onTourChanged();

            // Оновлюємо розрахунок при зміні валюти
            ProfilePanelController.CurrencySession.addListener(
                  () -> javafx.application.Platform.runLater(this::updateTotal)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** При зміні туру — оновлюємо готель та екскурсії */
    @FXML
    private void onTourChanged() {
        Tour tour = tourCombo != null ? tourCombo.getValue() : null;
        if (tour == null) return;

        // Готель туру
        if (hotelCombo != null) {
            hotelCombo.getItems().clear();
            if (tour.getHotelId() != null) {
                hotelRepo.findById(tour.getHotelId()).ifPresent(hotel -> {
                    hotelCombo.setConverter(new javafx.util.StringConverter<>() {
                        public String toString(Hotel h) {
                            if (h == null) return "Без готелю";
                            String stars = "★".repeat(h.getStars());
                            return h.getName() + " " + stars;
                        }
                        public Hotel fromString(String s) { return null; }
                    });
                    hotelCombo.setItems(FXCollections.observableArrayList(hotel));
                    hotelCombo.getSelectionModel().selectFirst();
                });
            } else {
                hotelCombo.setConverter(new javafx.util.StringConverter<>() {
                    public String toString(Hotel h) { return h != null ? h.getName() : "Готель не призначено"; }
                    public Hotel fromString(String s) { return null; }
                });
                // Завантажити всі готелі якщо тур без готелю
                List<Hotel> hotels = hotelRepo.findAll();
                List<Hotel> hotelsWithNull = new ArrayList<>();
                hotelsWithNull.add(null);
                hotelsWithNull.addAll(hotels);
                hotelCombo.setItems(FXCollections.observableArrayList(hotelsWithNull));
                hotelCombo.getSelectionModel().selectFirst();
            }
        }

        // Екскурсії для цього туру
        if (excursionCombo != null) {
            List<Excursion> excursions = excursionRepo.findByTourId(tour.getId());
            excursionCombo.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Excursion e) {
                    return e != null ? e.getName() + " (+₴" + String.format("%,.0f", e.getPrice()) + ")" : "Без екскурсії";
                }
                public Excursion fromString(String s) { return null; }
            });
            List<Excursion> excWithNull = new ArrayList<>();
            excWithNull.add(null);
            excWithNull.addAll(excursions);
            excursionCombo.setItems(FXCollections.observableArrayList(excWithNull));
            excursionCombo.getSelectionModel().selectFirst();
        }

        updateTotal();
    }

    @FXML
    private void onPromoApply() {
        updateTotal();
        String code = promoField != null ? promoField.getText().trim() : "";
        if (!code.isEmpty()) {
            var svc = SpringContext.getBean(PromoCodeService.class);
            boolean found = svc.findByCode(code).isPresent();
            if (promoField != null)
                promoField.setStyle(found ? "" : "-fx-border-color:#b83c2a;");
        }
    }

    private void updateTotal() {
        Tour tour = tourCombo != null ? tourCombo.getValue() : null;
        if (tour == null) return;

        int pax   = paxSpinner.getValue() != null ? paxSpinner.getValue() : 1;
        int child = childSpinner != null && childSpinner.getValue() != null ? childSpinner.getValue() : 0;
        int total = pax + child;

        java.util.List<java.util.UUID> excIds = new java.util.ArrayList<>();
        if (excursionCombo != null && excursionCombo.getValue() != null)
            excIds.add(excursionCombo.getValue().getId());

        java.util.UUID insId = (insuranceCombo != null && insuranceCombo.getValue() != null)
              ? insuranceCombo.getValue().getId() : null;
        java.util.UUID trId  = (transferCombo  != null && transferCombo.getValue()  != null)
              ? transferCombo.getValue().getId()  : null;
        String promoCode = promoField != null ? promoField.getText().trim() : null;
        if (promoCode != null && promoCode.isBlank()) promoCode = null;

        // Баг 1 виправлено: зчитуємо вибраний тип знижки з discountCombo
        int extraDiscount = parseExtraDiscountPercent(
              discountCombo != null ? discountCombo.getValue() : null);

        try {
            // Баг 1 виправлено: передаємо extraDiscount у PricingService
            var breakdown = pricingService.calculate(tour, total, child, excIds, insId, trId, promoCode, extraDiscount);

            if (baseLine   != null) baseLine.setText("Базова ціна × " + total);
            // Баг 2 виправлено: basePrice ← getBasePrice(), discountLine ← getTotalDiscount(), totalPrice ← getFinalPrice()
            if (basePrice   != null) basePrice.setText(fmt(breakdown.getBasePrice()));
            if (discountLine != null) discountLine.setText("−" + fmt(breakdown.getTotalDiscount()));
            if (totalPrice   != null) totalPrice.setText(fmt(breakdown.getFinalPrice()));
            if (dynamicLabel != null) dynamicLabel.setVisible(breakdown.hasDynamicSurcharge());
        } catch (Exception e) {
            if (totalPrice != null) totalPrice.setText("Помилка розрахунку");
        }
    }

    /**
     * Перетворює рядок з discountCombo на відсоток знижки.
     */
    private int parseExtraDiscountPercent(String selected) {
        if (selected == null) return 0;
        if (selected.contains("10%")) return 10;
        if (selected.contains("15%")) return 15;
        if (selected.contains("20%")) return 20;
        return 0;
    }

    @FXML
    private void onSaveBooking() {
        Client client = clientCombo != null ? clientCombo.getValue() : null;
        Tour tour     = tourCombo  != null ? tourCombo.getValue()  : null;

        if (client == null || tour == null) {
            VoyaAlert.warning("Оберіть клієнта та тур");
            return;
        }

        int pax   = paxSpinner.getValue() != null ? paxSpinner.getValue() : 1;
        int child = childSpinner != null && childSpinner.getValue() != null ? childSpinner.getValue() : 0;
        String promoCode = promoField != null ? promoField.getText().trim() : "";

        // Збираємо ID екскурсій
        List<UUID> excursionIds = new ArrayList<>();
        if (excursionCombo != null && excursionCombo.getValue() != null) {
            excursionIds.add(excursionCombo.getValue().getId());
        }

        // ID страховки і трансферу
        UUID insuranceId = (insuranceCombo != null && insuranceCombo.getValue() != null)
              ? insuranceCombo.getValue().getId() : null;
        UUID transferId  = (transferCombo  != null && transferCombo.getValue()  != null)
              ? transferCombo.getValue().getId()  : null;

        // Баг 1 виправлено: зчитуємо тип знижки і додаємо до запиту
        int extraDiscount = parseExtraDiscountPercent(
              discountCombo != null ? discountCombo.getValue() : null);

        BookingRequest req = new BookingRequest(
              client.getId(), tour.getId(), pax + child,
              excursionIds, insuranceId, transferId,
              promoCode.isEmpty() ? null : promoCode
        );
        req.setChildCount(child);
        req.setExtraDiscountPercent(extraDiscount);

        try {
            var booking = bookingService.createBooking(req);
            VoyaAlert.success("Бронювання створено!\nСума: " + fmt(booking.getTotalPrice()));
            if (onSavedCallback != null) onSavedCallback.run();
            onClose();
        } catch (Exception e) {
            VoyaAlert.error(e.getMessage());
        }
    }

    /** Попередньо вибрати тур у комбобоксі */
    public void preselectTour(Tour tour) {
        if (tourCombo == null || tour == null) return;
        tourCombo.getItems().stream()
              .filter(t -> t.getId() != null && t.getId().equals(tour.getId()))
              .findFirst()
              .ifPresent(t -> {
                  tourCombo.getSelectionModel().select(t);
                  onTourChanged();
              });
        // Оновлюємо бейдж у хедері
        if (tourBadge != null) {
            tourBadge.setText("📍 " + tour.getName() + "  ·  " + tour.getCountry());
            tourBadge.setVisible(true);
            tourBadge.setManaged(true);
        }
        // Ховаємо секцію вибору туру — він вже обраний
        if (tourSection != null) {
            tourSection.setVisible(false);
            tourSection.setManaged(false);
        }
    }

    /**
     * Викликається перед показом діалогу.
     * Для клієнта — автоматично обирає його як клієнта і ховає вибір клієнта.
     */
    public void setClientContext(UserRole role, String email) {
        if (role == UserRole.ADMIN) {
            // Адмін — показуємо секцію вибору клієнта
            if (clientSection != null) {
                clientSection.setVisible(true);
                clientSection.setManaged(true);
            }
            // clientCombo вже заповнений у initialize() — нічого більше не треба
            return;
        }
        // Клієнт — автоматично обираємо його і ховаємо секцію
        if (email == null || email.isBlank()) return;
        try {
            com.touroperator.repository.ClientRepository clientRepo =
                  com.touroperator.config.SpringContext.getBean(
                        com.touroperator.repository.ClientRepository.class);
            clientRepo.findByEmail(email).ifPresent(client -> {
                if (clientCombo != null) {
                    clientCombo.getItems().stream()
                          .filter(c -> c.getId().equals(client.getId()))
                          .findFirst()
                          .ifPresent(c -> clientCombo.getSelectionModel().select(c));
                }
                if (clientSection != null) {
                    clientSection.setVisible(false);
                    clientSection.setManaged(false);
                }
            });
        } catch (Exception ignored) {}
    }

    @FXML private void onExcursionChanged() { updateTotal(); }
    @FXML private void onDiscountChanged()  { updateTotal(); }

    private String fmt(java.math.BigDecimal amount) {
        if (amount == null) return ProfilePanelController.CurrencySession.getSymbol() + "0";
        return ProfilePanelController.CurrencySession.format(amount);
    }
}