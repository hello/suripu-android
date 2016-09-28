package is.hello.sense.units;

public class UnitOperations {
    public static long poundsToGrams(final long pounds) {
        return Math.round(pounds / 0.00220462);
    }

    public static long poundsToKilograms(final long pounds) {
        return poundsToGrams(pounds) / 1000L;
    }

    public static long gramsToPounds(final long grams) {
        return Math.round(grams * 0.00220462);
    }

    public static long gramsToKilograms(final long grams) {
        return grams / 1000L;
    }

    public static long centimetersToInches(final long inches) {
        return Math.round(inches * 0.39370);
    }

    public static double celsiusToFahrenheit(final double temperature) {
        return Math.round((temperature * 1.8) + 32.0);
    }

    public static float celsiusToFahrenheit(final float temperature) {
        return (temperature * 1.8f) + 32.f;
    }
}
