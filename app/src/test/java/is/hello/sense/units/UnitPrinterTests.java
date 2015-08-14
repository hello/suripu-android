package is.hello.sense.units;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UnitPrinterTests {
    @Test
    public void simple() {
        UnitPrinter printer = UnitPrinter.SIMPLE;
        assertThat(printer.print(42), is(equalTo("42")));
    }
}
