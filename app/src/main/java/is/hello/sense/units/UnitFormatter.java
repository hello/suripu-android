package is.hello.sense.units;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.util.Locale;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.interactors.Interactor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Constants;
import rx.Observable;

public class UnitFormatter extends Interactor {
    public static final String UNIT_SUFFIX_TEMPERATURE = "°";
    public static final String UNIT_SUFFIX_LIGHT = "lx";
    public static final String UNIT_SUFFIX_HUMIDITY = "%";
    public static final String UNIT_SUFFIX_AIR_QUALITY = "µg/m³";
    public static final String UNIT_SUFFIX_NOISE = "dB";
    public static final String UNIT_SUFFIX_GAS = "ppm";
    public static final String UNIT_SUFFIX_LIGHT_TEMPERATURE = "k"; //todo check validity
    public static final String UNIT_SUFFIX_KELVIN = "k"; //todo check validity
    public static final String UNIT_SUFFIX_PRESSURE = "kPa";


    // Used by PreferencesInteractor
    @Deprecated
    public static final String LEGACY_UNIT_SYSTEM_METRIC = "Metric";
    @Deprecated
    public static final String LEGACY_UNIT_SYSTEM_US_CUSTOMARY = "UsCustomary";


    private final PreferencesInteractor preferences;
    private final boolean defaultMetric;
    private final String placeHolder;
    private final UnitBuilder builder = new UnitBuilder();

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
    public UnitConverter getUnitConverterForSensor(@NonNull final SensorType type) {
        switch (type) {
            case TEMPERATURE: {
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
    private SuffixPrinter getSuffixPrinterForSensor(@NonNull final SensorType sensorType) {
        switch (sensorType) {
            case TEMPERATURE:
            case HUMIDITY:
                return () -> Styles.UNIT_STYLE_SUPERSCRIPT;
            default:
                return () -> Styles.UNIT_STYLE_SUBSCRIPT;
        }
    }

    @NonNull
    public String getSuffixForSensor(@NonNull final SensorType type) {
        switch (type) {
            case TEMPERATURE:
                return UNIT_SUFFIX_TEMPERATURE;
            case HUMIDITY:
                return UNIT_SUFFIX_HUMIDITY;
            case PARTICULATES:
                return UNIT_SUFFIX_AIR_QUALITY;
            case LIGHT:
                return UNIT_SUFFIX_LIGHT;
            case SOUND:
                return UNIT_SUFFIX_NOISE;
            case CO2:
                return UNIT_SUFFIX_GAS;
            case TVOC:
                return UNIT_SUFFIX_AIR_QUALITY;
            case LIGHT_TEMPERATURE:
                return UNIT_SUFFIX_LIGHT_TEMPERATURE;
            case UV:
                return UNIT_SUFFIX_KELVIN;
            case PRESSURE:
                return UNIT_SUFFIX_PRESSURE;
            case UNKNOWN:
            default:
                return Constants.EMPTY_STRING;
        }
    }

    //region Sensor Detail specific

    public String getMeasuredInString(@NonNull final SensorType type) {
        String measuredIn = Constants.EMPTY_STRING;
        switch (type) {
            case TEMPERATURE:
                if (preferences.getBoolean(PreferencesInteractor.USE_CELSIUS, false)) {
                    measuredIn = ApiService.UNIT_TEMPERATURE_CELSIUS.toUpperCase();
                } else {
                    measuredIn = ApiService.UNIT_TEMPERATURE_US_CUSTOMARY.toUpperCase();
                }
            default:
                measuredIn += getSuffixForSensor(type);
        }
        return measuredIn;
    }

    @StringRes
    public int getAboutStringRes(@NonNull final SensorType type) {
        switch (type) {
            case TEMPERATURE:
                if (preferences.getBoolean(PreferencesInteractor.USE_CELSIUS, false)) {
                    return R.string.sensor_about_temperature_celsius;
                } else {
                    return R.string.sensor_about_temperature_fahrenheit;
                }
            case HUMIDITY:
                return R.string.sensor_about_humidity;
            case LIGHT:
                return R.string.sensor_about_light;
            case CO2:
                return R.string.sensor_about_co2;
            case LIGHT_TEMPERATURE:
                return R.string.sensor_about_light_temp;
            case PARTICULATES:
                return R.string.sensor_about_particulates;
            case SOUND:
                return R.string.sensor_about_noise;
            case UV:
                return R.string.sensor_about_uv_light;
            case TVOC:
                return R.string.sensor_about_voc;
            case PRESSURE:
                return R.string.sensor_about_pressure;
            default:
                logEvent("No string found for type: " + type);
                return R.string.empty;
        }
    }


    public UnitBuilder createUnitBuilder(@NonNull final SensorType sensorType, final float value) {
        return builder.updateFor(sensorType, value);
    }

    public UnitBuilder createUnitBuilder(@NonNull final Sensor sensor) {
        return builder.updateFor(sensor);
    }

    public class UnitBuilder {
        private float value = 0;
        private UnitConverter unitConverter;
        private SuffixPrinter suffixPrinter;
        private int valueDecimalPlaces = 0;
        private String suffix;
        private boolean showValue = true;
        private boolean showSuffix = true;

        private UnitBuilder() {
        }

        private UnitBuilder updateFor(@NonNull final SensorType sensorType, final float value) {
            this.unitConverter = getUnitConverterForSensor(sensorType);
            this.suffixPrinter = getSuffixPrinterForSensor(sensorType);
            this.setValueDecimalPlaces(sensorType);
            this.value = value;
            this.suffix = getSuffixForSensor(sensorType);
            this.showValue = true;
            this.showSuffix = true;
            return this;
        }

        private UnitBuilder updateFor(@NonNull final Sensor sensor) {
            this.unitConverter = getUnitConverterForSensor(sensor.getType());
            this.suffixPrinter = getSuffixPrinterForSensor(sensor.getType());
            this.setValueDecimalPlaces(sensor.getType());
            this.value = sensor.getValue();
            this.suffix = getSuffixForSensor(sensor.getType());
            this.showValue = true;
            this.showSuffix = true;
            return this;
        }

        private void setValueDecimalPlaces(@NonNull final SensorType sensorType) {
            switch (sensorType) {
                case LIGHT:
                    setValueDecimalPlaces(1);
                    break;
                default:
                    setValueDecimalPlaces(0);
            }
        }

        public UnitBuilder setValueDecimalPlaces(final int valueDecimalPlaces) {
            if (valueDecimalPlaces >= 0) {
                this.valueDecimalPlaces = valueDecimalPlaces;
            }
            return this;
        }

        public UnitBuilder hideValue() {
            this.showValue = false;
            return this;
        }

        public UnitBuilder hideSuffix() {
            this.showSuffix = false;
            return this;
        }

        /**
         * @return no styles
         */
        public CharSequence build() {
            return formattedValue() + formattedSuffix();
        }

        /**
         * @return default styles
         */
        public CharSequence buildWithStyle() {
            return Styles.assembleReadingAndUnit(formattedValue(), formattedSuffix(), suffixPrinter.getUnitStyle());
        }

        /**
         * @return suffix as subscript
         */
        public CharSequence buildWithStyleSubscript() {
            return Styles.assembleReadingAndUnit(formattedValue(), formattedSuffix(), Styles.UNIT_STYLE_SUBSCRIPT);
        }

        /**
         * @return suffix as superscript
         */
        public CharSequence buildWithStyleSuperscript() {
            return Styles.assembleReadingAndUnit(formattedValue(), formattedSuffix(), Styles.UNIT_STYLE_SUPERSCRIPT);
        }

        private String formattedSuffix() {
            if (!showSuffix) {
                return Constants.EMPTY_STRING;
            }
            return suffix;

        }

        private CharSequence formattedValue() {
            if (!showValue) {
                return Constants.EMPTY_STRING;
            }
            if (value == Sensor.NO_VALUE) {
                return placeHolder;
            }
            return String.format("%." + valueDecimalPlaces + "f", unitConverter.convert(value));
        }
    }

    interface SuffixPrinter {
        @Styles.UnitStyle
        int getUnitStyle();
    }
//endregion
}
