package is.hello.sense.units;

import android.support.annotation.NonNull;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.graph.presenters.PreferencesPresenter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;

@Singleton
public final class UnitFormatter {
    public final ReplaySubject<UnitSystem> unitSystem = ReplaySubject.createWithSize(1);

    @Inject public UnitFormatter(@NonNull PreferencesPresenter preferencesPresenter) {
        Observable<String> unitSystemName = preferencesPresenter.observableString(PreferencesPresenter.UNIT_SYSTEM,
                                                                                  UnitSystem.getDefaultUnitSystem(Locale.getDefault()));
        unitSystemName.map(UnitSystem::createUnitSystemWithName)
                      .subscribeOn(AndroidSchedulers.mainThread())
                      .subscribe(unitSystem::onNext);
    }


    public interface Formatter {
        String format(Float value);
    }
}
