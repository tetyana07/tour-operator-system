package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.dto.CurrencyRate;
import com.touroperator.service.CurrencyService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Контролер панелі профілю.
 * Показує реальні дані поточного користувача (з SessionState).
 * Підтримує зміну валюти в реальному часі через CurrencySession.
 */
public class ProfilePanelController {

    @FXML private VBox        profilePanel;
    @FXML private Label       pdAvatar;
    @FXML private Label       pdFullName;
    @FXML private Label       pdRole;
    @FXML private TextField   fieldFirstName;
    @FXML private TextField   fieldLastName;
    @FXML private TextField   fieldEmail;
    @FXML private TextField   fieldPhone;
    @FXML private ComboBox<String> fieldCurrency;

    @FXML private Label previewLabel;

    private Runnable onCloseCallback;
    private Runnable onLogoutCallback;
    private CurrencyRate currentRate;

    public void setOnClose(Runnable r)   { this.onCloseCallback  = r; }
    public void setOnLogout(Runnable r)  { this.onLogoutCallback = r; }

    @FXML
    public void initialize() {
        String displayName = SessionState.getDisplayName();
        String email       = SessionState.getEmail();
        String phone       = SessionState.getPhone();
        String role        = SessionState.getRoleName();

        String firstName = "", lastName = "";
        if (displayName != null && !displayName.isBlank()) {
            String[] parts = displayName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName  = parts.length > 1 ? parts[1].replace(".", "").trim() : "";
        }

        String initials = initials(firstName, lastName);
        if (pdAvatar   != null) pdAvatar.setText(initials);
        if (pdFullName != null) pdFullName.setText(trim(firstName + " " + lastName));
        if (pdRole     != null) pdRole.setText((role != null && !role.isBlank() ? role : "Користувач") + " · AYVO");

        if (fieldFirstName != null) fieldFirstName.setText(firstName);
        if (fieldLastName  != null) fieldLastName.setText(lastName);
        if (fieldEmail     != null) fieldEmail.setText(email != null ? email : "");
        if (fieldPhone     != null) fieldPhone.setText(phone != null ? phone : "");

        // Live-оновлення аватарки та імені при введенні
        if (fieldFirstName != null) fieldFirstName.textProperty().addListener((obs, o, n) -> updateProfilePreview());
        if (fieldLastName  != null) fieldLastName.textProperty().addListener((obs, o, n) -> updateProfilePreview());

        // Ініціалізуємо ComboBox валюти
        if (fieldCurrency != null) {
            fieldCurrency.setItems(FXCollections.observableArrayList(
                  "грн", "$ USD", "€ EUR"
            ));
            fieldCurrency.setValue(CurrencySession.getCurrencyLabel());
        }

        // Завантажуємо курс у фоні
        Thread t = new Thread(() -> {
            try {
                CurrencyService svc = SpringContext.getBean(CurrencyService.class);
                currentRate = svc.getCurrentRates();
                CurrencySession.setRate(currentRate);
                Platform.runLater(this::updatePreview);
            } catch (Exception e) {
                currentRate = CurrencyRate.fallback();
                Platform.runLater(this::updatePreview);
            }
        }, "profile-currency");
        t.setDaemon(true);
        t.start();
    }

    /** Live-оновлення аватарки та імені в шапці попапу при введенні */
    private void updateProfilePreview() {
        String fn = fieldFirstName != null ? fieldFirstName.getText().trim() : "";
        String ln = fieldLastName  != null ? fieldLastName.getText().trim()  : "";
        String init = initials(fn, ln);
        if (pdAvatar   != null) pdAvatar.setText(init.isEmpty() ? "?" : init);
        if (pdFullName != null) pdFullName.setText(trim(fn + " " + ln));
    }

    @FXML
    private void onCurrencyChanged() {
        if (fieldCurrency == null) return;
        String selected = fieldCurrency.getValue();
        if (selected == null) return;
        CurrencySession.setCurrencyLabel(selected);
        updatePreview();
    }

