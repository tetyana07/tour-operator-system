package com.touroperator.ui;

import com.touroperator.ui.VoyaAlert;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Booking;
import com.touroperator.domain.Client;
import com.touroperator.domain.Tour;
import com.touroperator.service.BookingAuditService;
import com.touroperator.service.BookingService;
import com.touroperator.service.ClientService;
import com.touroperator.service.QuotaService;
import com.touroperator.service.TourService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.net.URI;
import java.awt.Desktop;

public class BookingsController implements RoleAware {

    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> tourFilter;
    @FXML private TableView<Booking>          bookingsTable;
    @FXML private TableColumn<Booking,String> colId;
    @FXML private TableColumn<Booking,String> colClient;
    @FXML private TableColumn<Booking,String> colTour;
    @FXML private TableColumn<Booking,String> colPax;
    @FXML private TableColumn<Booking,String> colDates;
    @FXML private TableColumn<Booking,String> colCost;
    @FXML private TableColumn<Booking,String> colStatus;
    @FXML private Label bookingCountLabel;
    @FXML private Label statTotal;
    @FXML private Label statConfirmed;
    @FXML private Label statPaid;
    @FXML private Label statCancelled;
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private Button btnNewBooking;

    @FXML private Button btnConfirm;
    @FXML private Button btnComplete;
    @FXML private Button btnCancel;
    @FXML private Button btnUndo;
    @FXML private Button btnRedo;
    @FXML private HBox   adminActionBar;

     
    @FXML private javafx.scene.layout.VBox auditPanel;
    @FXML private Label                    auditTitle;
    @FXML private javafx.scene.layout.VBox auditTimeline;

     
    @FXML private javafx.scene.layout.VBox adminView;
    @FXML private javafx.scene.layout.VBox clientView;
    @FXML private javafx.scene.layout.VBox clientCardsContainer;
    @FXML private javafx.scene.layout.VBox emptyState;
    @FXML private Label                    bookingCountLabelClient;
    @FXML private ComboBox<String>         statusFilterClient;

    @FXML private javafx.scene.layout.VBox detailPanel;
    @FXML private Label detailTitle;
    @FXML private Label detailSub;

    private BookingService bookingService;
    private ClientService  clientService;
    private TourService    tourService;
    private QuotaService   quotaService;
    private BookingAuditService auditService;
    private ObservableList<Booking> allBookings;
    private final Map<UUID, Client> clientCache = new HashMap<>();
    private final Map<UUID, Tour>   tourCache   = new HashMap<>();

    private UserRole currentRole  = UserRole.CLIENT;
    private String   currentEmail = "";

    @Override
    public void setRole(UserRole role, String email) {
        this.currentRole  = role;
        this.currentEmail = email;
        boolean isAdmin = role == UserRole.ADMIN;

        if (pageTitle != null) pageTitle.setText(isAdmin ? "Бронювання" : "Мої бронювання");
        if (pageSubtitle != null) {
            pageSubtitle.setText(isAdmin ? "" : "Тут відображаються лише ваші поїздки");
            pageSubtitle.setVisible(!isAdmin);
            pageSubtitle.setManaged(!isAdmin);
        }
        if (btnNewBooking != null) { btnNewBooking.setVisible(isAdmin); btnNewBooking.setManaged(isAdmin); }

         
        if (adminView  != null) { adminView.setVisible(isAdmin);   adminView.setManaged(isAdmin); }
        if (clientView != null) { clientView.setVisible(!isAdmin); clientView.setManaged(!isAdmin); }

        if (colClient   != null) colClient.setVisible(isAdmin);
        if (btnConfirm  != null) { btnConfirm.setVisible(isAdmin);  btnConfirm.setManaged(isAdmin); }
        if (btnComplete != null) { btnComplete.setVisible(isAdmin); btnComplete.setManaged(isAdmin); }
        if (btnCancel   != null) { btnCancel.setVisible(isAdmin);   btnCancel.setManaged(isAdmin); }
        if (adminActionBar != null) { adminActionBar.setVisible(isAdmin); adminActionBar.setManaged(isAdmin); }

        if (bookingService != null) loadData();
    }

