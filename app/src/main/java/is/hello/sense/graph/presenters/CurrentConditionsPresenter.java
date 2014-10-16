package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import rx.Observable;
import rx.subjects.ReplaySubject;

@Singleton public class CurrentConditionsPresenter extends Presenter {
    @Inject ApiService apiService;
    @Inject UnitFormatter unitFormatter;

    public final ReplaySubject<Result> currentConditions = ReplaySubject.createWithSize(1);

    @Override
    public void update() {
        Observable<Result> result = Observable.combineLatest(apiService.currentRoomConditions(),
                                                             unitFormatter.unitSystem,
                                                             Result::new);
        result.subscribe(currentConditions::onNext, currentConditions::onError);
    }

    public static final class Result {
        public final RoomConditions conditions;
        public final UnitSystem units;

        public Result(@NonNull RoomConditions conditions, @NonNull UnitSystem units) {
            this.conditions = conditions;
            this.units = units;
        }
    }
}
