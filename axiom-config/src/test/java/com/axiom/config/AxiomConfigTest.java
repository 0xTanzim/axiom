package com.axiom.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for AxiomConfig - the main configuration facade.
 */
@DisplayName("AxiomConfig")
class AxiomConfigTest {

    @Nested
    @DisplayName("load()")
    class LoadTests {

        @Test
        @DisplayName("should create config instance with default sources")
        void loadsDefaultSources() {
            AxiomConfig config = AxiomConfig.load();

            assertThat(config).isNotNull();
            // Default value set by AxiomConfig
            assertThat(config.get("axiom.config.loaded")).contains("true");
        }
    }

    @Nested
    @DisplayName("parse(String)")
    class ParseTests {

        @Test
        @DisplayName("should parse properties string")
        void parsesPropertiesString() {
            String props = """
                    server.port=8080
                    server.host=localhost
                    """;

            AxiomConfig config = AxiomConfig.parse(props);

            assertThat(config.get("server.port")).contains("8080");
            assertThat(config.get("server.host")).contains("localhost");
        }

        @Test
        @DisplayName("should handle empty string")
        void handlesEmptyString() {
            AxiomConfig config = AxiomConfig.parse("");

            assertThat(config).isNotNull();
        }

        @Test
        @DisplayName("should handle comments")
        void handlesComments() {
            String props = """
                    # This is a comment
                    server.port=8080
                    """;

            AxiomConfig config = AxiomConfig.parse(props);

            assertThat(config.get("server.port")).contains("8080");
            assertThat(config.get("# This is a comment")).isEmpty();
        }
    }

    @Nested
    @DisplayName("get(String)")
    class GetTests {

        @Test
        @DisplayName("should return empty optional for missing key")
        void returnEmptyForMissingKey() {
            AxiomConfig config = AxiomConfig.parse("");

            assertThat(config.get("nonexistent.key")).isEmpty();
        }

        @Test
        @DisplayName("should return value for existing key")
        void returnsValueForExistingKey() {
            AxiomConfig config = AxiomConfig.parse("my.key=my-value");

            assertThat(config.get("my.key")).contains("my-value");
        }
    }

    @Nested
    @DisplayName("require(String)")
    class RequireTests {

        @Test
        @DisplayName("should return value for existing key")
        void returnsValueForExistingKey() {
            AxiomConfig config = AxiomConfig.parse("my.key=my-value");

            assertThat(config.require("my.key")).isEqualTo("my-value");
        }

        @Test
        @DisplayName("should throw ConfigException.Missing for missing key")
        void throwsForMissingKey() {
            AxiomConfig config = AxiomConfig.parse("");

            assertThatThrownBy(() -> config.require("missing.key"))
                    .isInstanceOf(ConfigException.Missing.class)
                    .hasMessageContaining("missing.key");
        }
    }

    @Nested
    @DisplayName("getInt(String)")
    class GetIntTests {

        @Test
        @DisplayName("should parse integer value")
        void parsesIntegerValue() {
            AxiomConfig config = AxiomConfig.parse("port=8080");

            assertThat(config.getInt("port")).contains(8080);
        }

        @Test
        @DisplayName("should return empty for missing key")
        void returnEmptyForMissingKey() {
            AxiomConfig config = AxiomConfig.parse("");

            assertThat(config.getInt("port")).isEmpty();
        }

        @Test
        @DisplayName("should throw for invalid integer")
        void throwsForInvalidInteger() {
            AxiomConfig config = AxiomConfig.parse("port=not-a-number");

            assertThatThrownBy(() -> config.getInt("port"))
                    .isInstanceOf(ConfigException.WrongType.class)
                    .hasMessageContaining("port");
        }
    }

    @Nested
    @DisplayName("getBoolean(String)")
    class GetBooleanTests {

        @Test
        @DisplayName("should parse true value")
        void parsesTrueValue() {
            AxiomConfig config = AxiomConfig.parse("enabled=true");

            assertThat(config.getBoolean("enabled")).contains(true);
        }

        @Test
        @DisplayName("should parse false value")
        void parsesFalseValue() {
            AxiomConfig config = AxiomConfig.parse("enabled=false");

            assertThat(config.getBoolean("enabled")).contains(false);
        }

