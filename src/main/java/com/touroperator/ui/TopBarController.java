package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.service.NotificationService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;
import java.util.logging.Logger;

public class TopBarController {

    private static final Logger log = Logger.getLogger(TopBarController.class.getName());

    @FXML private Label     lblUserName;
    @FXML private Button    notifBtn;
    @FXML private Button    btnFullscreen;
    @FXML private Pane      notifBadge;
    @FXML private Pane      notifWrapper;

    private Popup profilePopup;
    private Popup notifPopup;

    private int            unreadCount      = 0;
    private boolean        listenerAttached = false;
    private UserRole       currentRole      = UserRole.CLIENT;
    private NotificationService.NotificationListener myListener;
    private MainController mainController;
    private java.util.function.Consumer<String> profileNameListener;

    @FXML
    public void initialize() {
        tryAttachListener();
    }

    /** Зберігає посилання на MainController — потрібне для logout із профілю */
    public void setMainController(MainController mc) {
        this.mainController = mc;
    }

    /** Викликається MainController після setUser() — Spring точно готовий */
    public void updateUser(String displayName, String roleName) {
        String initials = toInitials(displayName);
        if (lblUserName != null) lblUserName.setText(initials);

           
        this.currentRole = ("ADMIN".equalsIgnoreCase(roleName)
              || UserRole.ADMIN.getDisplayName().equalsIgnoreCase(roleName))
              ? UserRole.ADMIN : UserRole.CLIENT;

        ProfilePanelController.SessionState.setDisplayName(displayName);
        ProfilePanelController.SessionState.setRoleName(roleName);

           
        if (profileNameListener != null) {
            ProfilePanelController.SessionState.removeProfileListener(profileNameListener);
        }
        profileNameListener = newName ->
              Platform.runLater(() -> {
                  if (lblUserName != null) lblUserName.setText(toInitials(newName));
              });
        ProfilePanelController.SessionState.addProfileListener(profileNameListener);

           
        tryAttachListener();

           
           
        checkHistoryForUnread();
    }

    /**
     * Сканує history NotificationService і рахує непрочитані події для поточної ролі.
     * Викликається після входу/зміни ролі, щоб badge загорівся одразу.
     */
    private void checkHistoryForUnread() {
        try {
            NotificationService notifSvc = SpringContext.getBean(NotificationService.class);
            long count = notifSvc.getRecent(50).stream()
                  .filter(n -> isRelevantForRole(n.type()))
                  .count();
            unreadCount = (int) count;
            Platform.runLater(this::updateBell);
        } catch (Exception e) {
            log.warning("checkHistoryForUnread помилка: " + e.getMessage());
        }
    }

    /** Реєструє listener рівно один раз */
    private void tryAttachListener() {
        if (listenerAttached) return;
        try {
            NotificationService notifSvc = SpringContext.getBean(NotificationService.class);
            myListener = event -> Platform.runLater(() -> {
                   
                boolean relevant = isRelevantForRole(event.type());
                if (!relevant) return;
                unreadCount++;
                updateBell();
                showToast(event);
                if (notifPopup != null && notifPopup.isShowing()) {
                    refreshNotifPopup(notifSvc);
                }
            });
            notifSvc.addListener(myListener);
            listenerAttached = true;
            log.info("NotificationService listener зареєстровано");
        } catch (Exception e) {
            log.warning("Не вдалося зареєструвати listener: " + e.getMessage());
        }
    }

       

