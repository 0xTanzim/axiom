package io.axiom.core.routing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Segment")
class SegmentTest {

    @Nested
    @DisplayName("StaticSegment")
    class StaticSegmentTests {

        @Test
        @DisplayName("stores value correctly")
        void storesValueCorrectly() {
            StaticSegment segment = new StaticSegment("users");
            assertThat(segment.value()).isEqualTo("users");
        }

        @Test
        @DisplayName("rejects null value")
        void rejectsNullValue() {
            assertThatThrownBy(() -> new StaticSegment(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects empty value")
        void rejectsEmptyValue() {
            assertThatThrownBy(() -> new StaticSegment(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("ParamSegment")
    class ParamSegmentTests {

        @Test
        @DisplayName("stores name and generates value with colon prefix")
        void storesNameAndGeneratesValue() {
            ParamSegment segment = new ParamSegment("id");
            assertThat(segment.name()).isEqualTo("id");
            assertThat(segment.value()).isEqualTo(":id");
        }

        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            assertThatThrownBy(() -> new ParamSegment(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects empty name")
        void rejectsEmptyName() {
            assertThatThrownBy(() -> new ParamSegment(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("WildcardSegment")
    class WildcardSegmentTests {

        @Test
        @DisplayName("has wildcard value")
        void hasWildcardValue() {
            WildcardSegment segment = WildcardSegment.INSTANCE;
            assertThat(segment.value()).isEqualTo("*");
        }

        @Test
        @DisplayName("is singleton")
        void isSingleton() {
            assertThat(WildcardSegment.INSTANCE).isSameAs(WildcardSegment.INSTANCE);
        }
    }
}
