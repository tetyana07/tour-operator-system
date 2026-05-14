package com.touroperator.ui;

import com.touroperator.config.SpringContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входу JavaFX.
 *
 * Послідовність запуску:
 *   1. init()  → завантаження шрифтів + Spring context
 *   2. start() → SplashView (анімований сплеш ~3с)
 *   3. Splash  → LoginView (вхід / реєстрація)
 *   4. Login   → MainLayout (оригінальний дизайн VOYA)
 */
public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void init() {
        String[] fonts = {
              "/fonts/Unbounded-Regular.ttf",
              "/fonts/Unbounded-Medium.ttf",
              "/fonts/Unbounded-SemiBold.ttf",
              "/fonts/Unbounded-Bold.ttf",
              "/fonts/Unbounded-ExtraBold.ttf",
              "/fonts/Unbounded-Black.ttf",
              "/fonts/Unbounded-Light.ttf",
              "/fonts/Unbounded-ExtraLight.ttf"
        };
        for (String font : fonts) {
            var stream = getClass().getResourceAsStream(font);
            if (stream != null) Font.loadFont(stream, 14);
            else log.warn("Шрифт не знайдено: {}", font);
        }

        log.info("Ініціалізація Spring контексту...");
        SpringContext.init();
        log.info("Spring контекст готовий");
    }

    @Override
    public void start(Stage stage) throws Exception {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");

        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("/ui/SplashView.fxml"));
        Scene splashScene = new Scene(splashLoader.load(), 900, 620);
        splashScene.getStylesheets().add(
              getClass().getResource("/css/style.css").toExternalForm());

        SplashController splashCtrl = splashLoader.getController();
        splashCtrl.setStage(stage);

        stage.setScene(splashScene);
        stage.setTitle("AYVO");
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() {
        SpringContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
