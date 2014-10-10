package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.fasterxml.jackson.annotation.JsonCreator;

import is.hello.sense.R;
import is.hello.sense.api.model.Enums;

public class Units {
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
    
    
    public static enum System {
        US_CUSTOMARY(R.string.unit_suffix_mass_us, R.string.unit_suffix_distance_us, R.string.unit_suffix_temperature_us) {
            @Override
            public long convertMass(long mass) {
                return gramsToPounds(mass);
            }

            @Override
            public long convertDistance(long distance) {
                return centimetersToInches(distance);
            }

            @Override
            public long convertTemperature(long temperature) {
                return celsiusToFahrenheit(temperature);
            }
        },
        METRIC(R.string.unit_suffix_mass_metric, R.string.unit_suffix_distance_metric, R.string.unit_suffix_temperature_metric) {
            @Override
            public long convertMass(long mass) {
                return mass;
            }

            @Override
            public long convertDistance(long distance) {
                return distance;
            }

            @Override
            public long convertTemperature(long temperature) {
                return temperature;
            }
        };

        public abstract long convertMass(long mass);
        public abstract long convertDistance(long distance);
        public abstract long convertTemperature(long temperature);


        public final @StringRes int massSuffixRes;
        public final @StringRes int distanceSuffixRes;
        public final @StringRes int temperatureSuffixRes;

        private System(@StringRes int massSuffixRes, @StringRes int distanceSuffixRes, @StringRes int temperatureSuffixRes) {
            this.massSuffixRes = massSuffixRes;
            this.distanceSuffixRes = distanceSuffixRes;
            this.temperatureSuffixRes = temperatureSuffixRes;
        }

        @JsonCreator
        public static System fromString(@NonNull String string) {
            return Enums.fromString(string, values(), US_CUSTOMARY);
        }
    }
}
