package is.hello.sense.graph.presenters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.ui.widget.SensorStateView;
import is.hello.sense.units.UnitFormatter;
import rx.subjects.ReplaySubject;

@Singleton public class CurrentConditionsPresenter extends Presenter {
    @Inject ApiService apiService;

    public final ReplaySubject<RoomConditions> currentConditions = ReplaySubject.create(1);

    @Override
    public void update() {
        apiService.currentRoomConditions().subscribe(currentConditions::onNext, currentConditions::onError);
    }
}
