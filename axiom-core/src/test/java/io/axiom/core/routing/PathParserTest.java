package io.axiom.core.routing;

import java.util.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

@DisplayName("PathParser")
class PathParserTest {

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses empty path to empty list")
        void parsesEmptyPath() {
            final List<Segment> segments = PathParser.parse("");
            Assertions.assertThat(segments).isEmpty();
        }

        @Test
        @DisplayName("parses static segments")
        void parsesStaticSegments() {
            final List<Segment> segments = PathParser.parse("/users/list");
            Assertions.assertThat(segments).hasSize(2);
            Assertions.assertThat(segments.get(0)).isInstanceOf(StaticSegment.class);
        }

        @Test
        @DisplayName("parses parameter segments")
        void parsesParameterSegments() {
            final List<Segment> segments = PathParser.parse("/users/:id");
            Assertions.assertThat(segments).hasSize(2);
            Assertions.assertThat(segments.get(1)).isInstanceOf(ParamSegment.class);
        }

        @Test
        @DisplayName("parses wildcard segments")
        void parsesWildcardSegments() {
            final List<Segment> segments = PathParser.parse("/files/*");
            Assertions.assertThat(segments).hasSize(2);
            Assertions.assertThat(segments.get(1)).isInstanceOf(WildcardSegment.class);
        }

        @Test
        @DisplayName("rejects null path")
        void rejectsNullPath() {
            Assertions.assertThatThrownBy(() -> PathParser.parse(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("normalize()")
    class Normalize {

        @Test
        @DisplayName("strips leading slash")
        void stripsLeadingSlash() {
            final String result = PathParser.normalize("/users");
            Assertions.assertThat(result).isEqualTo("users");
        }

        @Test
        @DisplayName("strips trailing slash")
        void stripsTrailingSlash() {
            final String result = PathParser.normalize("users/");
            Assertions.assertThat(result).isEqualTo("users");
        }

        @Test
        @DisplayName("root path becomes empty string")
        void rootPathBecomesEmpty() {
            final String result = PathParser.normalize("/");
            Assertions.assertThat(result).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("join()")
    class Join {

        @Test
        @DisplayName("joins base and path with slash")
        void joinsBaseAndPath() {
            final String result = PathParser.join("/api", "/users");
            Assertions.assertThat(result).isEqualTo("/api/users");
        }

        @Test
        @DisplayName("handles empty base")
        void handlesEmptyBase() {
            final String result = PathParser.join("", "/users");
            Assertions.assertThat(result).isEqualTo("/users");
        }
    }
}
