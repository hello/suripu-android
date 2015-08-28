package is.hello.sense.units;

public class UnitOperations {
    public static long poundsToGrams(long pounds) {
        return Math.round(pounds / 0.0022046);
    }

    public static long poundsToKilograms(long kilograms) {
        return Math.round(kilograms / 2.20462);
    }

    public static long gramsToPounds(long grams) {
        return Math.round(grams * 0.0022046);
    }

    public static long gramsToKilograms(long grams) {
        return grams / 1000L;
    }

    public static long centimetersToInches(long inches) {
        return Math.round(inches * 0.39370);
    }

    public static double celsiusToFahrenheit(double temperature) {
        return Math.round((temperature * 1.8) + 32.0);
    }
}
