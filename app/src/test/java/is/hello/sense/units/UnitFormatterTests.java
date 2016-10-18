package is.hello.sense.units;

import android.annotation.SuppressLint;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Scale;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.PreferencesInteractor;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

@SuppressLint("CommitPrefEdits")
public class UnitFormatterTests extends InjectionTestCase {
    @Inject
    UnitFormatter unitFormatter;
    @Inject
    PreferencesInteractor preferences;

    @Test
    public void unitPreferenceChanges() throws Exception {
        Set<String> changes = new HashSet<>();

        unitFormatter.unitPreferenceChanges()
                     .subscribe(changes::add);

        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_CELSIUS, true)
                   .commit();

        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_CENTIMETERS, true)
                   .commit();

        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_GRAMS, true)
                   .commit();

        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_24_TIME, true)
                   .commit();

        assertThat(changes.size(), is(equalTo(3)));
        assertThat(changes, hasItem(PreferencesInteractor.USE_CELSIUS));
        assertThat(changes, hasItem(PreferencesInteractor.USE_CENTIMETERS));
        assertThat(changes, hasItem(PreferencesInteractor.USE_GRAMS));
        assertThat(changes, not(hasItem(PreferencesInteractor.USE_24_TIME)));
    }

    @Test
    public void formatWeight() throws Exception {
        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_GRAMS, true)
                   .commit();

        assertThat(unitFormatter.formatWeight(5800).toString(),
                   is(equalTo("5 kg")));

        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_GRAMS, false)
                   .commit();

        assertThat(unitFormatter.formatWeight(5800).toString(),
                   is(equalTo("13 lbs")));
    }

    @Test
    public void formatHeight() throws Exception {
        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_CENTIMETERS, true)
                   .commit();

        assertThat(unitFormatter.formatHeight(190).toString(),
                   is(equalTo("190 cm")));

        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_CENTIMETERS, false)
                   .commit();

        assertThat(unitFormatter.formatHeight(190).toString(),
                   is(equalTo("6' 3''")));
    }

    @Test
    public void formatTemperature() throws Exception {
        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_CELSIUS, true)
                   .commit();

        assertThat(unitFormatter.createUnitBuilder(SensorType.TEMPERATURE, 4)
                                .buildWithStyle(),
                   is(equalTo("4 °")));

        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_CELSIUS, false)
                   .commit();

        assertThat(unitFormatter.createUnitBuilder(SensorType.TEMPERATURE, 4)
                                .buildWithStyle(),
                   is(equalTo("39 °")));
    }


    @Test
    public void formatLight() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.LIGHT, 42.0f)
                                .setValueDecimalPlaces(0)
                                .buildWithStyle(),
                   is(equalTo("42 lx")));
        assertThat(unitFormatter.createUnitBuilder(SensorType.LIGHT, 9.5f)
                                .buildWithStyle(),
                   is(equalTo("9.5 lx")));
        assertThat(unitFormatter.createUnitBuilder(SensorType.LIGHT, 1.3f)
                                .buildWithStyle(),
                   is(equalTo("1.3 lx")));
        assertThat(unitFormatter.createUnitBuilder(SensorType.LIGHT, 3.7f)
                                .buildWithStyle(),
                   is(equalTo("3.7 lx")));
    }

    @Test
    public void formatHumidity() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.HUMIDITY, 42)
                                .buildWithStyle(),
                   is(equalTo("42 %")));
    }

    @Test
    public void formatNoise() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.SOUND, 42)
                                .buildWithStyle(),
                   is(equalTo("42 dB")));
    }

    @Test
    public void formatLightTemperature() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.LIGHT_TEMPERATURE, 42)
                                .buildWithStyle(),
                   is(equalTo("42 k")));
    }

    @Test
    public void formatUV() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.UV, 42)
                                .buildWithStyle(),
                   is(equalTo("42 k")));
    }

    @Test
    public void formatVOC() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.TVOC, 42)
                                .buildWithStyle(),
                   is(equalTo("42 µg/m³")));
    }

    @Test
    public void formatCO2() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.CO2, 42)
                                .buildWithStyle(),
                   is(equalTo("42 ppm")));
    }

    @Test
    public void formatPressure() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.PRESSURE, 42)
                                .buildWithStyle(),
                   is(equalTo("42 mBar")));
    }

    @Test
    public void formatUnknown() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.UNKNOWN, 42)
                                .buildWithStyle(),
                   is(equalTo("42 ")));
    }

    @Test
    public void hideValue() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.HUMIDITY, 42)
                                .hideValue()
                                .buildWithStyle(),
                   is(equalTo(" %")));
    }

    @Test
    public void hideSuffix() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.HUMIDITY, 42)
                                .hideSuffix()
                                .buildWithStyle(),
                   is(equalTo("42 ")));
    }

    @Test
    public void ignoreNegativeValueDecimalPlaces() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.HUMIDITY, 42.12f)
                                .setValueDecimalPlaces(-2)
                                .buildWithStyle(),
                   is(equalTo("42 %")));
    }

    @Test
    public void respectPositiveValueDecimalPlaces() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.HUMIDITY, 42.12f)
                                .setValueDecimalPlaces(2)
                                .buildWithStyle(),
                   is(equalTo("42.12 %")));
    }

    @Test
    public void defaultLightValueDecimalPlaceIsOne() throws Exception {
        assertThat(unitFormatter.createUnitBuilder(SensorType.LIGHT, 42.12f)
                                .buildWithStyle(),
                   is(equalTo("42.1 lx")));
    }

    @Test
    public void getUnitSuffixForSensor() throws Exception {
        assertThat(unitFormatter.getSuffixForSensor(SensorType.TEMPERATURE),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_TEMPERATURE)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.LIGHT),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_LIGHT)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.PARTICULATES),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_AIR_QUALITY)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.SOUND),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_NOISE)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.HUMIDITY),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_HUMIDITY)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.CO2),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_GAS)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.TVOC),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_AIR_QUALITY)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.UV),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_KELVIN)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.LIGHT_TEMPERATURE),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_LIGHT_TEMPERATURE)));
        assertThat(unitFormatter.getSuffixForSensor(SensorType.PRESSURE),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_PRESSURE)));
    }

    @Test
    public void getConvertedScalesReturnsSameScaleIfNoConversionNeeded() {
        final List<Scale> testScales = new ArrayList<>();
        testScales.add(new Scale("test", null, null, Condition.UNKNOWN));
        testScales.add(new Scale("non empty test", 0f, 20f, Condition.UNKNOWN));
        assertEquals(unitFormatter.getConvertedScales(testScales, SensorType.UNKNOWN), testScales);
    }

    @Test
    public void getConvertedTemperatureScaleReturnsExpected() {
        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_CELSIUS, false)
                   .commit();

        final List<Scale> testScales = new ArrayList<>();
        testScales.add(new Scale("test", null, null, Condition.UNKNOWN));
        testScales.add(new Scale("non empty test", 0f, 20f, Condition.UNKNOWN));
        final List<Scale> convertedScales = unitFormatter.getConvertedScales(testScales, SensorType.TEMPERATURE);
        final Scale convertedNonEmptyScale = convertedScales.get(1);
        final Scale originalNonEmptyScale = testScales.get(1);

        assertNotEquals(convertedScales, testScales);
        assertEquals(convertedScales.get(0), testScales.get(0));
        assertEquals(convertedNonEmptyScale.getMax(),
                     UnitOperations.celsiusToFahrenheit(originalNonEmptyScale.getMax()));
    }
}
