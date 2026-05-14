package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Booking;
import com.touroperator.domain.Client;
import com.touroperator.domain.Payment;
import com.touroperator.domain.Tour;
import com.touroperator.repository.BookingRepository;
import com.touroperator.repository.PaymentRepository;
import com.touroperator.repository.TourRepository;
import com.touroperator.service.BookingService;
import com.touroperator.service.ClientService;
import com.touroperator.service.PaymentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PaymentsController {

    @FXML private ComboBox<String> monthFilter;
    @FXML private ComboBox<String> typeFilter;

    @FXML private TableView<Payment>          paymentsTable;
    @FXML private TableColumn<Payment,String> colId;
    @FXML private TableColumn<Payment,String> colClient;
    @FXML private TableColumn<Payment,String> colBookingId;
    @FXML private TableColumn<Payment,String> colMethod;
    @FXML private TableColumn<Payment,String> colDate;
    @FXML private TableColumn<Payment,String> colAmount;
    @FXML private TableColumn<Payment,String> colStatus;
    @FXML private TableColumn<Payment,String> colActions;

    @FXML private Label statRevenue;
    @FXML private Label statRevenueChange;
    @FXML private Label statPending;
    @FXML private Label statPendingCount;
    @FXML private Label statRefunds;
    @FXML private Label statRefundsCount;

    private JdbcTemplate      jdbc;
    private BookingService    bookingService;
    private ClientService     clientService;
    private PaymentService    paymentService;
    private PaymentRepository paymentRepo;
    private BookingRepository bookingRepo;
    private TourRepository    tourRepo;

    private ObservableList<Payment> allPayments;
    private final Map<UUID, Client>  clientCache  = new HashMap<>();
    private final Map<UUID, Booking> bookingCache = new HashMap<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    public void initialize() {
        try {
            jdbc           = SpringContext.getBean(JdbcTemplate.class);
            bookingService = SpringContext.getBean(BookingService.class);
            clientService  = SpringContext.getBean(ClientService.class);
            paymentService = SpringContext.getBean(PaymentService.class);
            paymentRepo    = SpringContext.getBean(PaymentRepository.class);
            bookingRepo    = SpringContext.getBean(BookingRepository.class);
            tourRepo       = SpringContext.getBean(TourRepository.class);
            clientService.findAll().forEach(c -> clientCache.put(c.getId(), c));
            bookingService.findAll().forEach(b -> bookingCache.put(b.getId(), b));
            setupFilters();
            setupColumns();
            loadData();

            // Оновлення валюти в реальному часі
            ProfilePanelController.CurrencySession.addListener(this::loadData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupFilters() {
        // Динамічно генеруємо останні 6 місяців
        java.util.List<String> months = new java.util.ArrayList<>();
        months.add("Всі місяці");
        java.time.LocalDate now = java.time.LocalDate.now();
        String[] ukMonths = {"","Січень","Лютий","Березень","Квітень","Травень","Червень",
              "Липень","Серпень","Вересень","Жовтень","Листопад","Грудень"};
        for (int i = 0; i < 6; i++) {
            java.time.LocalDate d = now.minusMonths(i);
            months.add(ukMonths[d.getMonthValue()] + " " + d.getYear());
        }
        monthFilter.setItems(FXCollections.observableArrayList(months));
        monthFilter.getSelectionModel().selectFirst();
        typeFilter.setItems(FXCollections.observableArrayList(
              "Всі типи", "SUCCESS", "PENDING", "FAILED"
        ));
        typeFilter.getSelectionModel().selectFirst();
    }

    private void setupColumns() {
        // # — PAY-XXXX стиль
        colId.setCellValueFactory(c -> {
            String hex = c.getValue().getId().toString().replace("-","").substring(0,6).toUpperCase();
            return new SimpleStringProperty("PAY-" + hex.substring(0,4));
        });
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#8a9a85; -fx-font-size:13px;");
            }
        });

        // КЛІЄНТ
        colClient.setCellValueFactory(c -> {
            Booking b = bookingCache.get(c.getValue().getBookingId());
            if (b == null) return new SimpleStringProperty("—");
            Client cl = clientCache.get(b.getClientId());
            return new SimpleStringProperty(cl != null ? cl.getName() : "—");
        });
        colClient.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#0d2010; -fx-font-size:13px;");
            }
        });

        // БРОНЮВАННЯ — BK-XXXX
        colBookingId.setCellValueFactory(c -> {
            UUID bid = c.getValue().getBookingId();
            if (bid == null) return new SimpleStringProperty("—");
            Booking b = bookingCache.get(bid);
            if (b == null) return new SimpleStringProperty("—");
            String hex = bid.toString().replace("-","").substring(0,4).toUpperCase();
            return new SimpleStringProperty("BK-" + hex);
        });
        colBookingId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#4a7a2a; -fx-font-size:13px; -fx-font-weight:bold;");
            }
        });

        // МЕТОД — фіксований "Картка" або з БД якщо є поле
        colMethod.setCellValueFactory(c -> {
            String status = c.getValue().getStatus();
            if ("FAILED".equals(status)) return new SimpleStringProperty("🔄 Повернення");
            return new SimpleStringProperty("💳 Картка");
        });
        colMethod.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#4a5a46; -fx-font-size:13px;");
            }
        });

        // ДАТА
        colDate.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getPaymentDate();
            return new SimpleStringProperty(d != null ? d.format(DATE_FMT) : "—");
        });
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#4a5a46; -fx-font-size:13px;");
            }
        });

        // СУМА
        colAmount.setCellValueFactory(c -> {
            BigDecimal a = c.getValue().getAmount();
            if (a == null) return new SimpleStringProperty("—");
            String status = c.getValue().getStatus();
            String prefix = "FAILED".equals(status) ? "− " : "+ ";
            return new SimpleStringProperty(prefix + ProfilePanelController.CurrencySession.format(a));
        });
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                boolean isDanger = item != null && item.startsWith("−");
                setStyle(isDanger
                      ? "-fx-font-family:'Syne';-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#b83c2a;"
                      : "-fx-font-family:'Syne';-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#27500a;");
            }
        });

        // СТАТУС — pill
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label();
                switch (item) {
                    case "SUCCESS" -> { l.setText("Сплачено");   l.getStyleClass().add("pill-paid");       }
                    case "PENDING" -> { l.setText("Очікує");     l.getStyleClass().add("pill-pending");     }
                    case "FAILED"  -> { l.setText("Повернення"); l.getStyleClass().add("pill-cancelled");   }
                    default        -> { l.setText(item);         l.getStyleClass().add("pill-pending");     }
                }
                setGraphic(l); setText(null); setAlignment(Pos.CENTER_LEFT);
            }
        });

        // ДІЇ — кнопка підтвердити/квитанція
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); setText(null); return;
                }
                Payment p = getTableView().getItems().get(getIndex());
                Button btn = new Button();
                if ("PENDING".equals(p.getStatus())) {
                    btn.setText("✓ Підтвердити");
                    btn.getStyleClass().add("action-confirm-btn");
                    btn.setTooltip(new Tooltip("Підтвердити оплату"));
                    btn.setOnAction(e -> confirmPayment(p));
                } else if ("FAILED".equals(p.getStatus())) {
                    btn.setText("Повернення");
                    btn.getStyleClass().add("action-refund-btn");
                    btn.setTooltip(new Tooltip("Деталі повернення"));
                } else {
                    btn.setText("Квитанція");
                    btn.getStyleClass().add("action-receipt-btn");
                    btn.setTooltip(new Tooltip("Деталі платежу"));
                    btn.setOnAction(e -> showReceiptDialog(p));
                }
                HBox box = new HBox(btn);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(0, 0, 0, 2));
                setGraphic(box); setText(null);
            }
        });
    }

    /** Інвалідує кеш сторінок у MainController, щоб вони перезавантажились з БД. */
    private void invalidateBookingsPage() {
        try {
            Object userData = paymentsTable.getScene().getUserData();
            if (userData instanceof MainController mc) {
                mc.invalidatePage("bookings");
                mc.invalidatePage("clients");   // ← FIX: клієнти теж мають оновитись
                mc.invalidatePage("dashboard"); // ← FIX: дашборд також
            }
        } catch (Exception ignored) {}
    }

    private void showReceiptDialog(Payment p) {
        Booking booking = bookingCache.get(p.getBookingId());
        Client  client  = booking != null ? clientCache.get(booking.getClientId()) : null;
        Tour    tour    = null;
        if (booking != null && tourRepo != null) {
            tour = tourRepo.findById(booking.getTourId()).orElse(null);
        }

        // Формуємо короткі ID
        String payId = "PAY-" + p.getId().toString().replace("-","").substring(0,4).toUpperCase();
        String bkId  = booking != null
              ? "BK-" + booking.getId().toString().replace("-","").substring(0,4).toUpperCase() : "—";

        javafx.stage.Stage stage = new javafx.stage.Stage(javafx.stage.StageStyle.TRANSPARENT);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        javafx.stage.Window owner = paymentsTable.getScene() != null
              ? paymentsTable.getScene().getWindow() : null;
        if (owner != null) stage.initOwner(owner);

        // ── Модаль ────────────────────────────────────────────────────────
        VBox modal = new VBox(0);
        modal.getStyleClass().add("tour-detail-modal");
        modal.setMaxWidth(460);
        modal.setMinWidth(440);

        // ── Шапка ─────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setStyle(
              "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #2e5a10, #4a8a1a);" +
                    "-fx-background-radius: 28 28 0 0;"
        );
        VBox titleBlock = new VBox(3);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        Label titleLbl = new Label("Квитанція про оплату");
        titleLbl.setStyle("-fx-font-family:'Syne';-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label subLbl = new Label(payId + "  ·  " + p.getPaymentDate().format(DATE_FMT));
        subLbl.setStyle("-fx-font-size:11.5px;-fx-text-fill:rgba(255,255,255,0.72);");
        titleBlock.getChildren().addAll(titleLbl, subLbl);
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("tour-modal-close-btn");
        closeBtn.setOnAction(e -> stage.close());
        header.getChildren().addAll(titleBlock, closeBtn);

        // ── Тіло ──────────────────────────────────────────────────────────
        VBox body = new VBox(0);
        body.setPadding(new Insets(24, 28, 20, 28));

        // Статус
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(0, 0, 16, 0));
        Label statusLbl = new Label("Статус:");
        statusLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#8a9a85;");
        Label statusVal = new Label("Сплачено");
        statusVal.getStyleClass().add("pill-paid");
        statusRow.getChildren().addAll(statusLbl, statusVal);

        // Роздільник
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color:rgba(99,153,34,0.15);");
        VBox.setMargin(sep1, new Insets(0, 0, 14, 0));

        // Рядки деталей
        VBox details = new VBox(10);

        String clientName = client != null ? client.getName() : "—";
        String tourName   = tour   != null ? tour.getName()   : "—";
        String tourDest   = tour   != null ? tour.getCountry() + (tour.getCity() != null ? ", " + tour.getCity() : "") : "—";
        String tourists   = booking != null ? String.valueOf(booking.getTouristCount()) + " ос." : "—";
        String bookDate   = booking != null && booking.getBookingDate() != null
              ? booking.getBookingDate().format(DATE_FMT) : "—";

        details.getChildren().addAll(
              receiptRow("Клієнт",       clientName),
              receiptRow("Бронювання",   bkId),
              receiptRow("Тур",          tourName),
              receiptRow("Напрямок",     tourDest),
              receiptRow("Туристів",     tourists),
              receiptRow("Дата броні",   bookDate),
              receiptRow("Метод оплати", "Картка")
        );

        // Роздільник перед сумою
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color:rgba(99,153,34,0.15);");
        VBox.setMargin(sep2, new Insets(14, 0, 14, 0));

        // Сума
        HBox amountRow = new HBox();
        amountRow.setAlignment(Pos.CENTER_LEFT);
        Label amtKey = new Label("Сума оплати");
        amtKey.setStyle("-fx-font-size:13px;-fx-text-fill:#4a5a46;-fx-font-weight:bold;");
        HBox.setHgrow(amtKey, Priority.ALWAYS);
        Label amtVal = new Label(ProfilePanelController.CurrencySession.format(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO));
        amtVal.setStyle("-fx-font-family:'Syne';-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#27500a;");
        amountRow.getChildren().addAll(amtKey, amtVal);

        body.getChildren().addAll(statusRow, sep1, details, sep2, amountRow);

        // ── Футер ─────────────────────────────────────────────────────────
        HBox footer = new HBox();
        footer.getStyleClass().add("tour-detail-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPrefHeight(0);
        footer.setMinHeight(0);
        footer.setMaxHeight(0);
        footer.setManaged(false);
        footer.setVisible(false);

        modal.getChildren().addAll(header, body, footer);

        StackPane overlay = new StackPane(modal);
        overlay.getStyleClass().add("modal-overlay-pane");
        javafx.scene.Scene scene = new javafx.scene.Scene(overlay, 520, 480);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    /** Один рядок квитанції: ключ — значення */
    private HBox receiptRow(String key, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label k = new Label(key);
        k.setStyle("-fx-font-size:12.5px;-fx-text-fill:#8a9a85;-fx-min-width:120px;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size:12.5px;-fx-text-fill:#0d2010;-fx-font-weight:bold;");
        v.setWrapText(true);
        row.getChildren().addAll(k, v);
        return row;
    }

    private void confirmPayment(Payment p) {
        try {
            // Оновлюємо статус платежу з PENDING → SUCCESS
            paymentRepo.confirmByBookingId(p.getBookingId());
            // Оновлюємо статус бронювання → PAID
            bookingRepo.markPaid(p.getBookingId());
            bookingCache.clear();
            bookingService.findAll().forEach(b -> bookingCache.put(b.getId(), b));
            invalidateBookingsPage(); // інвалідує bookings + clients + dashboard
            loadData();
            // FIX: показуємо alert через Platform.runLater щоб уникнути невидимого вікна
            javafx.application.Platform.runLater(() -> VoyaAlert.success("Оплату підтверджено!"));
        } catch (Exception e) {
            VoyaAlert.error(e.getMessage());
        }
    }

    private void loadData() {
        try {
            List<Payment> payments = jdbc.query(
                  "SELECT id, booking_id, amount, payment_date, status FROM payments ORDER BY payment_date DESC",
                  (rs, i) -> {
                      Payment pay = new Payment();
                      try { pay.setId(UUID.fromString(rs.getString("id"))); } catch (Exception ignored) {}
                      try { pay.setBookingId(UUID.fromString(rs.getString("booking_id"))); } catch (Exception ignored) {}
                      try { pay.setAmount(rs.getBigDecimal("amount")); } catch (Exception ignored) {}
                      try {
                          java.sql.Date d = rs.getDate("payment_date");
                          pay.setPaymentDate(d != null ? d.toLocalDate() : LocalDate.now());
                      } catch (Exception ignored) { pay.setPaymentDate(LocalDate.now()); }
                      try { pay.setStatus(rs.getString("status")); } catch (Exception ignored) {}
                      return pay;
                  }
            );
            allPayments = FXCollections.observableArrayList(payments);
            paymentsTable.setItems(allPayments);
            updateStats(payments);
        } catch (Exception e) {
            e.printStackTrace();
            allPayments = FXCollections.observableArrayList();
            paymentsTable.setItems(allPayments);
            updateStats(java.util.Collections.emptyList());
        }
    }

    private void updateStats(List<Payment> payments) {
        // Виручка — тільки SUCCESS платежі
        BigDecimal revenue = payments.stream()
              .filter(p -> "SUCCESS".equals(p.getStatus()))
              .map(Payment::getAmount)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Очікується
        BigDecimal pending = BigDecimal.ZERO;
        long pendingCount = 0;
        try {
            BigDecimal p = jdbc.queryForObject(
                  "SELECT COALESCE(SUM(total_price), 0) FROM bookings WHERE status = 'CONFIRMED'",
                  BigDecimal.class);
            if (p != null) pending = p;
            Long cnt = jdbc.queryForObject(
                  "SELECT COUNT(*) FROM bookings WHERE status = 'CONFIRMED'",
                  Long.class);
            if (cnt != null) pendingCount = cnt;
        } catch (Exception e) { e.printStackTrace(); }

        // Повернення
        BigDecimal refunds = BigDecimal.ZERO;
        long refundCount = 0;
        try {
            BigDecimal r = jdbc.queryForObject(
                  "SELECT COALESCE(SUM(p.amount), 0) FROM payments p " +
                        "JOIN bookings b ON b.id = p.booking_id WHERE b.status = 'CANCELLED'",
                  BigDecimal.class);
            if (r != null) refunds = r;
            Long cnt = jdbc.queryForObject(
                  "SELECT COUNT(*) FROM bookings WHERE status = 'CANCELLED'",
                  Long.class);
            if (cnt != null) refundCount = cnt;
        } catch (Exception e) { e.printStackTrace(); }

        final BigDecimal fRevenue = revenue;
        final BigDecimal fPending = pending;
        final long fPendingCount = pendingCount;
        final BigDecimal fRefunds = refunds;
        final long fRefundCount = refundCount;

        javafx.application.Platform.runLater(() -> {
            if (statRevenue      != null) statRevenue.setText(ProfilePanelController.CurrencySession.format(fRevenue));
            if (statRevenueChange!= null) statRevenueChange.setText(payments.size() + " транзакцій всього");
            if (statPending      != null) statPending.setText(ProfilePanelController.CurrencySession.format(fPending));
            if (statPendingCount != null) statPendingCount.setText(fPendingCount + " непідтверджених");
            if (statRefunds      != null) statRefunds.setText(ProfilePanelController.CurrencySession.format(fRefunds));
            if (statRefundsCount != null) statRefundsCount.setText(fRefundCount + " скасованих бронювань");
        });
    }

    @FXML private void onFilter() { applyFilters(); }

    private void applyFilters() {
        String type = typeFilter.getValue();
        ObservableList<Payment> filtered = FXCollections.observableArrayList();
        for (Payment p : allPayments) {
            boolean ok = "Всі типи".equals(type) || type.equals(p.getStatus());
            if (ok) filtered.add(p);
        }
        paymentsTable.setItems(filtered);
    }

    @FXML private void onAddPayment() {
        javafx.stage.Stage stage = new javafx.stage.Stage(javafx.stage.StageStyle.TRANSPARENT);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        // Прив'язуємо до головного вікна — без цього JavaFX рендерить поза екраном
        javafx.stage.Window owner = paymentsTable.getScene() != null
              ? paymentsTable.getScene().getWindow() : null;
        if (owner != null) stage.initOwner(owner);

        // ── Головний контейнер модалі ──────────────────────────────────────
        VBox modal = new VBox(0);
        modal.getStyleClass().add("tour-detail-modal");
        modal.setMaxWidth(500);
        modal.setMinWidth(480);

        // ── Шапка ─────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setStyle(
              "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #2e5a10, #4a8a1a);" +
                    "-fx-background-radius: 28 28 0 0;"
        );

        VBox titleBlock = new VBox(3);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        Label titleLbl = new Label("💳  Прийняти оплату");
        titleLbl.setStyle("-fx-font-family:'Syne';-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label subLbl = new Label("Підтвердити платіж за бронювання");
        subLbl.setStyle("-fx-font-size:11.5px;-fx-text-fill:rgba(255,255,255,0.72);");
        titleBlock.getChildren().addAll(titleLbl, subLbl);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("tour-modal-close-btn");
        closeBtn.setOnAction(e -> stage.close());
        header.getChildren().addAll(titleBlock, closeBtn);

        // ── Тіло ──────────────────────────────────────────────────────────
        VBox body = new VBox(18);
        body.setPadding(new Insets(24, 26, 8, 26));

        // Вибір бронювання
        List<Booking> confirmable = bookingService.findAll().stream()
              .filter(b -> "CONFIRMED".equals(b.getStatus()))
              .collect(Collectors.toList());

        VBox bookingSection = new VBox(8);
        Label bookingLbl = new Label("БРОНЮВАННЯ (статус: CONFIRMED)");
        bookingLbl.getStyleClass().add("tour-detail-section-title");

        ComboBox<Booking> bookingCombo = new ComboBox<>();
        bookingCombo.setMaxWidth(Double.MAX_VALUE);
        bookingCombo.getItems().addAll(confirmable);
        bookingCombo.setPromptText("— оберіть бронювання —");
        bookingCombo.setStyle(
              "-fx-background-color:#f4faea;" +
                    "-fx-border-color:rgba(99,153,34,0.30);" +
                    "-fx-border-width:1.5;" +
                    "-fx-border-radius:10;" +
                    "-fx-background-radius:10;" +
                    "-fx-font-size:13px;" +
                    "-fx-text-fill:#0d2010;" +
                    "-fx-pref-height:40px;"
        );
        bookingCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Booking b) {
                if (b == null) return "— оберіть бронювання —";
                Client c = clientCache.get(b.getClientId());
                String name = c != null ? c.getName() : "Невідомий клієнт";
                String hex  = b.getId().toString().replace("-","").substring(0,4).toUpperCase();
                String sum  = b.getTotalPrice() != null
                      ? ProfilePanelController.CurrencySession.format(b.getTotalPrice()) : "";
                return name + "  ·  " + sum + "  [BK-" + hex + "]";
            }
            @Override public Booking fromString(String s) { return null; }
        });

        // Картка з деталями вибраного бронювання
        VBox detailCard = new VBox(10);
        detailCard.setPadding(new Insets(14, 16, 14, 16));
        detailCard.setStyle(
              "-fx-background-color:#f4faea;" +
                    "-fx-border-color:rgba(99,153,34,0.20);" +
                    "-fx-border-width:1;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;"
        );
        detailCard.setVisible(false);
        detailCard.setManaged(false);

        HBox amountRow = new HBox(10);
        amountRow.setAlignment(Pos.CENTER_LEFT);
        Label amountLblKey = new Label("Сума до оплати:");
        amountLblKey.setStyle("-fx-font-size:13px;-fx-text-fill:#4a5a46;");
        Label amountLblVal = new Label("—");
        amountLblVal.setStyle("-fx-font-family:'Syne';-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#27500a;");
        amountRow.getChildren().addAll(amountLblKey, amountLblVal);

        HBox clientRow = new HBox(10);
        clientRow.setAlignment(Pos.CENTER_LEFT);
        Label clientKey = new Label("Клієнт:");
        clientKey.setStyle("-fx-font-size:13px;-fx-text-fill:#4a5a46;-fx-min-width:80px;");
        Label clientVal = new Label("—");
        clientVal.setStyle("-fx-font-size:13px;-fx-text-fill:#0d2010;-fx-font-weight:bold;");
        clientRow.getChildren().addAll(clientKey, clientVal);

        detailCard.getChildren().addAll(amountRow, clientRow);

        bookingCombo.setOnAction(e -> {
            Booking sel = bookingCombo.getValue();
            if (sel != null) {
                if (sel.getTotalPrice() != null)
                    amountLblVal.setText(ProfilePanelController.CurrencySession.format(sel.getTotalPrice()));
                Client cl = clientCache.get(sel.getClientId());
                clientVal.setText(cl != null ? cl.getName() : "—");
                detailCard.setVisible(true);
                detailCard.setManaged(true);
            } else {
                detailCard.setVisible(false);
                detailCard.setManaged(false);
            }
        });

        bookingSection.getChildren().addAll(bookingLbl, bookingCombo, detailCard);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill:#b83c2a;-fx-font-size:12px;-fx-font-weight:bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        if (confirmable.isEmpty()) {
            Label emptyNote = new Label("⚠ Немає підтверджених бронювань для оплати");
            emptyNote.setStyle("-fx-text-fill:#8a5a00;-fx-font-size:12.5px;");
            bookingSection.getChildren().add(emptyNote);
            bookingCombo.setDisable(true);
        }

        body.getChildren().addAll(bookingSection, errorLabel);

        // ── Футер ─────────────────────────────────────────────────────────
        HBox footer = new HBox(10);
        footer.getStyleClass().add("tour-detail-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Скасувати");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> stage.close());

        Button payBtn = new Button("✓  Підтвердити оплату");
        payBtn.getStyleClass().add("add-btn");
        payBtn.setDisable(confirmable.isEmpty());
        payBtn.setOnAction(e -> {
            Booking sel = bookingCombo.getValue();
            if (sel == null) {
                errorLabel.setText("⚠ Оберіть бронювання зі списку.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            try {
                // Якщо вже є платіж зі статусом PENDING — тільки оновлюємо
                boolean existingPayment = paymentRepo.findByBookingId(sel.getId()).isPresent();
                if (existingPayment) {
                    paymentRepo.confirmByBookingId(sel.getId());
                    bookingRepo.markPaid(sel.getId());
                } else {
                    paymentService.pay(sel.getId());
                }
                // FIX: готуємо дані для alert ДО закриття stage
                Client cl = clientCache.get(sel.getClientId());
                String successMsg = "Оплату прийнято!\nКлієнт: "
                      + (cl != null ? cl.getName() : "—")
                      + "\nСума: ₴ " + String.format("%,.0f", sel.getTotalPrice());
                // FIX: закриваємо модаль ПЕРШ НІЖ оновлювати дані й показувати alert
                stage.close();
                bookingCache.clear();
                bookingService.findAll().forEach(b -> bookingCache.put(b.getId(), b));
                invalidateBookingsPage();
                loadData();
                // FIX: показуємо alert через Platform.runLater — після того як showAndWait повністю завершився
                javafx.application.Platform.runLater(() -> VoyaAlert.success(successMsg));
            } catch (Exception ex) {
                errorLabel.setText("⚠ " + ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });
        footer.getChildren().addAll(cancelBtn, payBtn);

        modal.getChildren().addAll(header, body, footer);

        StackPane overlay = new StackPane(modal);
        overlay.getStyleClass().add("modal-overlay-pane");
        javafx.scene.Scene scene = new javafx.scene.Scene(overlay, 560, 360);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }
}