package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.Client;
import com.touroperator.repository.ClientRepository;
import com.touroperator.service.ClientService;
import com.touroperator.service.EmailService;
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
import java.security.SecureRandom;

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

     
    @FXML private VBox      panelVerify;
    @FXML private TextField verifyCodeField;
    @FXML private Label     verifyHint;

    private Stage stage;
    private boolean showingLogin = true;
    /** Email клієнта, що очікує підтвердження */
    private String pendingVerifyEmail;

    private static final String[][] DEMO_USERS = {
          { "admin@ayvo.ua", "admin123", "ADMIN" },
    };

    public void setStage(Stage stage) { this.stage = stage; }

    @FXML
    public void initialize() {
        loginPassword.setOnAction(e -> onLogin());
        regPassword.setOnAction(e -> { if (regPasswordConfirm != null) regPasswordConfirm.requestFocus(); });
        if (regPasswordConfirm != null) regPasswordConfirm.setOnAction(e -> onRegister());
        if (verifyCodeField != null)    verifyCodeField.setOnAction(e -> onVerifyCode());
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

         
        try {
            ClientRepository clientRepo = SpringContext.getBean(ClientRepository.class);
            clientRepo.findByEmail(email).ifPresent(c -> {
                if (!c.isEmailVerified()) {
                    throw new RuntimeException("EMAIL_NOT_VERIFIED");
                }
            });
        } catch (RuntimeException ex) {
            if ("EMAIL_NOT_VERIFIED".equals(ex.getMessage())) {
                showError("❌ Email не підтверджено. Перевірте пошту і введіть код.");
                return;
            }
        }

        UserRole role = resolveRole(email, pass);
        if (role == null) { shakeError("Неправильний email або пароль"); return; }
        openMainApp(email, role);
    }

    @FXML private void onRegister() {
        String name  = regName.getText().trim();
        String email = regEmail.getText().trim();
        String phone = regPhone != null ? regPhone.getText().trim() : "";
        String pass  = regPassword.getText();

         
        if (name.isBlank()) { setRegHint("❌ Введіть ім'я та прізвище", "#c03030"); return; }
        if (name.length() < 2) { setRegHint("❌ Ім'я занадто коротке", "#c03030"); return; }

         
        if (email.isBlank()) { setRegHint("❌ Введіть email-адресу", "#c03030"); return; }
        if (!isValidEmail(email)) { setRegHint("❌ Невірний формат email (наприклад: user@example.com)", "#c03030"); return; }

         
        if (!phone.isBlank() && !isValidPhone(phone)) {
            setRegHint("❌ Невірний формат телефону (наприклад: +380501234567)", "#c03030"); return;
        }

         
        if (pass.isBlank()) { setRegHint("❌ Введіть пароль", "#c03030"); return; }
        if (pass.length() < 8) { setRegHint("❌ Пароль має бути мінімум 8 символів", "#c03030"); return; }
        if (!hasDigit(pass)) { setRegHint("❌ Пароль має містити хоча б одну цифру", "#c03030"); return; }
        if (!hasLetter(pass)) { setRegHint("❌ Пароль має містити хоча б одну літеру", "#c03030"); return; }

         
        String passConfirm = regPasswordConfirm != null ? regPasswordConfirm.getText() : "";
        if (!pass.equals(passConfirm)) { setRegHint("❌ Паролі не збігаються", "#c03030"); return; }

         
        try {
            ClientService clientService = SpringContext.getBean(ClientService.class);
            ClientRepository clientRepo = SpringContext.getBean(ClientRepository.class);
            EmailService emailService   = SpringContext.getBean(EmailService.class);

            if (clientRepo.findByEmail(email).isPresent()) {
                setRegHint("❌ Цей email вже зареєстрований", "#c03030");
                return;
            }

             
            String verifyToken = generateVerifyToken();

            Client newClient = new Client();
            newClient.setName(name);
            newClient.setEmail(email);
            newClient.setPhone(phone.isBlank() ? null : phone);
            newClient.setPasswordHash(hashPassword(pass));
            newClient.setVerifyToken(verifyToken);
            newClient.setEmailVerified(false);
            clientService.save(newClient);

             
            emailService.sendVerificationEmail(email, name, verifyToken);
            pendingVerifyEmail = email;
        } catch (Exception ex) {
            ex.printStackTrace();
            setRegHint("❌ Помилка збереження: " + ex.getMessage(), "#c03030");
            return;
        }

         
        setRegHint("✓ На вашу пошту надіслано код підтвердження!", "#639922");
        PauseTransition delay = new PauseTransition(Duration.millis(800));
        delay.setOnFinished(e -> Platform.runLater(this::showVerifyPanel));
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

    /** Показує панель введення коду підтвердження замість панелі реєстрації. */
    private void showVerifyPanel() {
        if (panelVerify == null) return;
        switchPanel(panelRegister, panelVerify);
        if (verifyCodeField != null) verifyCodeField.requestFocus();
    }

    /** Обробляє натискання кнопки "Підтвердити" на панелі верифікації. */
    @FXML private void onVerifyCode() {
        String code = verifyCodeField != null ? verifyCodeField.getText().trim() : "";
        if (code.isBlank()) {
            setVerifyHint("❌ Введіть код з листа", "#c03030"); return;
        }
        try {
            ClientRepository clientRepo = SpringContext.getBean(ClientRepository.class);
            boolean ok = clientRepo.verifyEmail(code);
            if (!ok) {
                setVerifyHint("❌ Невірний або вже використаний код", "#c03030"); return;
            }
        } catch (Exception ex) {
            setVerifyHint("❌ Помилка: " + ex.getMessage(), "#c03030"); return;
        }
        setVerifyHint("✅ Email підтверджено! Входимо...", "#639922");
        PauseTransition delay = new PauseTransition(Duration.millis(800));
        delay.setOnFinished(e -> Platform.runLater(() -> openMainApp(pendingVerifyEmail, UserRole.CLIENT)));
        delay.play();
    }

    private void setVerifyHint(String msg, String color) {
        if (verifyHint == null) return;
        verifyHint.setText(msg);
        verifyHint.setStyle("-fx-text-fill: " + color + ";");
        verifyHint.setVisible(true);
        verifyHint.setManaged(true);
    }

    /** Генерує 6-значний цифровий токен підтвердження. */
    private static String generateVerifyToken() {
        SecureRandom rng = new SecureRandom();
        int code = 100_000 + rng.nextInt(900_000); // 100000–999999
        return String.valueOf(code);
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
             
            ProfilePanelController.SessionState.setLastLoginTime(java.time.LocalDateTime.now());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/MainLayout.fxml"));
            javafx.geometry.Rectangle2D screen =
                  javafx.stage.Screen.getPrimary().getVisualBounds();
            double winW = Math.min(1400, screen.getWidth()  * 0.85);
            double winH = Math.min(900,  screen.getHeight() * 0.85);
            Scene scene = new Scene(loader.load(), winW, winH);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            MainController ctrl = loader.getController();
            scene.setUserData(ctrl);
            ctrl.setUser(email, role);
            stage.setScene(scene);
            stage.setTitle("AYVO — " + role.getDisplayName());
            stage.setMinWidth(900);
            stage.setMinHeight(600);
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