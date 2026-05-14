package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Client;
import com.touroperator.service.ClientService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class LoginController {

    @FXML private Button tabLogin;
    @FXML private Button tabRegister;
    @FXML private VBox   panelLogin;
    @FXML private VBox   panelRegister;

    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginError;

    @FXML private TextField     regName;
    @FXML private TextField     regEmail;
    @FXML private TextField     regPhone;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regPasswordConfirm;
    @FXML private Label         regHint;

    private Stage stage;
    private boolean showingLogin = true;

    private static final String[][] DEMO_USERS = {
          { "admin@ayvo.ua", "admin123", "ADMIN" },
    };

    public void setStage(Stage stage) { this.stage = stage; }

    @FXML
    public void initialize() {
        loginPassword.setOnAction(e -> onLogin());
        regPassword.setOnAction(e -> { if (regPasswordConfirm != null) regPasswordConfirm.requestFocus(); });
        if (regPasswordConfirm != null) regPasswordConfirm.setOnAction(e -> onRegister());
    }

    @FXML private void showLogin() {
        if (showingLogin) return;
        showingLogin = true;
        tabLogin.getStyleClass().add("auth-tab-active");
        tabRegister.getStyleClass().remove("auth-tab-active");
        switchPanel(panelRegister, panelLogin);
        if (loginError != null) { loginError.setText(""); loginError.setVisible(false); loginError.setManaged(false); }
    }

    @FXML private void showRegister() {
        if (!showingLogin) return;
        showingLogin = false;
        tabRegister.getStyleClass().add("auth-tab-active");
        tabLogin.getStyleClass().remove("auth-tab-active");
        switchPanel(panelLogin, panelRegister);
    }

    private void switchPanel(VBox hide, VBox show) {
        FadeTransition out = new FadeTransition(Duration.millis(150), hide);
        out.setToValue(0);
        out.setOnFinished(e -> {
            hide.setVisible(false);
            hide.setManaged(false);
            show.setOpacity(0);
            show.setVisible(true);
            show.setManaged(true);
            FadeTransition in = new FadeTransition(Duration.millis(200), show);
            in.setToValue(1);
            in.play();
        });
        out.play();
    }

    @FXML private void onLogin() {
        String email = loginEmail.getText().trim();
        String pass  = loginPassword.getText();
        if (email.isBlank() || pass.isBlank()) { showError("Будь ласка, заповніть всі поля"); return; }
        if (!isValidEmail(email)) { showError("Невірний формат email (наприклад: user@example.com)"); return; }
        if (pass.length() < 6) { showError("Пароль має бути мінімум 6 символів"); return; }
        UserRole role = resolveRole(email, pass);
        if (role == null) { shakeError("Неправильний email або пароль"); return; }
        openMainApp(email, role);
    }

    @FXML private void onRegister() {
        String name  = regName.getText().trim();
        String email = regEmail.getText().trim();
        String phone = regPhone != null ? regPhone.getText().trim() : "";
        String pass  = regPassword.getText();

        // Перевірка ім'я
        if (name.isBlank()) { setRegHint("❌ Введіть ім'я та прізвище", "#c03030"); return; }
        if (name.length() < 2) { setRegHint("❌ Ім'я занадто коротке", "#c03030"); return; }

        // Перевірка email
        if (email.isBlank()) { setRegHint("❌ Введіть email-адресу", "#c03030"); return; }
        if (!isValidEmail(email)) { setRegHint("❌ Невірний формат email (наприклад: user@example.com)", "#c03030"); return; }

        // Перевірка телефону (якщо введено)
        if (!phone.isBlank() && !isValidPhone(phone)) {
            setRegHint("❌ Невірний формат телефону (наприклад: +380501234567)", "#c03030"); return;
        }

        // Перевірка пароля
        if (pass.isBlank()) { setRegHint("❌ Введіть пароль", "#c03030"); return; }
        if (pass.length() < 8) { setRegHint("❌ Пароль має бути мінімум 8 символів", "#c03030"); return; }
        if (!hasDigit(pass)) { setRegHint("❌ Пароль має містити хоча б одну цифру", "#c03030"); return; }
        if (!hasLetter(pass)) { setRegHint("❌ Пароль має містити хоча б одну літеру", "#c03030"); return; }

        // Перевірка підтвердження пароля
        String passConfirm = regPasswordConfirm != null ? regPasswordConfirm.getText() : "";
        if (!pass.equals(passConfirm)) { setRegHint("❌ Паролі не збігаються", "#c03030"); return; }

        // Зберігаємо клієнта в базу даних
        try {
            ClientService clientService = SpringContext.getBean(ClientService.class);
            com.touroperator.repository.ClientRepository clientRepo =
                  SpringContext.getBean(com.touroperator.repository.ClientRepository.class);
            if (clientRepo.findByEmail(email).isPresent()) {
                setRegHint("❌ Цей email вже зареєстрований", "#c03030");
                return;
            }
            Client newClient = new Client();
            newClient.setName(name);
            newClient.setEmail(email);
            newClient.setPhone(phone.isBlank() ? null : phone);
            newClient.setPasswordHash(hashPassword(pass));
            clientService.save(newClient);
        } catch (Exception ex) {
            ex.printStackTrace();
            setRegHint("❌ Помилка збереження: " + ex.getMessage(), "#c03030");
            return;
        }

        setRegHint("✓ Реєстрація успішна! Входимо...", "#639922");
        PauseTransition delay = new PauseTransition(Duration.millis(700));
        delay.setOnFinished(e -> Platform.runLater(() -> openMainApp(email, UserRole.CLIENT)));
        delay.play();
    }

    private void setRegHint(String text, String color) {
        if (regHint == null) return;
        regHint.setText(text);
        regHint.setStyle("-fx-text-fill:" + color + ";");
    }

    private UserRole resolveRole(String email, String pass) {
        for (String[] u : DEMO_USERS)
            if (u[0].equalsIgnoreCase(email) && u[1].equals(pass))
                return UserRole.valueOf(u[2]);
        try {
            com.touroperator.repository.ClientRepository clientRepo =
                  SpringContext.getBean(com.touroperator.repository.ClientRepository.class);
            return clientRepo.findByEmail(email)
                  .filter(c -> c.getPasswordHash() != null &&
                        c.getPasswordHash().equals(hashPassword(pass)))
                  .map(c -> UserRole.CLIENT)
                  .orElse(null);
        } catch (Exception ignored) {}
        return null;
    }

    /** SHA-256 хеш пароля (hex) */
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Помилка хешування пароля", e);
        }
    }

    private void openMainApp(String email, UserRole role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/MainLayout.fxml"));
            Scene scene = new Scene(loader.load(), 1400, 900);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            MainController ctrl = loader.getController();
            scene.setUserData(ctrl);
            ctrl.setUser(email, role);
            stage.setScene(scene);
            stage.setTitle("AYVO — " + role.getDisplayName());
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Помилка: " + ex.getMessage());
        }
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$");
    }

    private static boolean isValidPhone(String phone) {
        // Дозволяємо +38 0XX XXX XX XX або просто 0XXXXXXXXX, з пробілами/дефісами
        String digits = phone.replaceAll("[\\s\\-()]", "");
        return digits.matches("^(\\+?380?|0)\\d{9}$") || digits.matches("^\\+?\\d{10,15}$");
    }

    private static boolean hasDigit(String s) {
        for (char c : s.toCharArray()) if (Character.isDigit(c)) return true;
        return false;
    }

    private static boolean hasLetter(String s) {
        for (char c : s.toCharArray()) if (Character.isLetter(c)) return true;
        return false;
    }

    private void showError(String msg) {
        if (loginError == null) return;
        loginError.setText(msg);
        loginError.setVisible(true);
        loginError.setManaged(true);
    }

    private void shakeError(String msg) {
        showError(msg);
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), panelLogin);
        shake.setByX(10); shake.setCycleCount(6); shake.setAutoReverse(true);
        shake.setOnFinished(e -> panelLogin.setTranslateX(0));
        shake.play();
    }
}