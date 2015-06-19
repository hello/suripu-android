package is.hello.sense.api.model;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class EnumsTests {
    @Test
    public void fromString() {
        assertEquals(TestEnum.VALUE1, TestEnum.fromString("VALUE1"));
        assertEquals(TestEnum.VALUE2, TestEnum.fromString("VALUE2"));
        assertEquals(TestEnum.VALUE3, TestEnum.fromString("VALUE3"));
        assertEquals(TestEnum.UNKNOWN, TestEnum.fromString("VALUE4"));
    }

    private enum TestEnum {
        VALUE1,
        VALUE2,
        VALUE3,
        UNKNOWN;

        @JsonCreator
        public static TestEnum fromString(@Nullable String value) {
            return Enums.fromString(value, values(), UNKNOWN);
        }
    }
}
