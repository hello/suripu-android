package is.hello.sense.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;

@Singleton public final class UnitFormatter implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Context context;
    private final SharedPreferences sharedPreferences;

    private Units.System unitSystem;

    @Inject public UnitFormatter(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = sharedPreferences;

        reloadUnitSystem();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    //region System Changes

    private void reloadUnitSystem() {
        String systemName = sharedPreferences.getString(Constants.GLOBAL_PREF_UNIT_SYSTEM, "US_CUSTOMARY");
        this.unitSystem = Units.System.fromString(systemName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        reloadUnitSystem();
    }

    //endregion


    //region Formatting

    public String formatMass(long mass) {
        return context.getString(R.string.unit_fmt, unitSystem.convertMass(mass), context.getString(unitSystem.massSuffixRes));
    }

    public String formatTemperature(long temperature) {
        return context.getString(R.string.unit_fmt, unitSystem.convertTemperature(temperature), context.getString(unitSystem.temperatureSuffixRes));
    }

    public String formatDistance(long distance) {
        return context.getString(R.string.unit_fmt, unitSystem.convertDistance(distance), context.getString(unitSystem.distanceSuffixRes));
    }

    public String formatPercentage(long percentage) {
        return context.getString(R.string.unit_fmt, percentage, "%");
    }

    public String raw(long value) {
        return Long.toString(value);
    }

    //endregion


    public interface Formatter {
        String format(long value);
    }
}
