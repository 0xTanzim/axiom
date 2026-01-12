package io.axiom.core.routing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

@DisplayName("Segment")
class SegmentTest {

    @Nested
    @DisplayName("StaticSegment")
    class StaticSegmentTests {

        @Test
        @DisplayName("stores value correctly")
        void storesValueCorrectly() {
            final StaticSegment segment = new StaticSegment("users");
            Assertions.assertThat(segment.value()).isEqualTo("users");
        }

        @Test
        @DisplayName("rejects null value")
        void rejectsNullValue() {
            Assertions.assertThatThrownBy(() -> new StaticSegment(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects empty value")
        void rejectsEmptyValue() {
            Assertions.assertThatThrownBy(() -> new StaticSegment(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("ParamSegment")
    class ParamSegmentTests {

        @Test
        @DisplayName("stores name and generates value with colon prefix")
        void storesNameAndGeneratesValue() {
            final ParamSegment segment = new ParamSegment("id");
            Assertions.assertThat(segment.name()).isEqualTo("id");
            Assertions.assertThat(segment.value()).isEqualTo(":id");
        }

        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            Assertions.assertThatThrownBy(() -> new ParamSegment(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects empty name")
        void rejectsEmptyName() {
            Assertions.assertThatThrownBy(() -> new ParamSegment(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("WildcardSegment")
    class WildcardSegmentTests {

        @Test
        @DisplayName("has wildcard value")
        void hasWildcardValue() {
            final WildcardSegment segment = WildcardSegment.INSTANCE;
            Assertions.assertThat(segment.value()).isEqualTo("*");
        }

        @Test
        @DisplayName("is singleton")
        void isSingleton() {
            Assertions.assertThat(WildcardSegment.INSTANCE).isSameAs(WildcardSegment.INSTANCE);
        }
    }
}
