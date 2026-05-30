package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Booking;
import com.touroperator.domain.Client;
import com.touroperator.repository.BookingRepository;
import com.touroperator.service.ClientService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ClientsController {

    @FXML private ComboBox<String> clientFilter;
    @FXML private TableView<Client>          clientsTable;
    @FXML private TableColumn<Client,String> colName;
    @FXML private TableColumn<Client,String> colEmail;
    @FXML private TableColumn<Client,String> colPhone;
    @FXML private TableColumn<Client,String> colBookings;
    @FXML private TableColumn<Client,String> colSpent;
    @FXML private TableColumn<Client,String> colLastTour;
    @FXML private TableColumn<Client,String> colStatus;
    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statLoyal;
    @FXML private Label statLtv;
    @FXML private Label clientCountLabel;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private ClientService     clientService;
    private BookingRepository bookingRepo;
    private ObservableList<Client> allClients;

    private final Map<UUID, List<Booking>> bookingsByClient = new HashMap<>();

    @FXML
    public void initialize() {
        try {
            clientService = SpringContext.getBean(ClientService.class);
            bookingRepo   = SpringContext.getBean(BookingRepository.class);
            buildBookingCache();
            setupColumns();
            setupSelectionListener();
            loadData();
            clientFilter.setItems(FXCollections.observableArrayList(
                  "Всі клієнти", "З бронюваннями", "Без бронювань", "Постійні (3+ броні)"));
            clientFilter.getSelectionModel().selectFirst();

            // Оновлення валюти в реальному часі
            ProfilePanelController.CurrencySession.addListener(this::loadData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Клієнт вважається "Постійним" якщо він бронював 3+ РІЗНИХ тури
     * (унікальних tourId серед усіх бронювань, що не скасовані).
     */
    private boolean isLoyal(UUID clientId) {
        long uniqueTours = bookingsByClient
              .getOrDefault(clientId, List.of()).stream()
              .filter(b -> b.getTourId() != null && !"CANCELLED".equals(b.getStatus()))
              .map(Booking::getTourId)
              .distinct()
              .count();
        return uniqueTours >= 3;
    }

    private void buildBookingCache() {
        bookingsByClient.clear();
        bookingRepo.findAll().forEach(b -> {
            if (b.getClientId() != null)
                bookingsByClient.computeIfAbsent(b.getClientId(), k -> new ArrayList<>()).add(b);
        });
    }

    private void setupSelectionListener() {
        clientsTable.getSelectionModel().selectedItemProperty().addListener(
              (obs, o, n) -> {
                  boolean has = n != null;
                  if (btnEdit   != null) { btnEdit.setDisable(!has);   btnEdit.setOpacity(has ? 1.0 : 0.4); }
                  if (btnDelete != null) { btnDelete.setDisable(!has); btnDelete.setOpacity(has ? 1.0 : 0.4); }
              });
        if (btnEdit   != null) { btnEdit.setDisable(true);   btnEdit.setOpacity(0.4); }
        if (btnDelete != null) { btnDelete.setDisable(true); btnDelete.setOpacity(0.4); }
    }

    private void setupColumns() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colName.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#0d2010; -fx-font-size:13px;");
            }
        });

        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEmail.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#4a5a46; -fx-font-size:13px;");
            }
        });

        colPhone.setCellValueFactory(c -> new SimpleStringProperty(
              c.getValue().getPhone() != null ? c.getValue().getPhone() : "—"));
        colPhone.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#4a5a46; -fx-font-size:13px;");
            }
        });

        if (colBookings != null)
            colBookings.setCellValueFactory(c -> {
                int cnt = bookingsByClient.getOrDefault(c.getValue().getId(), List.of()).size();
                return new SimpleStringProperty(String.valueOf(cnt));
            });

        if (colSpent != null)
            colSpent.setCellValueFactory(c -> {
                BigDecimal total = bookingsByClient
                      .getOrDefault(c.getValue().getId(), List.of()).stream()
                      .filter(b -> "PAID".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus()))
                      .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new SimpleStringProperty(total.compareTo(BigDecimal.ZERO) == 0
                      ? "—" : ProfilePanelController.CurrencySession.format(total));
            });

        if (colLastTour != null)
            colLastTour.setCellValueFactory(c -> {
                List<Booking> bks = bookingsByClient.getOrDefault(c.getValue().getId(), List.of());
                return new SimpleStringProperty(bks.isEmpty() ? "—" :
                      bks.stream()
                            .filter(b -> b.getBookingDate() != null)
                            .max(Comparator.comparing(Booking::getBookingDate))
                            .map(b -> b.getBookingDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                            .orElse("—"));
            });

        if (colStatus != null) {
            colStatus.setCellValueFactory(c -> {
                return new SimpleStringProperty(isLoyal(c.getValue().getId()) ? "loyal" : "active");
            });
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    Label l = new Label();
                    if ("loyal".equals(item)) {
                        l.setText("⭐ Постійний");
                        l.getStyleClass().add("pill-paid");
                    } else {
                        l.setText("Активний");
                        l.getStyleClass().add("pill-confirmed");
                    }
                    setGraphic(l); setText(null); setAlignment(Pos.CENTER_LEFT);
                }
            });
        }
    }

    private void loadData() {
        try {
            allClients = FXCollections.observableArrayList(clientService.findAll());
        } catch (Exception e) {
            e.printStackTrace();
            allClients = FXCollections.observableArrayList();
        }
        clientsTable.setItems(allClients);
        updateStats(allClients);
        if (clientCountLabel != null)
            clientCountLabel.setText(allClients.size() + " клієнтів у базі");
    }

    private void updateStats(List<Client> clients) {
        long total  = clients.size();
        long active = clients.stream()
              .filter(c -> !bookingsByClient.getOrDefault(c.getId(), List.of()).isEmpty())
              .count();
        long loyal  = clients.stream()
              .filter(c -> isLoyal(c.getId()))
              .count();
        BigDecimal totalSpent = bookingsByClient.values().stream()
              .flatMap(List::stream)
              .filter(b -> "PAID".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus()))
              .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
        String ltv = total == 0 ? "—" :
              ProfilePanelController.CurrencySession.format(totalSpent.divide(
                    BigDecimal.valueOf(total), 0, java.math.RoundingMode.HALF_UP));

        if (statTotal  != null) statTotal.setText(String.valueOf(total));
        if (statActive != null) statActive.setText(String.valueOf(active));
        if (statLoyal  != null) statLoyal.setText(String.valueOf(loyal));
        if (statLtv    != null) statLtv.setText(ltv);
    }

    @FXML private void onClientFilter() {
        String f = clientFilter.getValue();
        List<Client> filtered = allClients.stream().filter(c -> {
            int cnt = bookingsByClient.getOrDefault(c.getId(), List.of()).size();
            return switch (f) {
                case "З бронюваннями"    -> cnt > 0;
                case "Без бронювань"     -> cnt == 0;
                case "Постійні (3+ броні)" -> isLoyal(c.getId());
                default                  -> true;
            };
        }).collect(Collectors.toList());
        clientsTable.setItems(FXCollections.observableArrayList(filtered));
        if (clientCountLabel != null) {
            String suffix = "Всі клієнти".equals(f)
                  ? filtered.size() + " клієнтів у базі"
                  : filtered.size() + " клієнтів (фільтр: " + f + ")";
            clientCountLabel.setText(suffix);
        }
    }

    @FXML private void onAddClient()  { openClientDialog(null); }

    @FXML private void onEditClient() {
        Client sel = clientsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { VoyaAlert.warning("Оберіть клієнта у таблиці."); return; }
        openClientDialog(sel);
    }

    @FXML private void onDeleteClient() {
        Client sel = clientsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { VoyaAlert.warning("Оберіть клієнта у таблиці."); return; }
        int bookingCount = bookingsByClient.getOrDefault(sel.getId(), List.of()).size();
        if (bookingCount > 0) {
            VoyaAlert.warning("Неможливо видалити клієнта з " + bookingCount + " бронюваннями.");
            return;
        }
        try {
            clientService.delete(sel.getId());
            VoyaAlert.success("Клієнта \"" + sel.getName() + "\" видалено.");
            buildBookingCache();
            loadData();
        } catch (Exception e) { VoyaAlert.error(e.getMessage()); }
    }

    // ── Покращений діалог ────────────────────────────────────────────────
    private void openClientDialog(Client existing) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(clientsTable.getScene().getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox modal = new VBox(0);
        modal.getStyleClass().add("tour-detail-modal");
        modal.setMaxWidth(500);
        modal.setMinWidth(480);

        // ── Шапка (зелений градієнт як у оплатах/турах) ──────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 22, 18, 22));
        header.setStyle(
              "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #2e5a10, #4a8a1a);" +
                    "-fx-background-radius: 28 28 0 0;"
        );

        VBox titleBlock = new VBox(3);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        boolean isNew = existing == null;
        Label titleLbl = new Label(isNew ? "👤  Новий клієнт" : "✏️  Редагувати клієнта");
        titleLbl.setStyle("-fx-font-family:'Syne';-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label subLbl = new Label(isNew ? "Додати нового клієнта до бази" : "Оновити дані клієнта");
        subLbl.setStyle("-fx-font-size:11.5px;-fx-text-fill:rgba(255,255,255,0.72);");
        titleBlock.getChildren().addAll(titleLbl, subLbl);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("tour-modal-close-btn");
        closeBtn.setOnAction(e -> stage.close());
        header.getChildren().addAll(titleBlock, closeBtn);

        // ── Тіло ──────────────────────────────────────────────────────────
        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 26, 8, 26));

        TextField nameField  = lightField("Повне ім'я *");
        TextField emailField = lightField("Email *");
        TextField phoneField = lightField("Телефон");
        DatePicker birthPicker = new DatePicker();
        birthPicker.setPromptText("Дата народження");
        birthPicker.setMaxWidth(Double.MAX_VALUE);
        birthPicker.setStyle(
              "-fx-background-color:#f4faea;" +
                    "-fx-border-color:rgba(99,153,34,0.30);" +
                    "-fx-border-width:1.5;" +
                    "-fx-border-radius:10;" +
                    "-fx-background-radius:10;" +
                    "-fx-font-size:13px;" +
                    "-fx-pref-height:40px;"
        );

        if (existing != null) {
            nameField.setText(existing.getName());
            emailField.setText(existing.getEmail());
            phoneField.setText(existing.getPhone() != null ? existing.getPhone() : "");
            if (existing.getBirthDate() != null)
                birthPicker.setValue(existing.getBirthDate());
        }

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill:#b83c2a;-fx-font-size:12px;-fx-font-weight:bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        body.getChildren().addAll(
              fieldBlock("ІМ'Я", nameField),
              fieldBlock("EMAIL", emailField),
              fieldBlock("ТЕЛЕФОН", phoneField),
              fieldBlock("ДАТА НАРОДЖЕННЯ", birthPicker),
              errorLabel
        );

        // ── Футер ─────────────────────────────────────────────────────────
        HBox footer = new HBox(10);
        footer.getStyleClass().add("tour-detail-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Скасувати");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = new Button(isNew ? "✚  Додати" : "💾  Зберегти");
        saveBtn.getStyleClass().add("add-btn");
        saveBtn.setOnAction(e -> {
            String name  = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            LocalDate birth = birthPicker.getValue();

            if (name.isEmpty() || email.isEmpty()) {
                errorLabel.setText("⚠ Ім'я та email обов'язкові.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            try {
                Client c = existing != null ? existing : new Client();
                c.setName(name);
                c.setEmail(email);
                c.setPhone(phone.isEmpty() ? null : phone);
                c.setBirthDate(birth);
                clientService.save(c);
                VoyaAlert.success(isNew
                      ? "Клієнта \"" + name + "\" додано!"
                      : "Дані клієнта оновлено!");
                stage.close();
                buildBookingCache();
                loadData();
            } catch (Exception ex) {
                errorLabel.setText("⚠ " + ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });
        footer.getChildren().addAll(cancelBtn, saveBtn);

        modal.getChildren().addAll(header, body, footer);

        StackPane overlay = new StackPane(modal);
        overlay.getStyleClass().add("modal-overlay-pane");
        Scene scene = new Scene(overlay, 540, 480);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    /** Світле поле (як на сторінці оплат) */
    private TextField lightField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle(
              "-fx-background-color:#f4faea;" +
                    "-fx-border-color:rgba(99,153,34,0.30);" +
                    "-fx-border-width:1.5;" +
                    "-fx-border-radius:10;" +
                    "-fx-background-radius:10;" +
                    "-fx-text-fill:#0d2010;" +
                    "-fx-prompt-text-fill:#8a9a85;" +
                    "-fx-padding:10 14;" +
                    "-fx-font-size:13px;" +
                    "-fx-pref-height:40px;"
        );
        return tf;
    }

    private VBox fieldBlock(String label, javafx.scene.Node field) {
        VBox vb = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("tour-detail-section-title");
        vb.getChildren().addAll(lbl, field);
        return vb;
    }
}