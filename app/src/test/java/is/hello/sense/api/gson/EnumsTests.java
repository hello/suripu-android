package is.hello.sense.api.gson;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class EnumsTests extends SenseTestCase {
    @Test
    public void fromString() {
        assertEquals(TestEnum.VALUE1, TestEnum.fromString("VALUE1"));
        assertEquals(TestEnum.VALUE2, TestEnum.fromString("VALUE2"));
        assertEquals(TestEnum.VALUE3, TestEnum.fromString("VALUE3"));
        assertEquals(TestEnum.UNKNOWN, TestEnum.fromString("VALUE4"));
    }

    @Test
    public void caseInsensitivity() {
        assertEquals(TestEnum.VALUE1, TestEnum.fromString("VALUE1"));
        assertEquals(TestEnum.VALUE1, TestEnum.fromString("value1"));
        assertEquals(TestEnum.VALUE1, TestEnum.fromString("Value1"));
        assertEquals(TestEnum.VALUE1, TestEnum.fromString("vALuE1"));
    }

    @Test
    public void serialization() {
        final Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Enums.FromString.class, new Enums.Serialization())
                .create();

        assertThat(gson.toJson(TestEnum.VALUE1), is(equalTo("\"VALUE1\"")));
    }

    @Test
    public void deserialization() {
        final Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Enums.FromString.class, new Enums.Serialization())
                .create();

        final TestEnum value1 = gson.fromJson("VALUE1", TestEnum.class);
        assertThat(value1, is(notNullValue()));
        assertThat(value1, is(equalTo(TestEnum.VALUE1)));

        final TestEnum nullValue = gson.fromJson("null", TestEnum.class);
        assertThat(nullValue, is(nullValue()));
    }

    private enum TestEnum implements Enums.FromString {
        VALUE1,
        VALUE2,
        VALUE3,
        UNKNOWN;

        public static TestEnum fromString(@Nullable String value) {
            return Enums.fromString(value, values(), UNKNOWN);
        }
    }
}
