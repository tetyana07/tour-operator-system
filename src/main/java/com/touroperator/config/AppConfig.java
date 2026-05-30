package com.touroperator.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "com.touroperator")
@PropertySource("classpath:application.properties")
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private final Properties props = loadProps();

    /**
     * Реєструє application.properties як джерело для @Value ін'єкцій.
     * Обов'язково static — Spring має створити цей бін до ініціалізації інших.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(props.getProperty("db.url", "jdbc:postgresql://localhost:5432/tour_operator"));
        config.setUsername(props.getProperty("db.user", "postgres"));
        config.setPassword(props.getProperty("db.password", ""));

        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("TourOperatorPool");

        log.info("HikariCP pool ініціалізовано: {} (max={})",
              config.getJdbcUrl(), config.getMaximumPoolSize());

        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
              .dataSource(dataSource)
              .locations("classpath:db/migration")
              .baselineOnMigrate(true)
              .load();
    }

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