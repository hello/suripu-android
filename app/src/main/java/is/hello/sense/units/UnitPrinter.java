package is.hello.sense.units;

import java.util.Locale;

public class UnitPrinter {

    public static IUnitPrinter SIMPLE = v -> String.format(Locale.getDefault(), "%.0f", v);
}
