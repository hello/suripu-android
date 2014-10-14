package is.hello.sense.units;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.util.Constants;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

@Singleton
public final class UnitFormatter {
    private UnitSystem unitSystem;

    @Inject
    public UnitFormatter(@NonNull PreferencesPresenter preferencesPresenter) {
        Observable<String> unitSystemName = preferencesPresenter.observableString(Constants.GLOBAL_PREF_UNIT_SYSTEM, UnitSystem.DEFAULT_UNIT_SYSTEM);
        unitSystemName.subscribeOn(AndroidSchedulers.mainThread())
                      .subscribe(name -> this.unitSystem = UnitSystem.createUnitSystemWithName(name));
    }


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
