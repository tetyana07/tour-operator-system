package com.touroperator.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Головний Spring конфіг.
 * Ініціалізує DataSource (HikariCP connection pool), JdbcTemplate, Flyway.
 *
 * <p>Використовується патерн Connection Pool (HikariCP) замість прямого
 * підключення через DriverManagerDataSource. Це дозволяє повторно
 * використовувати з'єднання з БД замість створення нового для кожного запиту,
 * що значно підвищує продуктивність при паралельних операціях.</p>
 */
@Configuration
@ComponentScan(basePackages = "com.touroperator")
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private final Properties props = loadProps();

    /**
     * Налаштовує пул з'єднань HikariCP.
     *
     * <p>HikariCP — найшвидший JDBC connection pool для Java.
     * Замість створення нового з'єднання на кожен запит, пул утримує
     * готові з'єднання і видає їх за потреби.</p>
     *
     * @return налаштований {@link DataSource} на основі HikariCP
     */
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(props.getProperty("db.url", "jdbc:postgresql://localhost:5432/tour_operator"));
        config.setUsername(props.getProperty("db.user", "postgres"));
        config.setPassword(props.getProperty("db.password", ""));

        // Розмір пулу: мінімум 2, максимум 10 активних з'єднань
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);

        // Час очікування з'єднання з пулу — 30 секунд
        config.setConnectionTimeout(30_000);

        // Час простою з'єднання до закриття — 10 хвилин
        config.setIdleTimeout(600_000);

        // Максимальний час життя з'єднання — 30 хвилин
        config.setMaxLifetime(1_800_000);

        config.setPoolName("TourOperatorPool");

        log.info("HikariCP pool ініціалізовано: {} (max={})",
              config.getJdbcUrl(), config.getMaximumPoolSize());

        return new HikariDataSource(config);
    }

    /**
     * Створює {@link JdbcTemplate} на основі пулу з'єднань.
     *
     * @param dataSource пул з'єднань HikariCP
     * @return налаштований JdbcTemplate
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Ініціалізує Flyway для автоматичного управління міграціями БД.
     *
     * @param dataSource пул з'єднань
     * @return налаштований екземпляр Flyway
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
              .dataSource(dataSource)
              .locations("classpath:db/migration")
              .baselineOnMigrate(true)
              .load();
    }

    /**
     * Повертає API-ключ для сервісу погоди.
     *
     * @return рядок з API-ключем
     */
    @Bean
    public String weatherApiKey() {
        return props.getProperty("weather.api.key", "");
    }

    private Properties loadProps() {
        Properties p = new Properties();
        try (InputStream is = getClass().getClassLoader()
              .getResourceAsStream("application.properties")) {
            if (is != null) p.load(is);
        } catch (IOException e) {
            log.warn("Не вдалося завантажити application.properties: {}", e.getMessage());
        }
        return p;
    }
}