        @Test
        @DisplayName("should return empty for missing key")
        void returnEmptyForMissingKey() {
            AxiomConfig config = AxiomConfig.parse("");

            assertThat(config.getBoolean("enabled")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDuration(String)")
    class GetDurationTests {

        @Test
        @DisplayName("should parse duration with seconds")
        void parsesDurationSeconds() {
            AxiomConfig config = AxiomConfig.parse("timeout=30s");

            assertThat(config.getDuration("timeout"))
                    .contains(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("should parse duration with minutes")
        void parsesDurationMinutes() {
            AxiomConfig config = AxiomConfig.parse("timeout=5m");

            assertThat(config.getDuration("timeout"))
                    .contains(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("should parse duration with milliseconds")
        void parsesDurationMilliseconds() {
            AxiomConfig config = AxiomConfig.parse("timeout=500ms");

            assertThat(config.getDuration("timeout"))
                    .contains(Duration.ofMillis(500));
        }

        @Test
        @DisplayName("should return empty for missing key")
        void returnEmptyForMissingKey() {
            AxiomConfig config = AxiomConfig.parse("");

            assertThat(config.getDuration("timeout")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build config with custom properties")
        void buildsWithCustomProperties() {
            AxiomConfig config = AxiomConfig.builder()
                    .withProperty("app.name", "test-app")
                    .withProperty("app.version", "1.0.0")
                    .build();

            assertThat(config.get("app.name")).contains("test-app");
            assertThat(config.get("app.version")).contains("1.0.0");
        }

        @Test
        @DisplayName("should build config with properties file")
        void buildsWithPropertiesFile(@TempDir Path tempDir) throws Exception {
            Path propsFile = tempDir.resolve("test.properties");
            Files.writeString(propsFile, """
                    db.host=localhost
                    db.port=5432
                    """);

            AxiomConfig config = AxiomConfig.builder()
                    .withPropertiesFile(propsFile)
                    .build();

            assertThat(config.get("db.host")).contains("localhost");
            assertThat(config.get("db.port")).contains("5432");
        }

        @Test
        @DisplayName("should throw for missing properties file")
        void throwsForMissingFile() {
            Path nonExistent = Path.of("/nonexistent/file.properties");

            assertThatThrownBy(() ->
                    AxiomConfig.builder()
                            .withPropertiesFile(nonExistent)
                            .build()
            ).isInstanceOf(ConfigException.class);
        }

        @Test
        @DisplayName("should build without default sources")
        void buildsWithoutDefaults() {
            AxiomConfig config = AxiomConfig.builder()
                    .withoutDefaultSources()
                    .withProperty("only.this", "value")
                    .build();

            assertThat(config.get("only.this")).contains("value");
            // System properties should NOT be present
            assertThat(config.get("java.version")).isEmpty();
        }

        @Test
        @DisplayName("should support profile loading")
        void supportsProfiles(@TempDir Path tempDir) throws Exception {
            Path baseFile = tempDir.resolve("application.properties");
            Files.writeString(baseFile, """
                    db.host=prod-db.example.com
                    """);

            Path devFile = tempDir.resolve("application-dev.properties");
            Files.writeString(devFile, """
                    db.host=localhost
                    """);

            AxiomConfig config = AxiomConfig.builder()
                    .withPropertiesFile(baseFile)
                    .withProfile("dev", devFile)
                    .build();

            // Dev profile should override base
            assertThat(config.get("db.host")).contains("localhost");
        }
    }

    @Nested
    @DisplayName("Type-safe mapping")
    class MappingTests {

        @Test
        @DisplayName("should map to interface with @ConfigMapping")
        void mapsToInterface() {
            AxiomConfig config = AxiomConfig.builder()
                    .withoutDefaultSources()
                    .withMapping(ServerConfig.class)
                    .withProperty("server.host", "0.0.0.0")
                    .withProperty("server.port", "9090")
                    .build();

            ServerConfig serverConfig = config.getMapping(ServerConfig.class);

            assertThat(serverConfig).isNotNull();
            assertThat(serverConfig.host()).isEqualTo("0.0.0.0");
            assertThat(serverConfig.port()).isEqualTo(9090);
        }

        @Test
        @DisplayName("should use default values from interface")
        void usesDefaultValues() {
            AxiomConfig config = AxiomConfig.builder()
                    .withoutDefaultSources()
                    .withMapping(ServerConfig.class)
                    .withProperty("server.host", "custom-host")
                    .build();

            ServerConfig serverConfig = config.getMapping(ServerConfig.class);

            assertThat(serverConfig.host()).isEqualTo("custom-host");
            assertThat(serverConfig.port()).isEqualTo(8080); // default
        }

        @Test
        @DisplayName("should map Optional fields")
        void mapsOptionalFields() {
            AxiomConfig config = AxiomConfig.builder()
                    .withoutDefaultSources()
                    .withMapping(ServerConfig.class)
                    .withProperty("server.host", "localhost")
                    .withProperty("server.port", "8080")
                    .build();

            ServerConfig serverConfig = config.getMapping(ServerConfig.class);

            assertThat(serverConfig.contextPath()).isEmpty();
        }

        @Test
        @DisplayName("should map nested configuration")
        void mapsNestedConfig() {
            AxiomConfig config = AxiomConfig.builder()
                    .withoutDefaultSources()
                    .withMapping(DatabaseConfig.class)
                    .withProperty("database.host", "db.example.com")
                    .withProperty("database.port", "5432")
                    .withProperty("database.pool.min-size", "5")
                    .withProperty("database.pool.max-size", "20")
                    .build();

            DatabaseConfig dbConfig = config.getMapping(DatabaseConfig.class);

            assertThat(dbConfig.host()).isEqualTo("db.example.com");
            assertThat(dbConfig.port()).isEqualTo(5432);
            assertThat(dbConfig.pool().minSize()).isEqualTo(5);
            assertThat(dbConfig.pool().maxSize()).isEqualTo(20);
        }
    }
}
