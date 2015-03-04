package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.units.UnitSystem;
import rx.Observable;

public class SensorHistoryPresenter extends ValuePresenter<SensorHistoryPresenter.Result> {
    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferences;

    private String sensorName;
    private Mode mode = Mode.DAY;

    public final PresenterSubject<Result> history = this.subject;

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        super.onRestoreState(savedState);

        this.mode = Mode.values()[savedState.getInt("mode")];
        this.sensorName = savedState.getString("sensorName");
        update();
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle state = new Bundle();
        state.putString("sensorName", sensorName);
        state.putInt("mode", mode.ordinal());
        return state;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return !TextUtils.isEmpty(getSensorName());
    }

    @Override
    protected Observable<Result> provideUpdateObservable() {
        Observable<ArrayList<SensorGraphSample>> newHistory;
        if (getMode() == Mode.DAY) {
            newHistory = apiService.sensorHistoryForDay(getSensorName(), SensorGraphSample.timeForLatest());
        } else {
            newHistory = apiService.sensorHistoryForWeek(getSensorName(), SensorGraphSample.timeForLatest());
        }
        return Observable.combineLatest(newHistory, preferences.observableUnitSystem(), Result::new);
    }


    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
        update();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(@NonNull Mode mode) {
        this.mode = mode;
        update();
    }


    public static class Result implements Serializable {
        public final ArrayList<SensorGraphSample> data;
        public final UnitSystem unitSystem;

        public Result(@NonNull ArrayList<SensorGraphSample> data, @NonNull UnitSystem unitSystem) {
            this.data = data;
            this.unitSystem = unitSystem;
        }
    }

    public static enum Mode {
        WEEK,
        DAY,
    }
}
