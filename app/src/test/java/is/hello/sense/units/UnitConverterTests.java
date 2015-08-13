package is.hello.sense.units;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UnitConverterTests {
    @Test
    public void identity() {
        UnitConverter converter = UnitConverter.IDENTITY;
        assertThat(converter.convert(42L), is(equalTo(42L)));
    }
}
