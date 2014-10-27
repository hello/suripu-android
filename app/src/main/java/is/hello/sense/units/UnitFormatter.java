package is.hello.sense.units;

import android.support.annotation.NonNull;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

@Singleton
public final class UnitFormatter {
    public final PresenterSubject<UnitSystem> unitSystem = PresenterSubject.create();

    @Inject public UnitFormatter(@NonNull PreferencesPresenter preferencesPresenter) {
        Observable<String> unitSystemName = preferencesPresenter.observableString(PreferencesPresenter.UNIT_SYSTEM,
                                                                                  UnitSystem.getDefaultUnitSystem(Locale.getDefault()));
        unitSystemName.map(UnitSystem::createUnitSystemWithName)
                      .subscribeOn(AndroidSchedulers.mainThread())
                      .subscribe(unitSystem);
    }


    public interface Formatter {
        String format(Float value);
    }
}
