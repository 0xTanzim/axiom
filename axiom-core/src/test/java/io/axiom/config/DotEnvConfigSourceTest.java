package io.axiom.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DotEnvConfigSource - .env file loading.
 */
@DisplayName("DotEnvConfigSource")
class DotEnvConfigSourceTest {

    @Nested
    @DisplayName("parseDotEnv")
    class ParseTests {

        @Test
        @DisplayName("should parse simple key=value pairs")
        void parsesSimpleKeyValue(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """
                    DATABASE_URL=postgres://localhost/mydb
                    SECRET_KEY=mysecret
                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            // Keys are normalized to lowercase.dot.notation
            assertThat(source.getValue("database.url")).isEqualTo("postgres://localhost/mydb");
            assertThat(source.getValue("secret.key")).isEqualTo("mysecret");
        }

        @Test
        @DisplayName("should skip comments")
        void skipsComments(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """
                    # This is a comment
                    MY_KEY=myvalue
                    # Another comment
                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            assertThat(source.getValue("my.key")).isEqualTo("myvalue");
            assertThat(source.getProperties()).hasSize(1);
        }

        @Test
        @DisplayName("should skip empty lines")
        void skipsEmptyLines(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """

                    MY_KEY=value1

                    OTHER_KEY=value2

                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            assertThat(source.getProperties()).hasSize(2);
        }

        @Test
        @DisplayName("should handle quoted values - double quotes")
        void handlesDoubleQuotes(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """
                    MY_KEY="quoted value with spaces"
                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            assertThat(source.getValue("my.key")).isEqualTo("quoted value with spaces");
        }

        @Test
        @DisplayName("should handle quoted values - single quotes")
        void handlesSingleQuotes(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """
                    MY_KEY='single quoted value'
                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            assertThat(source.getValue("my.key")).isEqualTo("single quoted value");
        }

        @Test
        @DisplayName("should handle keys with dots")
        void handlesKeysWithDots(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """
                    server.host=localhost
                    server.port=8080
                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            assertThat(source.getValue("server.host")).isEqualTo("localhost");
            assertThat(source.getValue("server.port")).isEqualTo("8080");
        }

        @Test
        @DisplayName("should return empty map for non-existent file")
        void handlesNonExistentFile(@TempDir Path tempDir) {
            Path nonExistent = tempDir.resolve(".env.does.not.exist");

            DotEnvConfigSource source = new DotEnvConfigSource(nonExistent);

            assertThat(source.getProperties()).isEmpty();
        }

        @Test
        @DisplayName("should skip lines without equals sign")
        void skipsInvalidLines(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """
                    VALID_KEY=value
                    invalid line without equals
                    ANOTHER_KEY=another
                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            assertThat(source.getProperties()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("ordinal")
    class OrdinalTests {

        @Test
        @DisplayName("should have ordinal 295 - between properties and env vars")
        void hasCorrectOrdinal(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, "KEY=value");

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            // .env should be:
            // - Higher than application.properties (250)
            // - Lower than environment variables (300)
            assertThat(source.getOrdinal()).isEqualTo(295);
        }
    }

    @Nested
    @DisplayName("key normalization")
    class KeyNormalizationTests {

        @Test
        @DisplayName("should convert UPPER_CASE to lower.case")
        void normalizesUpperCase(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """
                    DATABASE_HOST=localhost
                    DATABASE_PORT=5432
                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            assertThat(source.getValue("database.host")).isEqualTo("localhost");
            assertThat(source.getValue("database.port")).isEqualTo("5432");
        }

        @Test
        @DisplayName("should preserve lowercase keys with dots")
        void preservesLowercaseDotKeys(@TempDir Path tempDir) throws Exception {
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, """
                    my.custom.key=value
                    """);

            DotEnvConfigSource source = new DotEnvConfigSource(envFile);

            assertThat(source.getValue("my.custom.key")).isEqualTo("value");
        }
    }
}