    @FXML
    public void initialize() {
        try {
            bookingService = SpringContext.getBean(BookingService.class);
            clientService  = SpringContext.getBean(ClientService.class);
            tourService    = SpringContext.getBean(TourService.class);
            quotaService   = SpringContext.getBean(QuotaService.class);
            auditService   = SpringContext.getBean(BookingAuditService.class);
            clientService.findAll().forEach(c -> clientCache.put(c.getId(), c));
            tourService.findAll().forEach(t -> tourCache.put(t.getId(), t));

             
            quotaService.addListener(new QuotaService.QuotaListener() {
                @Override
                public void onQuotaChanged(Tour tour, int remaining) {
                    if (remaining <= 3 && remaining > 0) {
                        Platform.runLater(() ->
                              VoyaAlert.warning("⚠ Тур «" + tour.getName() + "»: залишилось лише " + remaining + " місць!")
                        );
                    }
                }
                @Override
                public void onQuotaExceeded(Tour tour) {
                    Platform.runLater(() ->
                          VoyaAlert.error("🚫 Тур «" + tour.getName() + "»: місця вичерпані!")
                    );
                }
            });

            setupFilters();
            setupColumns();
            setupSelectionListener();
            loadData();

             
            var cm = getCommandManager();
            if (btnUndo != null) {
                btnUndo.disableProperty().bind(cm.canUndoProperty().not());
                cm.undoLabelProperty().addListener((obs, o, n) -> {
                    if (btnUndo != null) btnUndo.setText(n.isEmpty() ? "↩ Скасувати дію" : "↩ " + n);
                });
            }
            if (btnRedo != null) {
                btnRedo.disableProperty().bind(cm.canRedoProperty().not());
                cm.redoLabelProperty().addListener((obs, o, n) -> {
                    if (btnRedo != null) btnRedo.setText(n.isEmpty() ? "↪ Повторити" : "↪ " + n);
                });
            }

             
            ProfilePanelController.CurrencySession.addListener(this::loadData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     
    private void setupSelectionListener() {
        bookingsTable.getSelectionModel().selectedItemProperty().addListener(
              (obs, oldVal, newVal) -> {
                  updateActionButtons(newVal);
                  if (newVal != null && currentRole == UserRole.ADMIN) {
                      showAuditPanel(newVal);
                  } else {
                      hideAuditPanel();
                  }
              });
        updateActionButtons(null);
    }

    /**
     * Логіка статусів (адмін НЕ оплачує за клієнта):
     * NEW       → можна: Підтвердити, Скасувати
     * CONFIRMED → можна: Скасувати (клієнт сам оплачує)
     * PAID      → можна: Завершити
     * COMPLETED → нічого (кінцевий статус)
     * CANCELLED → нічого (кінцевий статус)
     */
    private void updateActionButtons(Booking b) {
        String status = b != null ? b.getStatus() : "";
        boolean hasCreated   = "CREATED".equals(status);
        boolean hasConfirmed = "CONFIRMED".equals(status);
        boolean hasPaid      = "PAID".equals(status);
        boolean canCancel    = hasCreated || hasConfirmed;

        setBtn(btnConfirm,  hasCreated);
        setBtn(btnComplete, hasPaid);
        setBtn(btnCancel,   canCancel);
    }

    private void setBtn(Button btn, boolean active) {
        if (btn == null) return;
        btn.setDisable(!active);
        btn.setOpacity(active ? 1.0 : 0.4);
    }

     
    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(
              "Всі статуси","CREATED","CONFIRMED","PAID","COMPLETED","CANCELLED"));
        statusFilter.getSelectionModel().selectFirst();
        List<String> names = new ArrayList<>();
        names.add("Всі тури");
        tourCache.values().stream().map(Tour::getName).sorted().forEach(names::add);
        tourFilter.setItems(FXCollections.observableArrayList(names));
        tourFilter.getSelectionModel().selectFirst();
    }

     
    private void setupColumns() {
         
        colId.setCellValueFactory(c -> {
             
            String id = c.getValue().getId().toString();
             
            String shortHex = id.replace("-","").substring(0,4).toUpperCase();
            return new SimpleStringProperty("BK-" + shortHex);
        });
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("-fx-text-fill:#8a9a85; -fx-font-size:13px;");
            }
        });
        colClient.setCellValueFactory(c -> {
            Client cl = clientCache.get(c.getValue().getClientId());
            return new SimpleStringProperty(cl != null ? cl.getName() : "—");
        });
        colClient.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                 
                String initials = getInitials(item);
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(10);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label ava = new Label(initials);
                String colorClass = pickAvatarColor(item);
                ava.getStyleClass().addAll("c-ava", colorClass);
                Label name = new Label(item);
                name.getStyleClass().add("td-normal");
                box.getChildren().addAll(ava, name);
                setGraphic(box);
                setText(null);
            }
        });
        colTour.setCellValueFactory(c -> {
            Tour t = tourCache.get(c.getValue().getTourId());
            return new SimpleStringProperty(t != null ? t.getName() : "—");
        });
        colTour.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#0d2010; -fx-font-size:13px;");
            }
        });
        colPax.setCellValueFactory(c ->
              new SimpleStringProperty(String.valueOf(c.getValue().getTouristCount())));
        colPax.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER_LEFT);
                setStyle("-fx-text-fill:#0d2010; -fx-font-size:13px;");
            }
        });
        colDates.setCellValueFactory(c -> {
            Tour t = tourCache.get(c.getValue().getTourId());
            return new SimpleStringProperty(t != null
                  ? t.getStartDate() + "–" + t.getEndDate() : "—");
        });
        colDates.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#4a5a46; -fx-font-size:13px;");
            }
        });
        colCost.setCellValueFactory(c -> {
            var p = c.getValue().getTotalPrice();
            return new SimpleStringProperty(p != null ? ProfilePanelController.CurrencySession.format(p) : "—");
        });
        colCost.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-font-family:'Syne'; -fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#27500a;");
            }
        });
        colStatus.setCellValueFactory(c ->
              new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                setGraphic(pillLabel(item));
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    private static String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private static String pickAvatarColor(String name) {
        int h = Math.abs(name.hashCode()) % 4;
        return switch (h) {
            case 0 -> "ca-a";
            case 1 -> "ca-b";
            case 2 -> "ca-c";
            default -> "ca-d";
        };
    }

     
    private void loadData() {
        com.touroperator.ui.util.AsyncDataLoader.load(
              () -> {
                   
                  if (currentRole == UserRole.CLIENT && currentEmail != null && !currentEmail.isBlank()) {
                       
                      return bookingService.findByClientEmail(currentEmail);
                  } else {
                      return bookingService.findAll();
                  }
              },
              bookings -> {
                   
                  allBookings = FXCollections.observableArrayList(bookings);
                  bookingsTable.setItems(allBookings);
                  updateStats();
                  updateActionButtons(null);
                  bookingsTable.getSelectionModel().clearSelection();
                   
                  if (currentRole == UserRole.CLIENT) buildClientCards();
              },
              errorMsg -> VoyaAlert.error(errorMsg)
        );
    }

    /** Будує картки бронювань для клієнта */
    private void buildClientCards() {
        if (clientCardsContainer == null) return;
        clientCardsContainer.getChildren().clear();

        List<Booking> list = allBookings.stream()
              .filter(b -> applyCurrentFilter(b))
              .toList();

        boolean empty = list.isEmpty();
        if (emptyState != null) { emptyState.setVisible(empty); emptyState.setManaged(empty); }
        if (bookingCountLabelClient != null) {
            bookingCountLabelClient.setText(empty ? "" :
                  list.size() + (list.size() == 1 ? " бронювання" : " бронювань"));
        }

        for (Booking b : list) {
            Tour   tour   = tourCache.get(b.getTourId());
            String tourName = tour != null ? tour.getName() : "Тур";
            String dates = tour != null && tour.getStartDate() != null
                  ? tour.getStartDate() + " – " + tour.getEndDate()
                  : b.getBookingDate() != null ? b.getBookingDate().toString() : "—";
            String cost = ProfilePanelController.CurrencySession.format(
                  b.getTotalPrice() != null ? b.getTotalPrice() : java.math.BigDecimal.ZERO);

             
            String statusText  = statusLabel(b.getStatus());
            String statusColor = statusColor(b.getStatus());
            boolean canCancel = "CREATED".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus());
            boolean canPay    = "CONFIRMED".equals(b.getStatus());
            boolean canRefund = "PAID".equals(b.getStatus());

             
            javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
            card.setStyle("-fx-background-color:white; -fx-background-radius:16;" +
                  "-fx-border-color:#e8f0e0; -fx-border-radius:16;" +
                  "-fx-padding:18 20; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

             
            javafx.scene.layout.HBox row1 = new javafx.scene.layout.HBox(10);
            row1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label nameLbl = new Label("✈  " + tourName);
            nameLbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1a3d08;");
            javafx.scene.layout.HBox.setHgrow(nameLbl, javafx.scene.layout.Priority.ALWAYS);
            Label statusLbl = new Label(statusText);
            statusLbl.setStyle("-fx-background-color:" + statusColor + "22;" +
                  "-fx-text-fill:" + statusColor + "; -fx-background-radius:20;" +
                  "-fx-padding:3 10; -fx-font-size:11px; -fx-font-weight:bold;");
            row1.getChildren().addAll(nameLbl, statusLbl);

             
            javafx.scene.layout.HBox row2 = new javafx.scene.layout.HBox(20);
            row2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label datesLbl = new Label("📅 " + dates);
            datesLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#6a7a5a;");
            Label paxLbl = new Label("👤 " + b.getTouristCount() + " туристів");
            paxLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#6a7a5a;");
            javafx.scene.layout.HBox.setHgrow(paxLbl, javafx.scene.layout.Priority.ALWAYS);
            Label costLbl = new Label(cost);
            costLbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#2d6b1a;");
            row2.getChildren().addAll(datesLbl, paxLbl, costLbl);

             
            javafx.scene.layout.HBox row3 = new javafx.scene.layout.HBox(10);
            row3.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label idLbl = new Label("№ " + b.getId().toString().substring(0, 8).toUpperCase());
            idLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#aab8a0;");
            javafx.scene.layout.HBox.setHgrow(idLbl, javafx.scene.layout.Priority.ALWAYS);

            if (canPay) {
                 
                Button liqpayBtn = new Button("🔗  Сплатити через LiqPay");
                liqpayBtn.setStyle("-fx-background-color: linear-gradient(to right,#1a5276,#2e86c1);" +
                      "-fx-text-fill:white; -fx-border-radius:8; -fx-background-radius:8;" +
                      "-fx-font-size:12px; -fx-font-weight:bold; -fx-padding:5 16; -fx-cursor:hand;");
                liqpayBtn.setOnMouseEntered(ev -> liqpayBtn.setStyle(
                      "-fx-background-color: linear-gradient(to right,#154060,#1a5a90);" +
                            "-fx-text-fill:white; -fx-border-radius:8; -fx-background-radius:8;" +
                            "-fx-font-size:12px; -fx-font-weight:bold; -fx-padding:5 16; -fx-cursor:hand;"));
                liqpayBtn.setOnMouseExited(ev -> liqpayBtn.setStyle(
                      "-fx-background-color: linear-gradient(to right,#1a5276,#2e86c1);" +
                            "-fx-text-fill:white; -fx-border-radius:8; -fx-background-radius:8;" +
                            "-fx-font-size:12px; -fx-font-weight:bold; -fx-padding:5 16; -fx-cursor:hand;"));
                liqpayBtn.setOnAction(e -> onClientPayLiqPay(b));

                Button cancelBtn = new Button("✕  Скасувати");
                cancelBtn.setStyle("-fx-background-color:transparent; -fx-border-color:#e05050;" +
                      "-fx-border-radius:8; -fx-background-radius:8;" +
                      "-fx-text-fill:#e05050; -fx-font-size:12px; -fx-padding:5 14; -fx-cursor:hand;");
                cancelBtn.setOnAction(e -> onClientCancelBooking(b));
                row3.getChildren().addAll(idLbl, cancelBtn, liqpayBtn);

            } else if (canCancel) {
                Button cancelBtn = new Button("✕  Скасувати");
                cancelBtn.setStyle("-fx-background-color:transparent; -fx-border-color:#e05050;" +
                      "-fx-border-radius:8; -fx-background-radius:8;" +
                      "-fx-text-fill:#e05050; -fx-font-size:12px; -fx-padding:5 14; -fx-cursor:hand;");
                cancelBtn.setOnAction(e -> onClientCancelBooking(b));
                row3.getChildren().addAll(idLbl, cancelBtn);
            } else if (canRefund) {
                Button refundBtn = new Button("↩  Повернути кошти");
                refundBtn.setStyle("-fx-background-color:transparent; -fx-border-color:#e08030;" +
                      "-fx-border-radius:8; -fx-background-radius:8;" +
                      "-fx-text-fill:#e08030; -fx-font-size:12px; -fx-padding:5 14; -fx-cursor:hand;");
                refundBtn.setOnAction(e -> onClientRefund(b));
                row3.getChildren().addAll(idLbl, refundBtn);
            } else {
                row3.getChildren().add(idLbl);
            }

            card.getChildren().addAll(row1, row2, row3);
            clientCardsContainer.getChildren().add(card);
        }

         
    }

    /**
     * Будує блок рекомендованих турів у клієнтському вигляді.
     * Логіка: показуємо активні тури (ACTIVE), яких клієнт ще не бронював,
     * відсортовані за заповненістю (популярністю) — не більше 3 штук.
     */
    private void buildTourRecommendations() {
        if (clientCardsContainer == null) return;

         
        java.util.Set<UUID> bookedTourIds = allBookings.stream()
              .map(Booking::getTourId)
              .collect(java.util.stream.Collectors.toSet());

        List<Tour> recommended = tourCache.values().stream()
              .filter(t -> t.getStatus() != null &&
                    "ACTIVE".equals(t.getStatus().name()))
              .filter(t -> !bookedTourIds.contains(t.getId()))
              .filter(t -> t.getAvailableSeats() > 0)
              .sorted((a, b2) -> Double.compare(b2.getFillRate(), a.getFillRate()))
              .limit(3)
              .toList();

        if (recommended.isEmpty()) return;

         
        javafx.scene.layout.VBox section = new javafx.scene.layout.VBox(12);
        section.setStyle("-fx-padding:20 0 0 0;");

        Label sectionTitle = new Label("🌍  Рекомендовані тури для вас");
        sectionTitle.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a3d08;" +
              " -fx-font-family:'Syne';");
        Label sectionSub = new Label("Популярні напрямки, які вас можуть зацікавити");
        sectionSub.setStyle("-fx-font-size:12px; -fx-text-fill:#6a8a5a;");

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color:#e8f0e0;");

        section.getChildren().addAll(sep, sectionTitle, sectionSub);

        for (Tour t : recommended) {
            javafx.scene.layout.VBox rec = buildRecommendationCard(t);
            section.getChildren().add(rec);
        }

        clientCardsContainer.getChildren().add(section);
    }

    private javafx.scene.layout.VBox buildRecommendationCard(Tour tour) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(8);
        card.setStyle("-fx-background-color:#f4fbee; -fx-background-radius:14;" +
              "-fx-border-color:#c8e0b0; -fx-border-radius:14; -fx-border-width:1.5;" +
              "-fx-padding:16 18; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);" +
              "-fx-cursor:default;");

         
        javafx.scene.layout.HBox row1 = new javafx.scene.layout.HBox(8);
        row1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label flag = new Label(countryFlag(tour.getCountry()));
        flag.setStyle("-fx-font-size:20px;");

        javafx.scene.layout.VBox nameBox = new javafx.scene.layout.VBox(2);
        javafx.scene.layout.HBox.setHgrow(nameBox, javafx.scene.layout.Priority.ALWAYS);
        Label nameLbl = new Label(tour.getName());
        nameLbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1a3d08;");
        Label locationLbl = new Label(tour.getCountry() + (tour.getCity() != null ? ", " + tour.getCity() : ""));
        locationLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#7a9a6a;");
        nameBox.getChildren().addAll(nameLbl, locationLbl);

        Label priceLbl = new Label(ProfilePanelController.CurrencySession.format(tour.getBasePrice()));
        priceLbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#27500a;" +
              " -fx-font-family:'Syne';");

        row1.getChildren().addAll(flag, nameBox, priceLbl);

         
        javafx.scene.layout.HBox row2 = new javafx.scene.layout.HBox(14);
        row2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label datesLbl = new Label(tour.getStartDate() != null
              ? "📅 " + tour.getStartDate() + " – " + tour.getEndDate()
              : "📅 Дати уточнюються");
        datesLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#6a7a5a;");

        int seats = tour.getAvailableSeats();
        Label seatsLbl = new Label("👤 Місць: " + seats);
        seatsLbl.setStyle("-fx-font-size:11px; -fx-text-fill:" +
              (seats <= 3 ? "#c05000" : "#4a6a3a") + ";");
        javafx.scene.layout.HBox.setHgrow(seatsLbl, javafx.scene.layout.Priority.ALWAYS);

         
        String badge = tour.getFillRate() >= 0.8 ? "🔥 Популярне"
              : tour.getFillRate() >= 0.5 ? "⭐ Рекомендовано"
                    : "✨ Новинка";
        String badgeColor = tour.getFillRate() >= 0.8 ? "#c06000" : "#2d7020";
        Label badgeLbl = new Label(badge);
        badgeLbl.setStyle("-fx-background-color:" + badgeColor + "22; -fx-text-fill:" + badgeColor + ";" +
              " -fx-background-radius:20; -fx-padding:2 8; -fx-font-size:10px; -fx-font-weight:bold;");

        row2.getChildren().addAll(datesLbl, seatsLbl, badgeLbl);

         
        javafx.scene.layout.HBox row3 = new javafx.scene.layout.HBox();
        row3.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button bookBtn = new Button("Забронювати →");
        bookBtn.setStyle("-fx-background-color:#27500a; -fx-text-fill:#eaf3de;" +
              " -fx-background-radius:10; -fx-padding:6 18; -fx-font-size:12px;" +
              " -fx-font-weight:bold; -fx-cursor:hand;");
        bookBtn.setOnMouseEntered(e -> bookBtn.setStyle("-fx-background-color:#173404; -fx-text-fill:#eaf3de;" +
              " -fx-background-radius:10; -fx-padding:6 18; -fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:hand;"));
        bookBtn.setOnMouseExited(e -> bookBtn.setStyle("-fx-background-color:#27500a; -fx-text-fill:#eaf3de;" +
              " -fx-background-radius:10; -fx-padding:6 18; -fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:hand;"));
         
        bookBtn.setOnAction(e -> openNewBookingForTour(tour));
        row3.getChildren().add(bookBtn);

        card.getChildren().addAll(row1, row2, row3);
        return card;
    }

    /** Відкриває діалог нового бронювання з передвибраним туром */
    private void openNewBookingForTour(Tour tour) {
        try {
            var loader = new javafx.fxml.FXMLLoader(
                  getClass().getResource("/ui/NewBookingDialog.fxml"));
            javafx.scene.layout.StackPane overlay = loader.load();
            var stage = new javafx.stage.Stage(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            var scene = new javafx.scene.Scene(overlay, 600, 580);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(
                  getClass().getResource("/css/style.css").toExternalForm());
            NewBookingController ctrl = loader.getController();
            ctrl.setClientContext(currentRole, currentEmail);
            ctrl.preselectTour(tour);
            ctrl.setOnSaved(this::loadData);
            ctrl.setOnClose(stage::close);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            VoyaAlert.error("Не вдалося відкрити форму: " + e.getMessage());
        }
    }

    /** Прапорець країни — делегуємо до UiUtils */
    private static String countryFlag(String country) {
        return UiUtils.countryFlag(country);
    }

    /** Клієнт скасовує своє бронювання */
    private void onClientCancelBooking(Booking booking) {
        VoyaAlert.confirm(
              "Скасувати бронювання?\n" +
                    "Якщо ви вже сплатили — кошти буде повернуто протягом 3–5 робочих днів.",
              () -> {
                  try {
                      bookingService.cancelBooking(booking.getId(), "Скасовано клієнтом");
                      VoyaAlert.success("Бронювання скасовано.\nКошти буде повернуто протягом 3–5 робочих днів.");
                      loadData();
                  } catch (Exception e) {
                      VoyaAlert.error("Не вдалося скасувати: " + e.getMessage());
                  }
              }
        );
    }

    /** Клієнт запитує повернення коштів за оплачене бронювання */
    private void onClientRefund(Booking booking) {
        Tour tour = tourCache.get(booking.getTourId());
        String tourName = tour != null ? tour.getName() : "тур";
        String cost = ProfilePanelController.CurrencySession.format(
              booking.getTotalPrice() != null ? booking.getTotalPrice() : java.math.BigDecimal.ZERO);
        VoyaAlert.confirm(
              "Повернути кошти за «" + tourName + "»?\n" +
                    "Сума: " + cost + "\n\n" +
                    "Бронювання буде скасовано, кошти повернуто\nпротягом 3–5 робочих днів на ваш рахунок.",
              () -> {
                  try {
                      bookingService.cancelBooking(booking.getId(), "Повернення коштів за запитом клієнта");
                      VoyaAlert.success("✅ Запит на повернення прийнято!\n" +
                            "Кошти (" + cost + ") буде повернуто\nпротягом 3–5 робочих днів.");
                      loadData();
                  } catch (Exception e) {
                      VoyaAlert.error("Не вдалося оформити повернення: " + e.getMessage());
                  }
              }
        );
    }

    /** Клієнт оплачує підтверджене бронювання */
    private void onClientPay(Booking booking) {
        Tour tour = tourCache.get(booking.getTourId());
        String tourName = tour != null ? tour.getName() : "тур";
        String cost = ProfilePanelController.CurrencySession.format(
              booking.getTotalPrice() != null ? booking.getTotalPrice() : java.math.BigDecimal.ZERO);
        VoyaAlert.confirm(
              "Оплатити бронювання?\n\n" +
                    "Тур: " + tourName + "\n" +
                    "Сума до сплати: " + cost,
              () -> {
                  try {
                      bookingService.payBooking(booking.getId(), "ONLINE");
                      VoyaAlert.success("✅ Оплату успішно здійснено!\n" +
                            "Тур «" + tourName + "» оплачено на суму " + cost + ".\n" +
                            "Деталі надіслано на вашу пошту.");
                      loadData();
                  } catch (Exception e) {
                      VoyaAlert.error("Помилка оплати: " + e.getMessage());
                  }
              }
        );
    }

    /** Клієнт оплачує підтверджене бронювання через LiqPay */
    private void onClientPayLiqPay(Booking booking) {
        Tour tour = tourCache.get(booking.getTourId());
        String tourName = tour != null ? tour.getName() : "тур";
        java.math.BigDecimal amount = booking.getTotalPrice();
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            VoyaAlert.error("Сума бронювання некоректна");
            return;
        }
        try {
            com.touroperator.service.LiqPayService liqPay =
                  com.touroperator.config.SpringContext.getBean(com.touroperator.service.LiqPayService.class);
            String orderId = booking.getId().toString().replace("-", "");
            String description = "AYVO · Тур «" + tourName + "»";
            String url = liqPay.generatePaymentUrl(amount, "USD", description, orderId);
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));

             
            com.touroperator.repository.PaymentRepository payRepo =
                  com.touroperator.config.SpringContext.getBean(com.touroperator.repository.PaymentRepository.class);
            com.touroperator.repository.BookingRepository bookRepo =
                  com.touroperator.config.SpringContext.getBean(com.touroperator.repository.BookingRepository.class);
            boolean hasPayment = payRepo.findByBookingId(booking.getId()).isPresent();
            if (hasPayment) {
                payRepo.confirmByBookingId(booking.getId());
            } else {
                com.touroperator.service.PaymentService payService =
                      com.touroperator.config.SpringContext.getBean(com.touroperator.service.PaymentService.class);
                payService.pay(booking.getId(), "LIQPAY");
            }
            bookRepo.markPaid(booking.getId());

             
            loadData();
            try {
                Object ud = bookingsTable != null
                      ? bookingsTable.getScene().getUserData()
                      : null;
                if (ud instanceof MainController mc) {
                    mc.invalidatePage("payments");
                    mc.invalidatePage("dashboard");
                }
            } catch (Exception ignored) {}

            javafx.application.Platform.runLater(() ->
                  VoyaAlert.success(
                        "✅ Оплату через LiqPay прийнято!\nСтатус бронювання: Сплачено."));
        } catch (Exception ex) {
            ex.printStackTrace();
            VoyaAlert.error("Помилка LiqPay: " + ex.getMessage());
        }
    }



    private void startLiqPayPolling(java.util.UUID bookingId) {
        java.util.concurrent.ScheduledExecutorService scheduler =
              java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                  Thread t = new Thread(r, "liqpay-poll");
                  t.setDaemon(true);
                  return t;
              });
        final int[] attempts = {0};
        final int maxAttempts = 60;
        scheduler.scheduleAtFixedRate(() -> {
            attempts[0]++;
            try {
                com.touroperator.repository.BookingRepository bRepo =
                      com.touroperator.config.SpringContext.getBean(com.touroperator.repository.BookingRepository.class);
                com.touroperator.domain.Booking b = bRepo.findById(bookingId).orElse(null);
                boolean paid = b != null && "PAID".equals(b.getStatus());
                if (paid || attempts[0] >= maxAttempts) {
                    scheduler.shutdown();
                    if (paid) {
                        javafx.application.Platform.runLater(() -> {
                            loadData();
                            VoyaAlert.success("✅ Оплату через LiqPay підтверджено!");
                        });
                    }
                }
            } catch (Exception ignored) {
                if (attempts[0] >= maxAttempts) scheduler.shutdown();
            }
        }, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    private boolean applyCurrentFilter(Booking b) {
        String f = statusFilter != null ? statusFilter.getValue() : null;
        if (f == null || f.equals("Всі статуси") || f.equals("Всі статуси")) return true;
        return f.equals(b.getStatus());
    }

    private String statusLabel(String s) {
        if (s == null) return "Невідомо";
        return switch (s) {
            case "CREATED"   -> "Нове";
            case "CONFIRMED" -> "Підтверджено";
            case "PAID"      -> "Сплачено";
            case "COMPLETED" -> "Завершено";
            case "CANCELLED" -> "Скасовано";
            default -> s;
        };
    }

    private String statusColor(String s) {
        if (s == null) return "#8a9a7a";
        return switch (s) {
            case "NEW"       -> "#e0a020";
            case "CONFIRMED" -> "#2d8a3a";
            case "PAID"      -> "#1a6aaa";
            case "COMPLETED" -> "#6a6aaa";
            case "CANCELLED" -> "#e05050";
            default -> "#8a9a7a";
        };
    }

    private void updateStats() {
        long total     = allBookings.size();
        long confirmed = allBookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        long paid      = allBookings.stream().filter(b -> "PAID".equals(b.getStatus())).count();
        long cancelled = allBookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        long pending   = allBookings.stream().filter(b -> "NEW".equals(b.getStatus())).count();

        if (bookingCountLabel != null) {
            if (currentRole == UserRole.CLIENT) {
                bookingCountLabel.setText(total == 0
                      ? "У вас ще немає бронювань"
                      : total + " бронювань · " + paid + " сплачено");
            } else {
                bookingCountLabel.setText(total + " всього · " + pending + " очікують підтвердження");
            }
        }
        if (statTotal     != null) statTotal.setText(String.valueOf(total));
        if (statConfirmed != null) statConfirmed.setText(String.valueOf(confirmed));
        if (statPaid      != null) statPaid.setText(String.valueOf(paid));
        if (statCancelled != null) statCancelled.setText(String.valueOf(cancelled));
    }

     

    @FXML private void onNewBooking() {
        try {
            var loader = new javafx.fxml.FXMLLoader(
                  getClass().getResource("/ui/NewBookingDialog.fxml"));
            javafx.scene.layout.StackPane overlay = loader.load();
            var stage = new javafx.stage.Stage(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            var scene = new javafx.scene.Scene(overlay, 600, 580);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(
                  getClass().getResource("/css/style.css").toExternalForm());
            NewBookingController ctrl = loader.getController();
            ctrl.setClientContext(currentRole, currentEmail);
            ctrl.setOnSaved(this::loadData);
            ctrl.setOnClose(stage::close);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void onStatusFilter() { applyFilters(); }
    @FXML private void onTourFilter()   { applyFilters(); }

    @FXML private void onGoToCatalog() {
        try {
            javafx.scene.Node node = clientCardsContainer;
            if (node != null && node.getScene() != null) {
                Object userData = node.getScene().getUserData();
                if (userData instanceof MainController mc) {
                    mc.showPage("tours");
                }
            }
        } catch (Exception ignored) {}
    }

    private void applyFilters() {
        String status   = statusFilter.getValue();
        String tourName = tourFilter.getValue();
        ObservableList<Booking> filtered = FXCollections.observableArrayList();
        for (Booking b : allBookings) {
            boolean statusOk = "Всі статуси".equals(status) || status.equals(b.getStatus());
            Tour t = tourCache.get(b.getTourId());
            boolean tourOk   = "Всі тури".equals(tourName)  ||
                  (t != null && t.getName().equals(tourName));
            if (statusOk && tourOk) filtered.add(b);
        }
        bookingsTable.setItems(filtered);
    }

     
    private com.touroperator.command.CommandManager commandManager;

    private com.touroperator.command.CommandManager getCommandManager() {
        if (commandManager == null)
            commandManager = SpringContext.getBean(com.touroperator.command.CommandManager.class);
        return commandManager;
    }

     
    @FXML private void onConfirmBooking() {
        Booking sel = selected();
        if (sel == null) return;
        try {
            String label = "#" + sel.getId().toString().substring(0, 8);
            getCommandManager().execute(new com.touroperator.command.BookingCommands.ConfirmBookingCommand(
                  bookingService, sel.getId(), label));
            VoyaAlert.success("Бронювання підтверджено!");
            loadData();
        } catch (Exception e) { VoyaAlert.error(e.getMessage()); }
    }

     
    @FXML private void onCompleteBooking() {
        Booking sel = selected();
        if (sel == null) return;
        try {
            String label = "#" + sel.getId().toString().substring(0, 8);
            getCommandManager().execute(new com.touroperator.command.BookingCommands.CompleteBookingCommand(
                  bookingService, sel.getId(), label));
            VoyaAlert.success("Бронювання завершено!");
            loadData();
        } catch (Exception e) { VoyaAlert.error(e.getMessage()); }
    }

     
    @FXML private void onCancelBooking() {
        Booking sel = selected();
        if (sel == null) return;
        try {
            String label = "#" + sel.getId().toString().substring(0, 8);
            getCommandManager().execute(new com.touroperator.command.BookingCommands.CancelBookingCommand(
                  bookingService, sel.getId(), "Скасовано менеджером",
                  sel.getStatus(), label));
            VoyaAlert.success("Бронювання скасовано.");
            loadData();
        } catch (Exception e) { VoyaAlert.error(e.getMessage()); }
    }

     
    private void showAuditPanel(Booking booking) {
        if (auditPanel == null || auditTimeline == null || auditService == null) return;
        auditTimeline.getChildren().clear();
        String shortId = booking.getId().toString().substring(0, 8).toUpperCase();
        if (auditTitle != null) auditTitle.setText("Історія змін · BK-" + shortId);
        try {
            var history = auditService.getHistory(booking.getId());
            if (history.isEmpty()) {
                Label empty = new Label("Записів ще немає — нові дії будуть відображатись тут");
                empty.setStyle("-fx-font-size:12px; -fx-text-fill:#9aaa8a; -fx-padding:6 0;");
                auditTimeline.getChildren().add(empty);
            } else {
                for (var row : history) {
                    auditTimeline.getChildren().add(buildAuditRow(row));
                }
            }
        } catch (Exception e) {
            Label err = new Label("Не вдалося завантажити аудит");
            err.setStyle("-fx-font-size:12px; -fx-text-fill:#c05050; -fx-padding:6 0;");
            auditTimeline.getChildren().add(err);
        }
        auditPanel.setVisible(true);
        auditPanel.setManaged(true);
    }

    private javafx.scene.layout.HBox buildAuditRow(java.util.Map<String, Object> row) {
        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setStyle("-fx-padding:5 0;");

         
        String action = String.valueOf(row.getOrDefault("action", ""));
        String icon = switch (action) {
            case "CREATE"   -> "🆕";
            case "CONFIRM"  -> "✅";
            case "PAY"      -> "💳";
            case "COMPLETE" -> "🏁";
            case "CANCEL"   -> "❌";
            default -> "📌";
        };
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:14px; -fx-min-width:24;");

         
        String oldSt  = row.get("old_status")  != null ? statusLabel(String.valueOf(row.get("old_status"))) : "—";
        String newSt  = row.get("new_status")  != null ? statusLabel(String.valueOf(row.get("new_status"))) : "—";
        String who    = String.valueOf(row.getOrDefault("changed_by", "SYSTEM"));
        String detail = row.get("details") != null ? " · " + row.get("details") : "";
        Label textLbl = new Label(oldSt + " → " + newSt + " (" + who + ")" + detail);
        textLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#2d4a1a;");
        javafx.scene.layout.HBox.setHgrow(textLbl, javafx.scene.layout.Priority.ALWAYS);

         
        Object ts = row.get("changed_at");
        String timeStr = ts != null ? ts.toString().substring(0, 16).replace("T", " ") : "";
        Label timeLbl = new Label(timeStr);
        timeLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#9aaa8a;");

        hbox.getChildren().addAll(iconLbl, textLbl, timeLbl);
        return hbox;
    }

    private void hideAuditPanel() {
        if (auditPanel == null) return;
        auditPanel.setVisible(false);
        auditPanel.setManaged(false);
    }

    @FXML private void onCloseAudit() { hideAuditPanel(); }

    /** Ctrl+Z — відмінити останню дію */
    @FXML public void onUndo() {
        try {
            getCommandManager().undo();
            VoyaAlert.success("Дію відмінено.");
            loadData();
        } catch (UnsupportedOperationException e) {
            VoyaAlert.warning(e.getMessage());
        } catch (Exception e) {
            VoyaAlert.error("Помилка відміни: " + e.getMessage());
        }
    }

    /** Ctrl+Y — повторити відмінену дію */
    @FXML public void onRedo() {
        try {
            getCommandManager().redo();
            VoyaAlert.success("Дію повторено.");
            loadData();
        } catch (Exception e) {
            VoyaAlert.error("Помилка повтору: " + e.getMessage());
        }
    }

    private Booking selected() {
        Booking sel = bookingsTable.getSelectionModel().getSelectedItem();
        if (sel == null) VoyaAlert.warning("Оберіть бронювання у таблиці.");
        return sel;
    }

    private void toggleDetail(Booking b) {
        if (detailPanel == null) return;
        boolean visible = detailPanel.isVisible();
        if (!visible) {
            Client c = clientCache.get(b.getClientId());
            Tour   t = tourCache.get(b.getTourId());
            if (detailTitle != null)
                detailTitle.setText("Бронювання #" + b.getId().toString().substring(0, 8));
            if (detailSub != null)
                detailSub.setText((c != null ? c.getName() : "—") +
                      " · " + (t != null ? t.getName() : "—"));
        }
        detailPanel.setVisible(!visible);
        detailPanel.setManaged(!visible);
    }

    static Label pillLabel(String status) {
        Label l = new Label();
        switch (status) {
            case "CREATED"    -> { l.setText("Нове");         l.getStyleClass().add("pill-pending");   }
            case "CONFIRMED"  -> { l.setText("Підтверджено"); l.getStyleClass().add("pill-confirmed"); }
            case "PAID"       -> { l.setText("Сплачено");     l.getStyleClass().add("pill-paid");      }
            case "COMPLETED"  -> { l.setText("Завершено");    l.getStyleClass().add("pill-confirmed"); }
            case "CANCELLED"  -> { l.setText("Скасовано");    l.getStyleClass().add("pill-cancelled"); }
            default           ->   l.setText(status);
        }
        return l;
    }
}