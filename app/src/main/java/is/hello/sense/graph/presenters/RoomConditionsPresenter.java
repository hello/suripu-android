package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import rx.Observable;

@Singleton public class RoomConditionsPresenter extends ValuePresenter<RoomConditionsPresenter.Result> {
    @Inject ApiService apiService;
    @Inject UnitFormatter unitFormatter;

    public final PresenterSubject<Result> currentConditions = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Result> provideUpdateObservable() {
        return Observable.combineLatest(apiService.currentRoomConditions(),
                                        unitFormatter.unitSystem,
                                        Result::new);
    }

    public static final class Result implements Serializable {
        public final RoomConditions conditions;
        public final UnitSystem units;

        public Result(@NonNull RoomConditions conditions, @NonNull UnitSystem units) {
            this.conditions = conditions;
            this.units = units;
        }
    }
}
