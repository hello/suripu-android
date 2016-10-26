package is.hello.sense.units;

import android.support.annotation.Nullable;

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

    public static Float celsiusToFahrenheit(@Nullable final Float temperature) {
        if (temperature == null) {
            return null;
        } else {
            return (temperature * 1.8f) + 32.f;
        }
    }

    /**
     * @param percentage must be between 0 - 100 %
     * @return converted value based on number of levels
     */
    public static int percentageToLevel(final int percentage, final int levels){
        // uses positive offset to adjust for server response decrementing percentage
        return (int) Math.floor(( (percentage+2) / 100f) * levels);
    }

    /**
     * @return converted value between 0 and 100
     */
    public static int levelToPercentage(final int selectedLevel, final int levels){
        return (int) Math.ceil((selectedLevel * 100f) / levels);
    }
}
