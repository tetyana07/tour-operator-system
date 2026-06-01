package com.touroperator.ui;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainController {

    @FXML private StackPane pageContainer;


    @FXML private SidebarController  iconSidebarController;
    @FXML private TopBarController   topBarController;

    private final Map<String, Node> pageCache = new HashMap<>();


    private static final Set<String> NO_CACHE_PAGES =
          Set.of("tours", "bookings", "dashboard", "clients", "payments");

    private UserRole currentRole  = UserRole.CLIENT;
    private String   currentEmail = "";

    @FXML
    public void initialize() {

    }

    /** Викликається LoginController одразу після loader.load(). */
    public void setUser(String email, UserRole role) {
        this.currentEmail = email;
        this.currentRole  = role;


        if (topBarController != null) {
            topBarController.setMainController(this);
            topBarController.updateUser(formatDisplayName(email), role.name()); // role.name() = "ADMIN"/"CLIENT"
        }


        ProfilePanelController.SessionState.setEmail(email != null ? email : "");

        if (role == UserRole.ADMIN) {

            ProfilePanelController.SessionState.setDisplayName("Адміністратор");
            ProfilePanelController.SessionState.setPhone("");
            ProfilePanelController.SessionState.setRoleName(role.getDisplayName());
        } else {
            try {
                com.touroperator.repository.ClientRepository clientRepo =
                      com.touroperator.config.SpringContext.getBean(
                            com.touroperator.repository.ClientRepository.class);
                clientRepo.findByEmail(email).ifPresent(client -> {

                    if (client.getName() != null && !client.getName().isBlank()) {
                        ProfilePanelController.SessionState.setDisplayName(client.getName());
                        if (topBarController != null) {
                            topBarController.updateUser(client.getName(), role.name());
                        }
                    }
                    if (client.getPhone() != null) {
                        ProfilePanelController.SessionState.setPhone(client.getPhone());
                    }
                });
            } catch (Exception ignored) {}
        }


        if (iconSidebarController != null) {
            iconSidebarController.applyRole(role);
        }

        pageCache.clear();
        showPage("tours");
    }

    public void showPage(String pageName) {
        pageContainer.getChildren().clear();
        try {

            boolean skipCache = NO_CACHE_PAGES.contains(pageName);
            if (skipCache) {
                pageCache.remove(pageName);
            }

            Node page;
            if (!pageCache.containsKey(pageName)) {
                String path = "/ui/pages/" + capitalize(pageName) + "Page.fxml";
                var url = getClass().getResource(path);
                if (url == null) { showPlaceholder(pageName); return; }

                FXMLLoader loader = new FXMLLoader(url);
                page = loader.load();

                Object ctrl = loader.getController();
                if (ctrl instanceof RoleAware ra) ra.setRole(currentRole, currentEmail);

                if (!skipCache) {
                    pageCache.put(pageName, page);
                }
            } else {
                page = pageCache.get(pageName);
            }
            page.setOpacity(0);
            pageContainer.getChildren().add(page);

            FadeTransition ft = new FadeTransition(Duration.millis(180), page);
            ft.setToValue(1.0);
            ft.play();

        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholder(pageName);
        }
    }

    private void showPlaceholder(String pageName) {
        var lbl = new javafx.scene.control.Label("Сторінка «" + capitalize(pageName) + "» у розробці");
        lbl.setStyle("-fx-text-fill:#7a9a6a; -fx-font-size:15px; -fx-padding:40;");
        pageContainer.getChildren().add(lbl);
    }

    public void logout() {
        pageCache.clear();
        if (topBarController != null) {
            topBarController.detachListener();
        }
        try {
            Stage stage = (Stage) pageContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 900, 650);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            LoginController ctrl = loader.getController();
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.setTitle("AYVO — Вхід");
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String formatDisplayName(String email) {
        if (email == null || !email.contains("@")) return email;
        String local = email.substring(0, email.indexOf('@'));
        String[] parts = local.split("[._\\-]");
        if (parts.length >= 2)
            return capitalize(parts[0]) + " " + Character.toUpperCase(parts[1].charAt(0)) + ".";
        return capitalize(local);
    }

    /** Видаляє сторінку з кешу — наступний showPage перезавантажить її з нуля. */
    public void invalidatePage(String pageName) {
        pageCache.remove(pageName);
    }

    public UserRole getCurrentRole()  { return currentRole; }
    public String   getCurrentEmail() { return currentEmail; }
}