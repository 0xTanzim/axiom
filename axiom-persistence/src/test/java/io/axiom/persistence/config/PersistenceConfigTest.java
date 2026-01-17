package io.axiom.persistence.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceConfigTest {

    @Test
    void builderCreatesValidConfig() {
        var config = PersistenceConfig.builder()
                .url("jdbc:h2:mem:test")
                .username("sa")
                .password("")
                .maximumPoolSize(5)
                .build();

        assertEquals("jdbc:h2:mem:test", config.url());
        assertEquals("sa", config.username());
        assertEquals("", config.password());
        assertEquals(5, config.maximumPoolSize());
    }

    @Test
    void builderUsesDefaults() {
        var config = PersistenceConfig.builder()
                .url("jdbc:h2:mem:test")
                .build();

        assertEquals(10, config.maximumPoolSize());
        assertEquals(10, config.minimumIdle());
        assertEquals(Duration.ofSeconds(30), config.connectionTimeout());
        assertEquals(Duration.ofMinutes(10), config.idleTimeout());
        assertEquals(Duration.ofMinutes(30), config.maxLifetime());
        assertFalse(config.autoCommit());
    }

    @Test
    void fromPropertiesLoadsAllValues() {
        Properties props = new Properties();
        props.setProperty("axiom.datasource.url", "jdbc:postgresql://localhost/mydb");
        props.setProperty("axiom.datasource.username", "user");
        props.setProperty("axiom.datasource.password", "secret");
        props.setProperty("axiom.datasource.driver-class-name", "org.postgresql.Driver");
        props.setProperty("axiom.datasource.pool.maximum-size", "20");
        props.setProperty("axiom.datasource.pool.minimum-idle", "5");
        props.setProperty("axiom.datasource.pool.name", "my-pool");

        var config = PersistenceConfig.fromProperties(props);

        assertEquals("jdbc:postgresql://localhost/mydb", config.url());
        assertEquals("user", config.username());
        assertEquals("secret", config.password());
        assertEquals("org.postgresql.Driver", config.driverClassName());
        assertEquals(20, config.maximumPoolSize());
        assertEquals(5, config.minimumIdle());
        assertEquals("my-pool", config.poolName());
    }

    @Test
    void fromPropertiesHandlesTimeoutSuffixes() {
        Properties props = new Properties();
        props.setProperty("axiom.datasource.url", "jdbc:h2:mem:test");
        props.setProperty("axiom.datasource.pool.connection-timeout", "5000");
        props.setProperty("axiom.datasource.pool.idle-timeout", "60000");

        var config = PersistenceConfig.fromProperties(props);

        assertEquals(Duration.ofMillis(5000), config.connectionTimeout());
        assertEquals(Duration.ofMillis(60000), config.idleTimeout());
    }

    @Test
    void hibernatePropertiesAreCopied() {
        Properties props = new Properties();
        props.setProperty("axiom.datasource.url", "jdbc:h2:mem:test");
        props.setProperty("hibernate.show_sql", "true");
        props.setProperty("hibernate.format_sql", "true");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");

        var config = PersistenceConfig.fromProperties(props);

        assertEquals("true", config.hibernateProperties().get("hibernate.show_sql"));
        assertEquals("true", config.hibernateProperties().get("hibernate.format_sql"));
        assertEquals("create-drop", config.hibernateProperties().get("hibernate.hbm2ddl.auto"));
    }

    @Test
    void configIsImmutable() {
        var config = PersistenceConfig.builder()
                .url("jdbc:h2:mem:test")
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
            config.hibernateProperties().put("key", "value")
        );

        assertThrows(UnsupportedOperationException.class, () ->
            config.dataSourceProperties().put("key", "value")
        );
    }

    @Test
    void builderAllowsFluentChaining() {
        var config = PersistenceConfig.builder()
                .url("jdbc:h2:mem:test")
                .username("sa")
                .password("")
                .driverClassName("org.h2.Driver")
                .maximumPoolSize(20)
                .minimumIdle(5)
                .connectionTimeout(Duration.ofSeconds(10))
                .idleTimeout(Duration.ofMinutes(5))
                .maxLifetime(Duration.ofMinutes(15))
                .validationTimeout(Duration.ofSeconds(5))
                .leakDetectionThreshold(Duration.ofMinutes(2))
                .poolName("test-pool")
                .autoCommit(false)
                .connectionTestQuery("SELECT 1")
                .build();

        assertNotNull(config);
        assertEquals("test-pool", config.poolName());
        assertEquals(Duration.ofMinutes(2), config.leakDetectionThreshold());
    }
}
