package is.hello.sense.api.model;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;

import junit.framework.TestCase;

public class EnumsTests extends TestCase {

    public void testFromString() {
        assertEquals(Test.VALUE1, Test.fromString("VALUE1"));
        assertEquals(Test.VALUE2, Test.fromString("VALUE2"));
        assertEquals(Test.VALUE3, Test.fromString("VALUE3"));
        assertEquals(Test.UNKNOWN, Test.fromString("VALUE4"));
    }

    private static enum Test {
        VALUE1,
        VALUE2,
        VALUE3,
        UNKNOWN;

        @JsonCreator
        public static Test fromString(@Nullable String value) {
            return Enums.fromString(value, values(), UNKNOWN);
        }
    }
}
