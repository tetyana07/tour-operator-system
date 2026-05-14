package com.touroperator.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.util.List;

public class SidebarController {

    @FXML private Button    btnTours;
    @FXML private StackPane bookingsBadgePane; // StackPane що містить btnBookings + badge
    @FXML private Button    btnBookings;
    @FXML private Button    btnPayments;
    @FXML private Button    btnClients;
    @FXML private Button    btnPromos;
    @FXML private Button    btnReports;
    @FXML private Button    btnSettings;

    private List<Button> allBtns;

    @FXML
    public void initialize() {
        allBtns = List.of(btnTours, btnBookings, btnPayments,
              btnClients, btnPromos, btnReports, btnSettings);
    }

    /**
     * Матриця доступу:
     *   ADMIN  — всі пункти
     *   CLIENT — тільки Тури, Бронювання, Налаштування
     */
    public void applyRole(UserRole role) {
        boolean isAdmin = role == UserRole.ADMIN;
        setVisible(btnPayments, isAdmin);
        setVisible(btnClients,  isAdmin);
        setVisible(btnPromos,   isAdmin);
        setVisible(btnReports,  isAdmin);
        // btnTours, btnBookings, btnSettings — для всіх
    }

    private void setVisible(Button btn, boolean visible) {
        if (btn == null) return;
        btn.setVisible(visible);
        btn.setManaged(visible);
    }

    @FXML
    private void onNav(javafx.event.ActionEvent e) {
        Button clicked = (Button) e.getSource();

        allBtns.forEach(b -> {
            b.getStyleClass().remove("nav-icon-btn-active");
            if (!b.getStyleClass().contains("nav-icon-btn"))
                b.getStyleClass().add("nav-icon-btn");
        });

        clicked.getStyleClass().remove("nav-icon-btn");
        clicked.getStyleClass().add("nav-icon-btn-active");

        // Отримуємо MainController через userData сцени
        Object userData = clicked.getScene().getUserData();
        if (userData instanceof MainController mc) {
            mc.showPage((String) clicked.getUserData());
        }
    }
}
