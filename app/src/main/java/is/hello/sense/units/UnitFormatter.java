package is.hello.sense.units;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.widget.util.Styles;
import rx.Observable;

public class UnitFormatter {
    public static final String UNIT_SUFFIX_TEMPERATURE = "°";
    public static final String UNIT_SUFFIX_LIGHT = "lux";
    public static final String UNIT_SUFFIX_HUMIDITY = "%";
    public static final String UNIT_SUFFIX_AIR_QUALITY = "µg/m³";
    public static final String UNIT_SUFFIX_NOISE = "dB";

    // Used by PreferencesInteractor
    @Deprecated
    public static final String LEGACY_UNIT_SYSTEM_METRIC = "Metric";
    @Deprecated
    public static final String LEGACY_UNIT_SYSTEM_US_CUSTOMARY = "UsCustomary";


    private final PreferencesInteractor preferences;
    private final boolean defaultMetric;
    private final String placeHolder;

    public static boolean isDefaultLocaleMetric() {
        final String country = Locale.getDefault().getCountry();
        return (!"US".equals(country) &&
                !"LR".equals(country) &&
                !"MM".equals(country));
    }

    @Inject
    public UnitFormatter(@NonNull final PreferencesInteractor preferences, @NonNull final Context context) {
        this.preferences = preferences;
        this.defaultMetric = isDefaultLocaleMetric();
        this.placeHolder = context.getString(R.string.missing_data_placeholder);
    }

    public Observable<String> unitPreferenceChanges() {
        return preferences.observeChangesOn(PreferencesInteractor.USE_CELSIUS,
                                            PreferencesInteractor.USE_CENTIMETERS,
                                            PreferencesInteractor.USE_GRAMS);
    }

    //region Formatting

    @NonNull
    public CharSequence formatTemperature(final double value) {
        if (value == -1) {
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_TEMPERATURE);
        }
        double convertedValue = value;
        if (!preferences.getBoolean(PreferencesInteractor.USE_CELSIUS, defaultMetric)) {
            convertedValue = UnitOperations.celsiusToFahrenheit(convertedValue);
        }

        return Styles.assembleReadingAndUnit(convertedValue, UNIT_SUFFIX_TEMPERATURE);
    }

    @NonNull
    public CharSequence formatWeight(final long value) {
        if (preferences.getBoolean(PreferencesInteractor.USE_GRAMS, defaultMetric)) {
            final long kilograms = UnitOperations.gramsToKilograms(value);
            return kilograms + " kg";
        } else {
            final long pounds = UnitOperations.gramsToPounds(value);
            return pounds + " lbs";
        }
    }

    @NonNull
    public CharSequence formatHeight(final long value) {
        if (preferences.getBoolean(PreferencesInteractor.USE_CENTIMETERS, defaultMetric)) {
            return value + " cm";
        } else {
            final long totalInches = UnitOperations.centimetersToInches(value);
            final long feet = totalInches / 12;
            final long inches = totalInches % 12;
            if (inches > 0) {
                return String.format("%d' %d''", feet, inches);
            } else {
                return String.format("%d'", feet);
            }
        }
    }

    @NonNull
    public CharSequence formatLight(final double value) {
        if (value == -1) {
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_LIGHT);
        } else if (value < 10.0) {
            return Styles.assembleReadingAndUnit(String.format("%.1f", value),
                                                 UNIT_SUFFIX_LIGHT,
                                                 Styles.UNIT_STYLE_SUPERSCRIPT);
        } else {
            return Styles.assembleReadingAndUnit(value, UNIT_SUFFIX_LIGHT);
        }
    }

    @NonNull
    public CharSequence formatHumidity(final double value) {
        if (value == -1) {
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_HUMIDITY);
        }
        return Styles.assembleReadingAndUnit(value, UNIT_SUFFIX_HUMIDITY);
    }

    @NonNull
    public CharSequence formatAirQuality(final double value) {
        if (value == -1) {
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_AIR_QUALITY);
        }
        return Styles.assembleReadingAndUnit(value, UNIT_SUFFIX_AIR_QUALITY);
    }

    @NonNull
    public CharSequence formatNoise(final double value) {
        if (value == -1) {
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_NOISE);
        }
        return Styles.assembleReadingAndUnit(value, UNIT_SUFFIX_NOISE);
    }

    @NonNull
    public UnitConverter getUnitConverterForSensor(@NonNull final String sensor) {
        switch (sensor) {
            case ApiService.SENSOR_NAME_TEMPERATURE: {
                if (preferences.getBoolean(PreferencesInteractor.USE_CELSIUS, defaultMetric)) {
                    return UnitConverter.IDENTITY;
                } else {
                    return UnitOperations::celsiusToFahrenheit;
                }
            }
            default: {
                return UnitConverter.IDENTITY;
            }
        }
    }

    @NonNull
    public UnitPrinter getUnitPrinterForSensor(@NonNull final String sensor) {
        switch (sensor) {
            case ApiService.SENSOR_NAME_TEMPERATURE:
                return this::formatTemperature;

            case ApiService.SENSOR_NAME_HUMIDITY:
                return this::formatHumidity;

            case ApiService.SENSOR_NAME_PARTICULATES:
                return this::formatAirQuality;

            case ApiService.SENSOR_NAME_LIGHT:
                return this::formatLight;

            case ApiService.SENSOR_NAME_SOUND:
                return this::formatNoise;

            default:
                return UnitPrinter.SIMPLE;
        }
    }

    @Nullable
    public String getUnitSuffixForSensor(@NonNull final String sensor) {
        switch (sensor) {
            case ApiService.SENSOR_NAME_TEMPERATURE:
                return UNIT_SUFFIX_TEMPERATURE;

            case ApiService.SENSOR_NAME_HUMIDITY:
                return UNIT_SUFFIX_HUMIDITY;

            case ApiService.SENSOR_NAME_PARTICULATES:
                return UNIT_SUFFIX_AIR_QUALITY;

            case ApiService.SENSOR_NAME_LIGHT:
                return UNIT_SUFFIX_LIGHT;

            case ApiService.SENSOR_NAME_SOUND:
                return UNIT_SUFFIX_NOISE;

            default:
                return null;
        }
    }

    //endregion
}
