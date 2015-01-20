package is.hello.sense.units;

public class UnitOperations {
    public static int poundsToGrams(int pounds) {
        return Math.round(pounds / 0.0022046f);
    }

    public static int gramsToPounds(int grams) {
        return Math.round(grams * 0.0022046f);
    }

    public static int kilogramsToPounds(int kilograms) {
        return Math.round(kilograms * 2.20462f);
    }

    public static int inchesToCentimeters(int inches) {
        return Math.round(inches / 0.39370f);
    }

    public static long centimetersToInches(long inches) {
        return Math.round(inches * 0.39370);
    }

    public static long celsiusToFahrenheit(long temperature) {
        return Math.round((temperature * 1.8) + 32.0);
    }
}