    @FXML
    private void onFullscreenToggle() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) lblUserName.getScene().getWindow();
            boolean full = !stage.isFullScreen();
               
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
            stage.setFullScreen(full);
            if (btnFullscreen != null) {
                btnFullscreen.setText(full ? "⛶" : "⛶");
                btnFullscreen.setStyle(full
                      ? "-fx-cursor:hand;-fx-opacity:1.0;"
                      : "-fx-cursor:hand;");
            }
        } catch (Exception e) {
            log.warning("onFullscreenToggle помилка: " + e.getMessage());
        }
    }

       

    /** Знімає listener при logout — запобігає накопиченню "зомбі-listeners" між сесіями */
    public void detachListener() {
           
        if (listenerAttached && myListener != null) {
            try {
                NotificationService notifSvc = SpringContext.getBean(NotificationService.class);
                notifSvc.removeListener(myListener);
                myListener = null;
                listenerAttached = false;
                log.info("NotificationService listener знято");
            } catch (Exception ignored) {}
        }
           
        if (profileNameListener != null) {
            ProfilePanelController.SessionState.removeProfileListener(profileNameListener);
            profileNameListener = null;
        }
           
        unreadCount = 0;
        Platform.runLater(this::updateBell);
    }

    @FXML
    private void onNotifClick() {
        if (notifPopup != null && notifPopup.isShowing()) {
            notifPopup.hide();
            return;
        }
        unreadCount = 0;
        updateBell();

        try {
            NotificationService notifSvc = SpringContext.getBean(NotificationService.class);
               
            List<NotificationService.AppNotification> filtered = notifSvc.getRecent(20).stream()
                  .filter(n -> isRelevantForRole(n.type()))
                  .toList();
            VBox panel = buildNotifPanel(filtered);

            notifPopup = new Popup();
            notifPopup.setAutoHide(true);
            notifPopup.getContent().add(panel);

            Bounds b = notifBtn.localToScreen(notifBtn.getBoundsInLocal());
            notifPopup.show(notifBtn.getScene().getWindow(),
                  b.getMaxX() - 340, b.getMaxY() + 10);
        } catch (Exception e) {
            log.warning("onNotifClick помилка: " + e.getMessage());
        }
    }

    /**
     * Адмін бачить лише адміністративні події (нові бронювання, підтвердження тощо).
     * Клієнт — лише свої особисті сповіщення (CLIENT_*).
     * CLIENT_* події НІКОЛИ не показуються адміну.
     */
    private boolean isRelevantForRole(NotificationService.EventType type) {
        if (currentRole == UserRole.ADMIN) {
            return switch (type) {
                case BOOKING_CREATED, BOOKING_CONFIRMED, BOOKING_PAID,
                     BOOKING_CANCELLED, BOOKING_COMPLETED, EXCEL_EXPORTED -> true;
                   
                default -> false;
            };
        } else {
            return switch (type) {
                case CLIENT_BOOKING_CANCELLED, CLIENT_BOOKING_CONFIRMED,
                     CLIENT_BOOKING_PAID, CLIENT_TOUR_REMINDER -> true;
                default -> false;
            };
        }
    }

    private void refreshNotifPopup(NotificationService svc) {
        if (notifPopup == null || !notifPopup.isShowing()) return;
        notifPopup.getContent().clear();
        List<NotificationService.AppNotification> filtered = svc.getRecent(20).stream()
              .filter(n -> isRelevantForRole(n.type()))
              .toList();
        notifPopup.getContent().add(buildNotifPanel(filtered));
    }

    private VBox buildNotifPanel(List<NotificationService.AppNotification> items) {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("notif-panel");
        panel.setPrefWidth(340);
        panel.setStyle(
              "-fx-background-color:#ffffff;" +
                    "-fx-background-radius:14;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),18,0,0,4);" +
                    "-fx-border-color:#e8f0e0;-fx-border-radius:14;-fx-border-width:1;"
        );

        Label title = new Label("Сповіщення");
        title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#27500a;" +
              "-fx-padding:14 16 10 16;-fx-font-family:'DM Sans';");
        panel.getChildren().add(title);

        if (items.isEmpty()) {
            Label empty = new Label("Немає нових сповіщень");
            empty.setStyle("-fx-text-fill:#8a9a85;-fx-font-size:13px;" +
                  "-fx-padding:12 16 16 16;-fx-font-family:'DM Sans';");
            panel.getChildren().add(empty);
            return panel;
        }

        for (NotificationService.AppNotification n : items) {
            HBox row = new HBox(10);
            row.setStyle("-fx-padding:10 16;" +
                  "-fx-border-color:transparent transparent #f0f5eb transparent;" +
                  "-fx-border-width:1;");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label icon = new Label(n.icon());
            icon.setStyle("-fx-font-size:16px;");

            VBox textBox = new VBox(2);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            Label msg = new Label(n.message());
            msg.setStyle("-fx-font-size:12px;-fx-text-fill:#2d3b22;" +
                  "-fx-font-family:'DM Sans';-fx-wrap-text:true;");
            msg.setMaxWidth(240);
            Label time = new Label(n.timeAgo());
            time.setStyle("-fx-font-size:11px;-fx-text-fill:#8a9a85;-fx-font-family:'DM Sans';");
            textBox.getChildren().addAll(msg, time);

            row.getChildren().addAll(icon, textBox);
            panel.getChildren().add(row);
        }
        return panel;
    }

    /** Показує тимчасовий toast у правому нижньому куті екрану */
    private void showToast(NotificationService.AppNotification n) {
        try {
            HBox toast = new HBox(10);
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.setStyle(
                  "-fx-background-color:#ffffff;" +
                        "-fx-background-radius:12;" +
                        "-fx-padding:12 16 12 14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),16,0,0,4);" +
                        "-fx-border-color:#d4eabc;-fx-border-radius:12;-fx-border-width:1;"
            );
            toast.setPrefWidth(320);

            Label icon = new Label(n.icon());
            icon.setStyle("-fx-font-size:18px;");

            VBox textBox = new VBox(2);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            Label msg = new Label(n.message());
            msg.setStyle("-fx-font-size:12px;-fx-text-fill:#2d3b22;" +
                  "-fx-font-family:'DM Sans';-fx-wrap-text:true;");
            msg.setMaxWidth(260);
            Label time = new Label(n.timeAgo());
            time.setStyle("-fx-font-size:11px;-fx-text-fill:#8a9a85;-fx-font-family:'DM Sans';");
            textBox.getChildren().addAll(msg, time);
            toast.getChildren().addAll(icon, textBox);

            Popup toastPopup = new Popup();
            toastPopup.setAutoHide(false);
            toastPopup.getContent().add(toast);

               
            javafx.stage.Window window = notifBtn.getScene().getWindow();
            double x = window.getX() + window.getWidth()  - 340;
            double y = window.getY() + window.getHeight() - 110;
            toastPopup.show(window, x, y);

               
            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);

            PauseTransition pause = new PauseTransition(Duration.millis(3500));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> toastPopup.hide());

            SequentialTransition seq = new SequentialTransition(fadeIn, pause, fadeOut);
            seq.play();
        } catch (Exception e) {
            log.warning("showToast помилка: " + e.getMessage());
        }
    }

    /** Показує/ховає червону крапку над дзвоником */
    private void updateBell() {
        if (notifBadge == null) return;
        notifBadge.setVisible(unreadCount > 0);
    }

       

    @FXML
    private void onUserChipClick() {
        if (profilePopup != null && profilePopup.isShowing()) {
            profilePopup.hide();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/ProfilePanel.fxml"));
            VBox panel = loader.load();
            ProfilePanelController ctrl = loader.getController();

            profilePopup = new Popup();
            profilePopup.setAutoHide(true);
            profilePopup.getContent().add(panel);

            ctrl.setOnClose(() -> profilePopup.hide());
            if (mainController != null) {
                ctrl.setOnLogout(() -> {
                    profilePopup.hide();
                    mainController.logout();
                });
            }

            Bounds bounds = lblUserName.localToScreen(lblUserName.getBoundsInLocal());
            profilePopup.show(
                  lblUserName.getScene().getWindow(),
                  bounds.getMaxX() - 380,
                  bounds.getMaxY() + 10
            );
        } catch (Exception e) {
            log.warning("onUserChipClick помилка: " + e.getMessage());
        }
    }

       

    private static String toInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) return "??";
        String[] parts = displayName.trim().split("[\\s.]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (sb.length() == 2) break;
            }
        }
        return sb.length() > 0 ? sb.toString() : "??";
    }
}