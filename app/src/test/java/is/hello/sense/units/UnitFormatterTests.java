package is.hello.sense.units;

import android.annotation.SuppressLint;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.PreferencesInteractor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@SuppressLint("CommitPrefEdits")
public class UnitFormatterTests extends InjectionTestCase {
    @Inject UnitFormatter unitFormatter;
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

        assertThat(unitFormatter.formatTemperature(4).toString(),
                   is(equalTo("4 °")));

        preferences.edit()
                   .putBoolean(PreferencesInteractor.USE_CELSIUS, false)
                   .commit();

        assertThat(unitFormatter.formatTemperature(4).toString(),
                   is(equalTo("39 °")));
    }

    @Test
    public void formatLight() throws Exception {
        assertThat(unitFormatter.formatLight(42.0).toString(),
                   is(equalTo("42 lux")));
        assertThat(unitFormatter.formatLight(9.5).toString(),
                   is(equalTo("9.5 lux")));
        assertThat(unitFormatter.formatLight(1.3).toString(),
                   is(equalTo("1.3 lux")));
        assertThat(unitFormatter.formatLight(3.7).toString(),
                   is(equalTo("3.7 lux")));
    }

    @Test
    public void formatHumidity() throws Exception {
        assertThat(unitFormatter.formatHumidity(42).toString(),
                   is(equalTo("42 %")));
    }

    @Test
    public void formatAirQuality() throws Exception {
        assertThat(unitFormatter.formatAirQuality(42).toString(),
                   is(equalTo("42 µg/m³")));
    }

    @Test
    public void formatNoise() throws Exception {
        assertThat(unitFormatter.formatNoise(42).toString(),
                   is(equalTo("42 dB")));
    }

    @Test
    public void getUnitSuffixForSensor() throws Exception {
        assertThat(unitFormatter.getUnitSuffixForSensor(ApiService.SENSOR_NAME_TEMPERATURE),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_TEMPERATURE)));
        assertThat(unitFormatter.getUnitSuffixForSensor(ApiService.SENSOR_NAME_LIGHT),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_LIGHT)));
        assertThat(unitFormatter.getUnitSuffixForSensor(ApiService.SENSOR_NAME_PARTICULATES),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_AIR_QUALITY)));
        assertThat(unitFormatter.getUnitSuffixForSensor(ApiService.SENSOR_NAME_SOUND),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_NOISE)));
        assertThat(unitFormatter.getUnitSuffixForSensor(ApiService.SENSOR_NAME_HUMIDITY),
                   is(equalTo(UnitFormatter.UNIT_SUFFIX_HUMIDITY)));
    }
}
