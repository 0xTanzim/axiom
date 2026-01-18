package io.axiom.config;

import java.time.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

/**
 * Comprehensive tests for Config - the simplified static configuration API.
 * Tests all edge cases including type conversion, record binding, and error handling.
 */
@DisplayName("Config (Static API)")
class ConfigTest {

    @BeforeEach
    void setUp() {
        Config.reset();
    }

    @AfterEach
    void tearDown() {
        Config.reset();
    }

    @Nested
    @DisplayName("Static get methods")
    class GetTests {

        @Test
        @DisplayName("should get string value with default")
        void getsStringWithDefault() {
            Config.init(AxiomConfig.parse("app.name=MyApp"));

            Assertions.assertThat(Config.get("app.name", "default")).isEqualTo("MyApp");
            Assertions.assertThat(Config.get("missing.key", "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("should get int value with default")
        void getsIntWithDefault() {
            Config.init(AxiomConfig.parse("server.port=9090"));

            Assertions.assertThat(Config.get("server.port", 8080)).isEqualTo(9090);
            Assertions.assertThat(Config.get("missing.port", 8080)).isEqualTo(8080);
        }

        @Test
        @DisplayName("should get boolean value with default")
        void getsBooleanWithDefault() {
            Config.init(AxiomConfig.parse("app.debug=true"));

            Assertions.assertThat(Config.get("app.debug", false)).isTrue();
            Assertions.assertThat(Config.get("missing.debug", false)).isFalse();
        }

        @Test
        @DisplayName("should get long value with default")
        void getsLongWithDefault() {
            Config.init(AxiomConfig.parse("app.maxSize=1000000000"));

            Assertions.assertThat(Config.get("app.maxSize", 0L)).isEqualTo(1_000_000_000L);
            Assertions.assertThat(Config.get("missing.size", 100L)).isEqualTo(100L);
        }

        @Test
        @DisplayName("should get Duration value with default")
        void getsDurationWithDefault() {
            Config.init(AxiomConfig.parse("server.timeout=30s"));

            Assertions.assertThat(Config.get("server.timeout", Duration.ofSeconds(10)))
                .isEqualTo(Duration.ofSeconds(30));
            Assertions.assertThat(Config.get("missing.timeout", Duration.ofSeconds(10)))
                .isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("should throw for required missing key")
        void throwsForRequiredMissing() {
            Config.init(AxiomConfig.parse(""));

            Assertions.assertThatThrownBy(() -> Config.get("missing.required"))
                .isInstanceOf(ConfigException.Missing.class)
                .hasMessageContaining("missing.required");
        }
    }

    @Nested
    @DisplayName("Edge cases for type conversion")
    class TypeConversionEdgeCases {

        @Test
        @DisplayName("should handle empty string value as missing")
        void handlesEmptyStringValue() {
            Config.init(AxiomConfig.parse("app.name="));

            // SmallRye treats empty values as "not present", so default is returned
            Assertions.assertThat(Config.get("app.name", "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("should handle whitespace in values")
        void handlesWhitespaceInValues() {
            Config.init(AxiomConfig.parse("app.name=  spaced value  "));

            // Properties typically trim whitespace
            Assertions.assertThat(Config.get("app.name", "default")).contains("spaced");
        }

        @Test
        @DisplayName("should parse zero values correctly")
        void parsesZeroValues() {
            Config.init(AxiomConfig.parse("count=0"));

            Assertions.assertThat(Config.get("count", 999)).isEqualTo(0);
        }

        @Test
        @DisplayName("should parse negative integers")
        void parsesNegativeIntegers() {
            Config.init(AxiomConfig.parse("offset=-100"));

            Assertions.assertThat(Config.get("offset", 0)).isEqualTo(-100);
        }

        @Test
        @DisplayName("should handle large long values")
        void handlesLargeLongValues() {
            Config.init(AxiomConfig.parse("bignum=9223372036854775807"));

            Assertions.assertThat(Config.get("bignum", 0L)).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("should parse false boolean variations")
        void parsesFalseBooleanVariations() {
            Config.init(AxiomConfig.parse("flag=false"));

            Assertions.assertThat(Config.get("flag", true)).isFalse();
        }

        @Test
        @DisplayName("should parse boolean as false for non-true values")
        void parsesBooleanNonTrueAsFalse() {
            Config.init(AxiomConfig.parse("flag=yes"));

            // Java's Boolean.parseBoolean only accepts "true" as true
            Assertions.assertThat(Config.get("flag", true)).isFalse();
        }

        @Test
        @DisplayName("should parse duration in milliseconds")
        void parsesDurationInMilliseconds() {
            Config.init(AxiomConfig.parse("timeout=500ms"));

            Assertions.assertThat(Config.get("timeout", Duration.ZERO))
                .isEqualTo(Duration.ofMillis(500));
        }

        @Test
        @DisplayName("should parse duration in minutes")
        void parsesDurationInMinutes() {
            Config.init(AxiomConfig.parse("timeout=5m"));

            Assertions.assertThat(Config.get("timeout", Duration.ZERO))
                .isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("should parse duration in hours")
        void parsesDurationInHours() {
            Config.init(AxiomConfig.parse("timeout=2h"));

            Assertions.assertThat(Config.get("timeout", Duration.ZERO))
                .isEqualTo(Duration.ofHours(2));
        }

        @Test
        @DisplayName("should parse duration in days")
        void parsesDurationInDays() {
            Config.init(AxiomConfig.parse("retention=7d"));

            Assertions.assertThat(Config.get("retention", Duration.ZERO))
                .isEqualTo(Duration.ofDays(7));
        }

        @Test
        @DisplayName("should parse ISO-8601 duration format")
        void parsesIsoDurationFormat() {
            Config.init(AxiomConfig.parse("timeout=PT30S"));

            Assertions.assertThat(Config.get("timeout", Duration.ZERO))
                .isEqualTo(Duration.ofSeconds(30));
        }
    }

    @Nested
    @DisplayName("Optional value access")
    class OptionalValueTests {

        @Test
        @DisplayName("should return Optional.empty for missing key")
        void returnsEmptyForMissingKey() {
            Config.init(AxiomConfig.parse(""));

            Assertions.assertThat(Config.getOptional("missing")).isEmpty();
        }

        @Test
        @DisplayName("should return Optional with value for existing key")
        void returnsValueForExistingKey() {
            Config.init(AxiomConfig.parse("app.name=MyApp"));

            Assertions.assertThat(Config.getOptional("app.name")).contains("MyApp");
        }
    }

    @Nested
    @DisplayName("Record binding")
    class BindTests {

        record SimpleConfig(String host, int port) {}

        record ConfigWithDefaults(String name) {}

        record ConfigWithAllTypes(
            String name,
            int count,
            long bigNumber,
            boolean enabled,
            double ratio
        ) {}

        record NestedKeyConfig(String poolSize, String maxWait) {}

        @Test
        @DisplayName("should bind to simple record")
        void bindsToSimpleRecord() {
            Config.init(AxiomConfig.parse("""
                server.host=localhost
                server.port=8080
                """));

            final SimpleConfig server = Config.bind("server", SimpleConfig.class);

            Assertions.assertThat(server.host()).isEqualTo("localhost");
            Assertions.assertThat(server.port()).isEqualTo(8080);
        }

        @Test
        @DisplayName("should throw for missing record fields")
        void throwsForMissingFields() {
            Config.init(AxiomConfig.parse("server.host=localhost"));

            Assertions.assertThatThrownBy(() -> Config.bind("server", SimpleConfig.class))
                .isInstanceOf(ConfigException.Missing.class)
                .hasMessageContaining("port");
        }

        @Test
        @DisplayName("should bind record with multiple types")
        void bindsRecordWithMultipleTypes() {
            Config.init(AxiomConfig.parse("""
                app.name=TestApp
                app.count=42
                app.big-number=9999999999
                app.enabled=true
                app.ratio=3.14
                """));

            final ConfigWithAllTypes app = Config.bind("app", ConfigWithAllTypes.class);

            Assertions.assertThat(app.name()).isEqualTo("TestApp");
            Assertions.assertThat(app.count()).isEqualTo(42);
            Assertions.assertThat(app.bigNumber()).isEqualTo(9999999999L);
            Assertions.assertThat(app.enabled()).isTrue();
            Assertions.assertThat(app.ratio()).isEqualTo(3.14);
        }

        @Test
        @DisplayName("should handle kebab-case property names")
        void handlesKebabCasePropertyNames() {
            Config.init(AxiomConfig.parse("""
                db.pool-size=10
                db.max-wait=5000
                """));

            final NestedKeyConfig db = Config.bind("db", NestedKeyConfig.class);

            Assertions.assertThat(db.poolSize()).isEqualTo("10");
            Assertions.assertThat(db.maxWait()).isEqualTo("5000");
        }
    }

    @Nested
    @DisplayName("Pre-built configs")
    class PreBuiltConfigTests {

        @Test
        @DisplayName("should provide server config with defaults")
        void providesServerConfigWithDefaults() {
            Config.init(AxiomConfig.parse(""));

            final Config.ServerConfig server = Config.server();

            Assertions.assertThat(server.host()).isEqualTo("0.0.0.0");
            Assertions.assertThat(server.port()).isEqualTo(8080);
            Assertions.assertThat(server.contextPath()).isEqualTo("/");
        }

        @Test
        @DisplayName("should provide server config with custom values")
        void providesServerConfigWithCustomValues() {
            Config.init(AxiomConfig.parse("""
                server.host=127.0.0.1
                server.port=3000
                server.contextPath=/api
                """));

            final Config.ServerConfig server = Config.server();

            Assertions.assertThat(server.host()).isEqualTo("127.0.0.1");
            Assertions.assertThat(server.port()).isEqualTo(3000);
            Assertions.assertThat(server.contextPath()).isEqualTo("/api");
        }

        @Test
        @DisplayName("should provide database config")
        void providesDatabaseConfig() {
            Config.init(AxiomConfig.parse("""
                database.url=jdbc:postgresql://localhost/mydb
                database.username=admin
                database.password=secret
                database.poolSize=20
                """));

            final Config.DatabaseConfig db = Config.database();

            Assertions.assertThat(db.url()).isEqualTo("jdbc:postgresql://localhost/mydb");
            Assertions.assertThat(db.username()).isEqualTo("admin");
            Assertions.assertThat(db.password()).isEqualTo("secret");
            Assertions.assertThat(db.poolSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("should provide database config with defaults for optional fields")
        void providesDatabaseConfigWithDefaults() {
            Config.init(AxiomConfig.parse("database.url=jdbc:h2:mem:test"));

            final Config.DatabaseConfig db = Config.database();

            Assertions.assertThat(db.url()).isEqualTo("jdbc:h2:mem:test");
            Assertions.assertThat(db.username()).isEmpty();
            Assertions.assertThat(db.password()).isEmpty();
            Assertions.assertThat(db.poolSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("should throw for missing required database.url")
        void throwsForMissingDatabaseUrl() {
            Config.init(AxiomConfig.parse("database.username=admin"));

            Assertions.assertThatThrownBy(() -> Config.database())
                .isInstanceOf(ConfigException.Missing.class)
                .hasMessageContaining("database.url");
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("should report isLoaded correctly")
        void reportsIsLoaded() {
            Assertions.assertThat(Config.isLoaded()).isFalse();

            Config.init(AxiomConfig.parse(""));

            Assertions.assertThat(Config.isLoaded()).isTrue();
        }

        @Test
        @DisplayName("should auto-load on first access")
        void autoLoadsOnFirstAccess() {
            // Don't call init, just access
            final String value = Config.get("nonexistent", "default");

            Assertions.assertThat(Config.isLoaded()).isTrue();
            Assertions.assertThat(value).isEqualTo("default");
        }

        @Test
        @DisplayName("should be thread-safe for concurrent access")
        void isThreadSafeForConcurrentAccess() throws Exception {
            Config.init(AxiomConfig.parse("shared.value=test"));

            final var threads = new Thread[10];
            final var results = new String[10];

            for (int i = 0; i < 10; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    results[index] = Config.get("shared.value", "failed");
                });
            }

            for (final Thread t : threads) t.start();
            for (final Thread t : threads) t.join();

            for (final String result : results) {
                Assertions.assertThat(result).isEqualTo("test");
            }
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should show helpful error message for missing key")
        void showsHelpfulErrorForMissingKey() {
            Config.init(AxiomConfig.parse(""));

            Assertions.assertThatThrownBy(() -> Config.get("my.missing.key"))
                .isInstanceOf(ConfigException.Missing.class)
                .hasMessageContaining("my.missing.key")
                .hasMessageContaining("application.properties")
                .hasMessageContaining("MY_MISSING_KEY");
        }

        @Test
        @DisplayName("should show helpful error for wrong type")
        void showsHelpfulErrorForWrongType() {
            Config.init(AxiomConfig.parse("port=not-a-number"));

            Assertions.assertThatThrownBy(() -> Config.get("port", 8080))
                .isInstanceOf(ConfigException.WrongType.class)
                .hasMessageContaining("port")
                .hasMessageContaining("Integer")
                .hasMessageContaining("not-a-number");
        }
    }

    @Nested
    @DisplayName("Complex key names")
    class ComplexKeyNameTests {

        @Test
        @DisplayName("should handle deeply nested keys")
        void handlesDeeplyNestedKeys() {
            Config.init(AxiomConfig.parse("a.b.c.d.e.f=deep"));

            Assertions.assertThat(Config.get("a.b.c.d.e.f", "none")).isEqualTo("deep");
        }

        @Test
        @DisplayName("should handle keys with numbers")
        void handlesKeysWithNumbers() {
            Config.init(AxiomConfig.parse("server1.port=8081"));

            Assertions.assertThat(Config.get("server1.port", 0)).isEqualTo(8081);
        }

        @Test
        @DisplayName("should handle keys with hyphens")
        void handlesKeysWithHyphens() {
            Config.init(AxiomConfig.parse("my-app.feature-flag=true"));

            Assertions.assertThat(Config.get("my-app.feature-flag", false)).isTrue();
        }
    }

    @Nested
    @DisplayName("Special values")
    class SpecialValueTests {

        @Test
        @DisplayName("should handle URL values with special characters")
        void handlesUrlValues() {
            Config.init(AxiomConfig.parse("db.url=jdbc:postgresql://user:pass@localhost:5432/db?ssl=true"));

            Assertions.assertThat(Config.get("db.url", ""))
                .isEqualTo("jdbc:postgresql://user:pass@localhost:5432/db?ssl=true");
        }

        @Test
        @DisplayName("should handle values with equals sign")
        void handlesValuesWithEqualsSign() {
            Config.init(AxiomConfig.parse("query=a=1&b=2"));

            Assertions.assertThat(Config.get("query", "")).isEqualTo("a=1&b=2");
        }

        @Test
        @DisplayName("should handle unicode values")
        void handlesUnicodeValues() {
            Config.init(AxiomConfig.parse("greeting=Hello 世界"));

            Assertions.assertThat(Config.get("greeting", "")).isEqualTo("Hello 世界");
        }
    }
}
