package is.hello.sense.units;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import is.hello.sense.api.ApiService;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.systems.MetricUnitSystem;
import is.hello.sense.units.systems.UsCustomaryUnitSystem;

/**
 * An object that converts raw SI units to human readable values.
 */
public class UnitSystem implements Serializable {
    public static final String TEMP_SUFFIX = "°";

    //region Vending Systems

    public static @NonNull String getLocaleUnitSystemName(@NonNull Locale locale) {
        String countryCode = locale.getCountry();
        if ("US".equals(countryCode) || "LR".equals(countryCode) || "MM".equals(countryCode)) {
            return UsCustomaryUnitSystem.NAME;
        } else {
            return MetricUnitSystem.NAME;
        }
    }

    public static @NonNull UnitSystem createUnitSystemWithName(@NonNull String name) {
        switch (name) {
            case MetricUnitSystem.NAME: {
                return new MetricUnitSystem();
            }

            case UsCustomaryUnitSystem.NAME: {
                return new UsCustomaryUnitSystem();
            }

            default: {
                throw new IllegalStateException("unknown unit system with name: " + name);
            }
        }
    }

    //endregion


    public String getApiTemperatureUnit() {
        return ApiService.UNIT_TEMPERATURE_CELSIUS;
    }


    //region Mass

    public String getMassUnit() {
        return "g";
    }

    public long convertMass(long mass) {
        return mass;
    }

    public CharSequence formatMass(long mass) {
        return Styles.assembleReadingAndUnit(convertMass(mass), getMassUnit());
    }

    //endregion


    //region Temperature

    public String getTemperatureUnit() {
        return TEMP_SUFFIX;
    }

    public long convertTemperature(long temperature) {
        return temperature;
    }

    public CharSequence formatTemperature(long temperature) {
        return Styles.assembleReadingAndUnit(convertTemperature(temperature), getTemperatureUnit());
    }

    //endregion


    //region Humidity

    public String getHumidityUnit() {
        return "%";
    }

    public CharSequence formatHumidity(long humidity) {
        return Styles.assembleReadingAndUnit(humidity, getHumidityUnit());
    }

    //endregion


    //region Height

    public String getHeightUnit() {
        return "cm";
    }

    public CharSequence formatHeight(long distance) {
        return Styles.assembleReadingAndUnit(distance, getHeightUnit());
    }

    //endregion


    //region Sound

    public String getSoundUnit() {
        return "db";
    }

    public CharSequence formatSound(long decibels) {
        return Styles.assembleReadingAndUnit(decibels, getSoundUnit());
    }

    //endregion


    //region Light

    public String getLightUnit() {
        return "lux";
    }

    public CharSequence formatLight(long lux) {
        return Styles.assembleReadingAndUnit(lux, getLightUnit());
    }

    //endregion


    //region Particulates

    public String getParticulatesUnit() {
        return "";
    }

    public CharSequence formatParticulates(long particulates) {
        return Long.toString(particulates);
    }

    //endregion


    //region Vendors

    public @Nullable Formatter getUnitFormatterForSensor(@NonNull String sensor) {
        switch (sensor) {
            case ApiService.SENSOR_NAME_TEMPERATURE:
                return this::formatTemperature;

            case ApiService.SENSOR_NAME_HUMIDITY:
                return this::formatHumidity;

            case ApiService.SENSOR_NAME_PARTICULATES:
                return this::formatParticulates;

            case ApiService.SENSOR_NAME_LIGHT:
                return this::formatLight;

            case ApiService.SENSOR_NAME_SOUND:
                return this::formatSound;

            default:
                return null;
        }
    }

    public List<Unit> toUnitList() {
        // This order applies to:
        // - RoomSensorHistory
        // - RoomConditions
        // - RoomConditionsFragment
        // - UnitSystem
        // - OnboardingRoomCheckFragment
        return Arrays.asList(
            new Unit(this::formatTemperature, this::convertTemperature, getTemperatureUnit()),
            new Unit(this::formatHumidity, IDENTITY_CONVERTER, getHumidityUnit()),
            new Unit(this::formatLight, IDENTITY_CONVERTER, getLightUnit()),
            new Unit(this::formatSound, IDENTITY_CONVERTER, getSoundUnit())
        );
    }

    //endregion

    public interface Formatter {
        @NonNull CharSequence format(long value);
    }

    /**
     * The identity converter. Returns a value unmodified.
     */
    public static final Converter IDENTITY_CONVERTER = v -> v;

    /**
     * A functor that converts a raw value into the user's unit system.
     */
    public interface Converter {
        long convert(long value);
    }

    public static class Unit {
        private final @Nullable Formatter formatter;
        private final @NonNull Converter converter;
        private final @NonNull String name;

        public Unit(@Nullable Formatter formatter,
                    @NonNull Converter converter,
                    @NonNull String name) {
            this.formatter = formatter;
            this.converter = converter;
            this.name = name;
        }

        public CharSequence format(long value) {
            if (formatter != null) {
                return formatter.format(value);
            } else {
                return Long.toString(value);
            }
        }

        public long convert(long value) {
            return converter.convert(value);
        }

        @Nullable
        public Formatter getFormatter() {
            return formatter;
        }

        @NonNull
        public String getName() {
            return name;
        }
    }
}
