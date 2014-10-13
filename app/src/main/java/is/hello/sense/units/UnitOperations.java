package is.hello.sense.units;

public class UnitOperations {
    public static long poundsToGrams(long pounds) {
        return Math.round(pounds / 0.0022046);
    }

    public static long gramsToPounds(long grams) {
        return Math.round(grams * 0.0022046);
    }

    public static long inchesToCentimeters(long inches) {
        return Math.round(inches / 0.39370);
    }

    public static long centimetersToInches(long inches) {
        return Math.round(inches * 0.39370);
    }

    public static long celsiusToFahrenheit(long temperature) {
        return Math.round((temperature * 1.8) + 32.0);
    }
}
