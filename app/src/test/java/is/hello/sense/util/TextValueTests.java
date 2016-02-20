package is.hello.sense.util;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.ui.widget.util.Styles;

import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;

public class TextValueTests extends SenseTestCase {
    @Test
    public void testValues() {
        String value;
        final float value1 = 10.01f;
        value = Styles.createTextValue(value1, 0);
        assertThat(value, is("10"));
        value = Styles.createTextValue(value1, 1);
        assertThat(value, is("10.0"));
        value = Styles.createTextValue(value1, 2);
        assertThat(value, is("10.01"));
        value = Styles.createTextValue(value1, 3);
        assertThat(value, is("10.010"));
        final float value2 = 9.80f;
        value = Styles.createTextValue(value2, 0);
        assertThat(value, is("10"));
        value = Styles.createTextValue(value2, 1);
        assertThat(value, is("9.8"));
        value = Styles.createTextValue(value2, 2);
        assertThat(value, is("9.80"));
        value = Styles.createTextValue(value2, 3);
        assertThat(value, is("9.800"));
        final float value3= 12.34f;
        value = Styles.createTextValue(value3, 0);
        assertThat(value, is("12"));
        value = Styles.createTextValue(value3, 1);
        assertThat(value, is("12.3"));
        value = Styles.createTextValue(value3, 2);
        assertThat(value, is("12.34"));
        value = Styles.createTextValue(value3, 3);
        assertThat(value, is("12.340"));
        final float value4= 03.00f;
        value = Styles.createTextValue(value4, 0);
        assertThat(value, is("3"));
        value = Styles.createTextValue(value4, 1);
        assertThat(value, is("3.0"));
        value = Styles.createTextValue(value4, 2);
        assertThat(value, is("3.00"));
        value = Styles.createTextValue(value4, 3);
        assertThat(value, is("3.000"));
    }
}
