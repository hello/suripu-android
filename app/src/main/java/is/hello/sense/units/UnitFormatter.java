package is.hello.sense.units;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.util.Constants;

@Singleton public final class UnitFormatter implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final SharedPreferences sharedPreferences;

    private UnitSystem unitSystem;

    @Inject public UnitFormatter(@NonNull SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;

        reloadUnitSystem();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    //region System Changes

    private void reloadUnitSystem() {
        String systemName = sharedPreferences.getString(Constants.GLOBAL_PREF_UNIT_SYSTEM, UnitSystem.DEFAULT_UNIT_SYSTEM);
        this.unitSystem = UnitSystem.createUnitSystemWithName(systemName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        reloadUnitSystem();
    }

    //endregion


    //region Formatting

    public String assemble(long value, String unit) {
        return value + unit;
    }

    public String formatMass(long mass) {
        return assemble(unitSystem.convertGrams(mass), unitSystem.getMassSuffix());
    }

    public String formatTemperature(long temperature) {
        return assemble(unitSystem.convertDegreesCelsius(temperature), unitSystem.getTemperatureSuffix());
    }

    public String formatDistance(long distance) {
        return assemble(unitSystem.convertCentimeters(distance), unitSystem.getDistanceSuffix());
    }

    public String formatPercentage(long percentage) {
        return percentage + "%";
    }

    public String formatRaw(long value) {
        return Long.toString(value);
    }

    //endregion


    public interface Formatter {
        String format(long value);
    }
}
