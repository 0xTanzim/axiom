package io.axiom.core.routing;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PathParser")
class PathParserTest {

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses empty path to empty list")
        void parsesEmptyPath() {
            List<Segment> segments = PathParser.parse("");
            assertThat(segments).isEmpty();
        }

        @Test
        @DisplayName("parses static segments")
        void parsesStaticSegments() {
            List<Segment> segments = PathParser.parse("/users/list");
            assertThat(segments).hasSize(2);
            assertThat(segments.get(0)).isInstanceOf(StaticSegment.class);
        }

        @Test
        @DisplayName("parses parameter segments")
        void parsesParameterSegments() {
            List<Segment> segments = PathParser.parse("/users/:id");
            assertThat(segments).hasSize(2);
            assertThat(segments.get(1)).isInstanceOf(ParamSegment.class);
        }

        @Test
        @DisplayName("parses wildcard segments")
        void parsesWildcardSegments() {
            List<Segment> segments = PathParser.parse("/files/*");
            assertThat(segments).hasSize(2);
            assertThat(segments.get(1)).isInstanceOf(WildcardSegment.class);
        }

        @Test
        @DisplayName("rejects null path")
        void rejectsNullPath() {
            assertThatThrownBy(() -> PathParser.parse(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("normalize()")
    class Normalize {

        @Test
        @DisplayName("strips leading slash")
        void stripsLeadingSlash() {
            String result = PathParser.normalize("/users");
            assertThat(result).isEqualTo("users");
        }

        @Test
        @DisplayName("strips trailing slash")
        void stripsTrailingSlash() {
            String result = PathParser.normalize("users/");
            assertThat(result).isEqualTo("users");
        }

        @Test
        @DisplayName("root path becomes empty string")
        void rootPathBecomesEmpty() {
            String result = PathParser.normalize("/");
            assertThat(result).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("join()")
    class Join {

        @Test
        @DisplayName("joins base and path with slash")
        void joinsBaseAndPath() {
            String result = PathParser.join("/api", "/users");
            assertThat(result).isEqualTo("/api/users");
        }

        @Test
        @DisplayName("handles empty base")
        void handlesEmptyBase() {
            String result = PathParser.join("", "/users");
            assertThat(result).isEqualTo("/users");
        }
    }
}
