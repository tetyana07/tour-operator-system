package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import com.touroperator.domain.PromoCode;
import com.touroperator.service.PromoCodeService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контролер сторінки промокодів.
 * Динамічно будує картки промокодів із БД.
 */
public class PromosController {

    @FXML private FlowPane promosContainer;

    private PromoCodeService promoService;

    @FXML
    public void initialize() {
        try {
            promoService = SpringContext.getBean(PromoCodeService.class);
            refreshCards();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void refreshCards() {
        if (promosContainer == null) return;
        promosContainer.getChildren().clear();
        List<PromoCode> promos = promoService.findAll();
        for (PromoCode p : promos) {
            promosContainer.getChildren().add(buildPromoCard(p));
        }
        promosContainer.getChildren().add(buildAddCard());
    }


    private VBox buildPromoCard(PromoCode p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("panel-card");
        card.setPrefWidth(240);
        card.setPadding(new Insets(16));

        Label code = new Label(p.getCode());
        code.getStyleClass().add("promo-code-big");

        Label discount = new Label("-" + p.getDiscountPercent() + "% знижка");
        discount.getStyleClass().add("stat-change");

        Label until = new Label("Діє до: " + p.getValidUntil()
              .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        until.getStyleClass().add("promo-expiry-lbl");

        ProgressBar bar = new ProgressBar(p.isValid() ? 1.0 : 0.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("quota-bar");

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label status = new Label(p.isValid() ? "✓ Активний" : "✕ Прострочений");
        status.getStyleClass().add(p.isValid() ? "pill-active" : "pill-expired");
        HBox.setHgrow(status, Priority.ALWAYS);


        Button delBtn = new Button("🗑");
        delBtn.getStyleClass().add("btn-ghost");
        delBtn.setStyle("-fx-font-size:11px;-fx-padding:3 7;");
        delBtn.setTooltip(new Tooltip("Видалити промокод"));
        delBtn.setOnAction(e -> {
            promoService.delete(p.getId());
            refreshCards();
        });

        footer.getChildren().addAll(status, delBtn);
        card.getChildren().addAll(code, discount, until, bar, footer);
        return card;
    }


    private VBox buildAddCard() {
        VBox card = new VBox();
        card.setPrefWidth(240);
        card.setPrefHeight(145);
        card.setAlignment(Pos.CENTER);
        card.setSpacing(8);
        card.setPadding(new Insets(16));
        card.setStyle(
              "-fx-background-color:transparent;" +
                    "-fx-border-color:#7ab648;" +
                    "-fx-border-width:2;" +
                    "-fx-border-style:dashed;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;" +
                    "-fx-cursor:hand;"
        );

        Label plus = new Label("+");
        plus.setStyle(
              "-fx-text-fill:#7ab648;" +
                    "-fx-font-size:22px;" +
                    "-fx-font-weight:bold;"
        );

        Label text = new Label("Новий промокод");
        text.setStyle(
              "-fx-text-fill:#7ab648;" +
                    "-fx-font-size:13px;"
        );

        card.getChildren().addAll(plus, text);


        card.setOnMouseEntered(e -> card.setStyle(
              "-fx-background-color:rgba(99,153,34,0.07);" +
                    "-fx-border-color:#4a8c1c;" +
                    "-fx-border-width:2;" +
                    "-fx-border-style:dashed;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;" +
                    "-fx-cursor:hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
              "-fx-background-color:transparent;" +
                    "-fx-border-color:#7ab648;" +
                    "-fx-border-width:2;" +
                    "-fx-border-style:dashed;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;" +
                    "-fx-cursor:hand;"
        ));

        card.setOnMouseClicked(e -> onAddPromo());
        return card;
    }


    @FXML
    private void onAddPromo() {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        // ═══════════════════════════════════════════════════════════════
        // ROOT MODAL
        // ═══════════════════════════════════════════════════════════════
        VBox modal = new VBox(0);
        modal.getStyleClass().add("tour-detail-modal");
        modal.setMaxWidth(460);
        modal.setPrefWidth(460);


        HBox hero = new HBox(12);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setStyle(
              "-fx-background-color: linear-gradient(to bottom right, #173404, #2a5a08);" +
                    "-fx-padding: 22 24 22 26;" +
                    "-fx-background-radius: 28 28 0 0;"
        );



        VBox heroText = new VBox(3);
        HBox.setHgrow(heroText, Priority.ALWAYS);
        Label heroSub = new Label("УПРАВЛІННЯ ЗНИЖКАМИ");
        heroSub.setStyle(
              "-fx-font-family:'Syne'; -fx-font-size:10px; -fx-font-weight:bold;" +
                    "-fx-text-fill:rgba(143,203,163,0.85); -fx-letter-spacing:1.4;"
        );
        Label heroTitle = new Label("Новий промокод");
        heroTitle.setStyle(
              "-fx-font-family:'Syne'; -fx-font-size:18px; -fx-font-weight:bold;" +
                    "-fx-text-fill:white;"
        );
        heroText.getChildren().addAll(heroSub, heroTitle);

        Label closeBtn = new Label("✕");
        closeBtn.getStyleClass().add("tour-modal-close-btn");
        closeBtn.setOnMouseClicked(e -> stage.close());

        hero.getChildren().addAll( heroText, closeBtn);


        VBox body = new VBox(18);
        body.setPadding(new Insets(26, 26, 10, 26));

        // --- Поле: КОД ПРОМОКОДУ ---
        TextField codeField = new TextField();
        codeField.setPromptText("Наприклад: ЛІТО2026");
        codeField.getStyleClass().add("field-input");
        codeField.setMaxWidth(Double.MAX_VALUE);
        codeField.setStyle(
              "-fx-font-family:'Syne'; -fx-font-size:16px; -fx-font-weight:bold;" +
                    "-fx-background-color:#f4faea;" +
                    "-fx-border-color:rgba(99,153,34,0.25); -fx-border-width:1.5;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-text-fill:#0d2010; -fx-padding:11 14;"
        );

        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(newVal.toUpperCase())) {
                int caret = codeField.getCaretPosition();
                codeField.setText(newVal.toUpperCase());
                codeField.positionCaret(caret);
            }
        });


        HBox row2 = new HBox(14);


        Spinner<Integer> discountSpinner = new Spinner<>(1, 99, 10, 1);
        discountSpinner.setEditable(true);
        HBox.setHgrow(discountSpinner, Priority.ALWAYS);
        discountSpinner.setMaxWidth(Double.MAX_VALUE);
        discountSpinner.setStyle(
              "-fx-background-color:#f4faea;" +
                    "-fx-border-color:rgba(99,153,34,0.25); -fx-border-width:1.5;" +
                    "-fx-border-radius:10; -fx-background-radius:10;"
        );
        discountSpinner.getEditor().setStyle(
              "-fx-background-color:#f4faea; -fx-text-fill:#0d2010;" +
                    "-fx-font-size:14px; -fx-font-family:'DM Sans';"
        );


        DatePicker datePicker = new DatePicker(LocalDate.now().plusMonths(3));
        HBox.setHgrow(datePicker, Priority.ALWAYS);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.getStyleClass().add("field-input");
        datePicker.setStyle(
              "-fx-background-color:#f4faea;" +
                    "-fx-border-color:rgba(99,153,34,0.25); -fx-border-width:1.5;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-font-size:13px;"
        );

        VBox discountBlock = fieldBlock("ЗНИЖКА (%)", discountSpinner);
        HBox.setHgrow(discountBlock, Priority.ALWAYS);
        VBox dateBlock = fieldBlock("ДІЙСНИЙ ДО", datePicker);
        HBox.setHgrow(dateBlock, Priority.ALWAYS);
        row2.getChildren().addAll(discountBlock, dateBlock);


        HBox previewBox = new HBox(14);
        previewBox.setAlignment(Pos.CENTER_LEFT);
        previewBox.setStyle(
              "-fx-background-color:#f4faea;" +
                    "-fx-border-color:rgba(99,153,34,0.18); -fx-border-width:1;" +
                    "-fx-border-radius:12; -fx-background-radius:12;" +
                    "-fx-padding:14 18;"
        );
        Label previewIcon = new Label("🏷");
        previewIcon.setStyle("-fx-font-size:20px;");
        VBox previewText = new VBox(2);
        Label previewCode = new Label("—");
        previewCode.setStyle(
              "-fx-font-family:'Syne'; -fx-font-size:15px; -fx-font-weight:bold;" +
                    "-fx-text-fill:#173404;"
        );
        Label previewDiscount = new Label("Введіть дані вище");
        previewDiscount.setStyle("-fx-font-size:12px; -fx-text-fill:#639922;");
        previewText.getChildren().addAll(previewCode, previewDiscount);
        previewBox.getChildren().addAll(previewIcon, previewText);


        Runnable updatePreview = () -> {
            String c = codeField.getText().trim();
            int pct  = discountSpinner.getValue();
            LocalDate d = datePicker.getValue();
            previewCode.setText(c.isEmpty() ? "—" : c);
            previewDiscount.setText(c.isEmpty() ? "Введіть дані вище"
                  : "-" + pct + "% знижка · до " + (d != null
                        ? d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "?"));
        };
        codeField.textProperty().addListener((o, ov, nv) -> updatePreview.run());
        discountSpinner.valueProperty().addListener((o, ov, nv) -> updatePreview.run());
        datePicker.valueProperty().addListener((o, ov, nv) -> updatePreview.run());


        Label errorLabel = new Label();
        errorLabel.setStyle(
              "-fx-text-fill:#b83c2a; -fx-font-size:12px;" +
                    "-fx-background-color:rgba(184,60,42,0.08);" +
                    "-fx-border-color:rgba(184,60,42,0.25); -fx-border-width:1;" +
                    "-fx-border-radius:8; -fx-background-radius:8;" +
                    "-fx-padding:8 12;"
        );
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        body.getChildren().addAll(
              fieldBlock("КОД ПРОМОКОДУ", codeField),
              row2,
              fieldBlock("ПОПЕРЕДНІЙ ВИГЛЯД", previewBox),
              errorLabel
        );


        HBox footer = new HBox(10);
        footer.getStyleClass().add("tour-detail-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Скасувати");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = new Button("✚ Створити промокод");
        saveBtn.getStyleClass().add("add-btn");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            String code    = codeField.getText().trim().toUpperCase();
            Integer percent = discountSpinner.getValue();
            LocalDate until = datePicker.getValue();

            if (code.isEmpty()) {
                showError(errorLabel, "⚠  Введіть код промокоду."); return;
            }
            if (code.length() < 3) {
                showError(errorLabel, "⚠  Код має містити щонайменше 3 символи."); return;
            }
            if (until == null) {
                showError(errorLabel, "⚠  Оберіть дату завершення дії."); return;
            }
            if (until.isBefore(LocalDate.now())) {
                showError(errorLabel, "⚠  Дата завершення не може бути в минулому."); return;
            }

            try {
                PromoCode promo = new PromoCode(code, percent, until);
                promoService.save(promo);
                VoyaAlert.success("Промокод «" + code + "» створено — " + percent + "% знижки до "
                      + until.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ".");
                stage.close();
                refreshCards();
            } catch (Exception ex) {
                showError(errorLabel, ex.getMessage());
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        modal.getChildren().addAll(hero, body, footer);


        StackPane overlay = new StackPane(modal);
        overlay.getStyleClass().add("modal-overlay-pane");
        Scene scene = new Scene(overlay, 520, 480);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
              getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }


    private void showError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private VBox fieldBlock(String label, javafx.scene.Node field) {
        VBox vb = new VBox(6);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("tour-detail-section-title");
        VBox.setVgrow(field, Priority.NEVER);
        vb.getChildren().addAll(lbl, field);
        return vb;
    }
}