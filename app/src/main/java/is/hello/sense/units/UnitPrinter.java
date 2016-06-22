package is.hello.sense.units;

public class UnitPrinter {

    public static IUnitPrinter SIMPLE = v -> String.format("%.0f", v);
}