    private void updatePreview() {
        if (previewLabel == null || currentRate == null) return;
        String curr = CurrencySession.getCurrencyLabel();
        // Показуємо приклад: 1 000 UAH у вибраній валюті
        java.math.BigDecimal uahSample = new java.math.BigDecimal("1000");
        // UAH → USD: divide by usdToUah
        // UAH → EUR: UAH / usdToUah * usdToEur
        String preview;
        try {
            java.math.BigDecimal usdToUah = currentRate.getUsdToUah();
            java.math.BigDecimal usdToEur = currentRate.getUsdToEur();
            if (curr.startsWith("$")) {
                java.math.BigDecimal usd = uahSample.divide(usdToUah, 2, java.math.RoundingMode.HALF_UP);
                preview = "₴1 000 ≈ $" + String.format("%,.2f", usd);
            } else if (curr.startsWith("€")) {
                java.math.BigDecimal usd = uahSample.divide(usdToUah, 6, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal eur = usd.multiply(usdToEur).setScale(2, java.math.RoundingMode.HALF_UP);
                preview = "₴1 000 ≈ €" + String.format("%,.2f", eur);
            } else {
                preview = "Гривня — основна валюта";  // грн
            }
        } catch (Exception e) {
            preview = "Курс недоступний";
        }
        previewLabel.setText(preview);
        previewLabel.setVisible(true);
        previewLabel.setManaged(true);
    }

    @FXML
    private void onLogout() {
        // Очищаємо сесію
        SessionState.setDisplayName("");
        SessionState.setEmail("");
        SessionState.setPhone("");
        SessionState.setRoleName("");
        CurrencySession.setCurrencyLabel("грн");

        // Викликаємо callback logout — TopBarController передає mainController.logout()
        if (onLogoutCallback != null) {
            onLogoutCallback.run();
            return;
        }

        // Запасний варіант якщо callback не встановлено (не повинно траплятись)
        if (onCloseCallback != null) onCloseCallback.run();
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) profilePanel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/LoginView.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 1100, 720);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            LoginController ctrl = loader.getController();
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.setTitle("AYVO");
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        if (onCloseCallback != null) onCloseCallback.run();
    }

    @FXML
    private void onSave() {
        String firstName = fieldFirstName != null ? fieldFirstName.getText().trim() : "";
        String lastName  = fieldLastName  != null ? fieldLastName.getText().trim()  : "";
        String email     = fieldEmail     != null ? fieldEmail.getText().trim()     : "";
        String phone     = fieldPhone     != null ? fieldPhone.getText().trim()     : "";

        if (firstName.isEmpty() || lastName.isEmpty()) {
            VoyaAlert.warning("Будь ласка, заповніть ім'я та прізвище.");
            return;
        }
        if (email.isEmpty() || !email.contains("@")) {
            VoyaAlert.warning("Введіть коректну email-адресу.");
            return;
        }

        SessionState.setDisplayName(firstName + " " + lastName);
        SessionState.setEmail(email);
        SessionState.setPhone(phone);

        // Зберігаємо в базу даних
        try {
            com.touroperator.repository.ClientRepository clientRepo =
                  com.touroperator.config.SpringContext.getBean(
                        com.touroperator.repository.ClientRepository.class);
            clientRepo.findByEmail(SessionState.getEmail())
                  .or(() -> clientRepo.findByEmail(email))
                  .ifPresent(c -> {
                      c.setName(firstName + " " + lastName);
                      c.setEmail(email);
                      c.setPhone(phone.isBlank() ? null : phone);
                      clientRepo.update(c);
                  });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SessionState.setEmail(email);

        // Зберігаємо вибрану валюту
        if (fieldCurrency != null && fieldCurrency.getValue() != null) {
            CurrencySession.setCurrencyLabel(fieldCurrency.getValue());
        }

        String initials = initials(firstName, lastName);
        if (pdAvatar   != null) pdAvatar.setText(initials);
        if (pdFullName != null) pdFullName.setText(firstName + " " + lastName);

        // Сповіщаємо TopBar — оновлює аватарку (ініціали) одразу
        SessionState.notifyProfileSaved(firstName + " " + lastName);

        VoyaAlert.success("Профіль оновлено:\n" + firstName + " " + lastName + "  ·  " + email
              + "\nВалюта: " + CurrencySession.getCurrencyLabel());
        if (onCloseCallback != null) onCloseCallback.run();
    }

    private static String initials(String first, String last) {
        String f = (first != null && !first.isEmpty()) ? String.valueOf(first.charAt(0)).toUpperCase() : "";
        String l = (last  != null && !last.isEmpty())  ? String.valueOf(last.charAt(0)).toUpperCase()  : "";
        return f + l;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    // ── Стан сесії ──────────────────────────────────────────────────────────
    public static class SessionState {
        private static String firstName = "";
        private static String lastName  = "";
        private static String email     = "";
        private static String phone     = "";
        private static String roleName  = "";

        /** Слухачі, що спрацьовують після збереження профілю (ім'я/аватарка) */
        private static final java.util.List<java.util.function.Consumer<String>> profileListeners =
              new java.util.ArrayList<>();

        public static void addProfileListener(java.util.function.Consumer<String> l) {
            profileListeners.add(l);
        }
        public static void removeProfileListener(java.util.function.Consumer<String> l) {
            profileListeners.remove(l);
        }
        /** Викликати після збереження профілю з новим displayName */
        public static void notifyProfileSaved(String displayName) {
            for (var l : new java.util.ArrayList<>(profileListeners)) {
                try { l.accept(displayName); } catch (Exception ignored) {}
            }
        }

        public static String getFirstName()         { return firstName; }
        public static void   setFirstName(String v) { firstName = v; }
        public static String getLastName()          { return lastName; }
        public static void   setLastName(String v)  { lastName  = v; }
        public static String getEmail()             { return email; }
        public static void   setEmail(String v)     { email     = v; }
        public static String getPhone()             { return phone; }
        public static void   setPhone(String v)     { phone     = v; }
        public static String getRoleName()          { return roleName; }
        public static void   setRoleName(String v)  { roleName  = v; }
        public static String getDisplayName()       { return (firstName + " " + lastName).trim(); }
        public static void   setDisplayName(String v) {
            String[] p = v == null ? new String[0] : v.trim().split("\\s+", 2);
            firstName = p.length > 0 ? p[0] : "";
            lastName  = p.length > 1 ? p[1] : "";
        }
    }

    // ── Глобальний стан валюти з Observer ───────────────────────────────────
    public static class CurrencySession {
        private static String currencyLabel = "грн";
        private static CurrencyRate rate    = null;
        private static final java.util.List<Runnable> listeners = new java.util.ArrayList<>();

        public static String getCurrencyLabel()       { return currencyLabel; }
        public static CurrencyRate getRate()          { return rate; }
        public static void setRate(CurrencyRate r)    { rate = r; }

        /** Зареєструвати слухача — викликається при зміні валюти */
        public static void addListener(Runnable r)    { listeners.add(r); }
        public static void removeListener(Runnable r) { listeners.remove(r); }

        /** Змінити валюту і сповістити всіх слухачів */
        public static void setCurrencyLabel(String v) {
            currencyLabel = v;
            for (Runnable r : new java.util.ArrayList<>(listeners)) {
                try { r.run(); } catch (Exception ignored) {}
            }
        }

        /** Символ валюти */
        public static String getSymbol() {
            if (currencyLabel == null || currencyLabel.equals("грн")) return "грн";
            if (currencyLabel.startsWith("$")) return "$";
            if (currencyLabel.startsWith("€")) return "€";
            return "грн";
        }

        /**
         * Конвертує UAH у вибрану валюту.
         * UAH → USD: uah / usdToUah   (напр. 34800 / 41.50 = $839)
         * UAH → EUR: uah / usdToUah * usdToEur
         */
        public static java.math.BigDecimal convert(java.math.BigDecimal uahAmount) {
            if (uahAmount == null) return java.math.BigDecimal.ZERO;
            if (rate == null || currencyLabel.equals("грн")) return uahAmount;
            try {
                java.math.BigDecimal usdToUah = rate.getUsdToUah();
                java.math.BigDecimal usdToEur = rate.getUsdToEur();
                if (currencyLabel.startsWith("$")) {
                    return uahAmount.divide(usdToUah, 2, java.math.RoundingMode.HALF_UP);
                } else if (currencyLabel.startsWith("€")) {
                    java.math.BigDecimal usd = uahAmount.divide(usdToUah, 6, java.math.RoundingMode.HALF_UP);
                    return usd.multiply(usdToEur).setScale(2, java.math.RoundingMode.HALF_UP);
                }
            } catch (Exception ignored) {}
            return uahAmount;
        }

        /** Форматує ціну у вибраній валюті */
        public static String format(java.math.BigDecimal uahAmount) {
            java.math.BigDecimal converted = convert(uahAmount);
            String symbol = getSymbol();
            String number = String.format("%,.0f", converted);
            // грн ставимо після числа: "34 800 грн", $ і € — перед: "$839"
            if (symbol.equals("грн")) {
                return number + "\u00A0" + symbol;
            }
            return symbol + "\u00A0" + number;
        }
    }
}