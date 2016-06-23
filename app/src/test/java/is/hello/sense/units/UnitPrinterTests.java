package is.hello.sense.units;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UnitPrinterTests {
    @Test
    public void simple() {
        IUnitPrinter printer = UnitPrinter.SIMPLE;
        assertThat(printer.print(42.0), is(equalTo("42")));
    }
}
