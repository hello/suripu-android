package is.hello.sense.units;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UnitConverterTests {
    @Test
    public void identity() {
        final UnitConverter converter = UnitConverter.IDENTITY;
        assertThat(converter.convert(42.0f), is(equalTo(42.0f)));
    }
}
