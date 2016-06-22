package is.hello.sense.units;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.widget.util.Styles;
import rx.Observable;

public class UnitFormatter {
    public static final String UNIT_SUFFIX_TEMPERATURE = "°";
    public static final String UNIT_SUFFIX_LIGHT = "lux";
    public static final String UNIT_SUFFIX_HUMIDITY = "%";
    public static final String UNIT_SUFFIX_AIR_QUALITY = "µg/m³";
    public static final String UNIT_SUFFIX_NOISE = "dB";

    // Used by PreferencesPresenter
    @Deprecated
    public static final String LEGACY_UNIT_SYSTEM_METRIC = "Metric";
    @Deprecated
    public static final String LEGACY_UNIT_SYSTEM_US_CUSTOMARY = "UsCustomary";


    private final PreferencesPresenter preferences;
    private final boolean defaultMetric;
    private final String placeHolder;

    public static boolean isDefaultLocaleMetric() {
        String country = Locale.getDefault().getCountry();
        return (!"US".equals(country) &&
                !"LR".equals(country) &&
                !"MM".equals(country));
    }

    @Inject public UnitFormatter(@NonNull PreferencesPresenter preferences, @NonNull Context context) {
        this.preferences = preferences;
        this.defaultMetric = isDefaultLocaleMetric();
        this.placeHolder = context.getString(R.string.missing_data_placeholder);
    }

    public Observable<String> unitPreferenceChanges() {
        return preferences.observeChangesOn(PreferencesPresenter.USE_CELSIUS,
                                            PreferencesPresenter.USE_CENTIMETERS,
                                            PreferencesPresenter.USE_GRAMS);
    }

    //region Formatting

    public @NonNull CharSequence formatTemperature(double value) {
        if (value == -1){
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_TEMPERATURE);
        }
        double convertedValue = value;
        if (!preferences.getBoolean(PreferencesPresenter.USE_CELSIUS, defaultMetric)) {
            convertedValue = UnitOperations.celsiusToFahrenheit(convertedValue);
        }

        return Styles.assembleReadingAndUnit(convertedValue, UNIT_SUFFIX_TEMPERATURE);
    }

    public @NonNull CharSequence formatWeight(long value) {
        if (preferences.getBoolean(PreferencesPresenter.USE_GRAMS, defaultMetric)) {
            long kilograms = UnitOperations.gramsToKilograms(value);
            return kilograms + " kg";
        } else {
            long pounds = UnitOperations.gramsToPounds(value);
            return pounds + " lbs";
        }
    }

    public @NonNull CharSequence formatHeight(long value) {
        if (preferences.getBoolean(PreferencesPresenter.USE_CENTIMETERS, defaultMetric)) {
            return value + " cm";
        } else {
            long totalInches = UnitOperations.centimetersToInches(value);
            long feet = totalInches / 12;
            long inches = totalInches % 12;
            if (inches > 0) {
                return String.format("%d' %d''", feet, inches);
            } else {
                return String.format("%d'", feet);
            }
        }
    }

    public @NonNull CharSequence formatLight(double value) {
        if (value == -1){
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_LIGHT);
        }else if (value < 10.0) {
            return Styles.assembleReadingAndUnit(String.format("%.1f", value),
                                                 UNIT_SUFFIX_LIGHT,
                                                 Styles.UNIT_STYLE_SUPERSCRIPT);
        } else {
            return Styles.assembleReadingAndUnit(value, UNIT_SUFFIX_LIGHT);
        }
    }

    public @NonNull CharSequence formatHumidity(double value) {
        if (value == -1){
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_HUMIDITY);
        }
        return Styles.assembleReadingAndUnit(value, UNIT_SUFFIX_HUMIDITY);
    }

    public @NonNull CharSequence formatAirQuality(double value) {
        if (value == -1){
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_AIR_QUALITY);
        }
        return Styles.assembleReadingAndUnit(value, UNIT_SUFFIX_AIR_QUALITY);
    }

    public @NonNull CharSequence formatNoise(double value) {
        if (value == -1){
            return Styles.assembleReadingAndUnit(placeHolder, UNIT_SUFFIX_NOISE);
        }
        return Styles.assembleReadingAndUnit(value, UNIT_SUFFIX_NOISE);
    }

    public @NonNull
    IUnitConverter getUnitConverterForSensor(@NonNull String sensor) {
        switch (sensor) {
            case ApiService.SENSOR_NAME_TEMPERATURE: {
                if (preferences.getBoolean(PreferencesPresenter.USE_CELSIUS, defaultMetric)) {
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

    public @NonNull
    IUnitPrinter getUnitPrinterForSensor(@NonNull String sensor) {
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

    public @Nullable String getUnitSuffixForSensor(@NonNull String sensor) {
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
