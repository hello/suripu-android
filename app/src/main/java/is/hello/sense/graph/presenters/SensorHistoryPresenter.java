package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import rx.Observable;

public class SensorHistoryPresenter extends ValuePresenter<SensorHistoryPresenter.Result> {
    public static final int MODE_WEEK = 0;
    public static final int MODE_DAY = 1;

    @Inject ApiService apiService;
    @Inject UnitFormatter unitFormatter;

    private String sensorName;
    private int mode = MODE_DAY;

    public final PresenterSubject<Result> history = this.subject;

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        super.onRestoreState(savedState);

        this.mode = savedState.getInt("mode");
        this.sensorName = savedState.getString("sensorName");
        update();
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle state = new Bundle();
        state.putString("sensorName", sensorName);
        state.putInt("mode", mode);
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
        Observable<ArrayList<SensorHistory>> newHistory;
        if (getMode() == MODE_DAY) {
            newHistory = apiService.sensorHistoryForDay(getSensorName(), SensorHistory.timeForLatest());
        } else {
            newHistory = apiService.sensorHistoryForWeek(getSensorName(), SensorHistory.timeForLatest());
        }
        return Observable.combineLatest(newHistory, unitFormatter.unitSystem, Result::new);
    }


    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
        update();
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
        update();
    }


    public static class Result implements Serializable {
        public final ArrayList<SensorHistory> data;
        public final UnitSystem unitSystem;

        public Result(@NonNull ArrayList<SensorHistory> data, @NonNull UnitSystem unitSystem) {
            this.data = data;
            this.unitSystem = unitSystem;
        }
    }
}
