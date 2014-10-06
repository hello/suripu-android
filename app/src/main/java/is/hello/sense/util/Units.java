package is.hello.sense.util;

public class Units {
    public static int poundsToGrams(int pounds) {
        return (int) Math.round((float) pounds / 0.0022046);
    }

    public static int gramsToPounds(int grams) {
        return (int) Math.round((float) grams * 0.0022046);
    }

    public static int inchesToCentimeters(int inches) {
        return (int) Math.round((float)inches / 0.39370);
    }

    public static int centimetersToInches(int inches) {
        return (int) Math.round((float)inches * 0.39370);
    }
}
