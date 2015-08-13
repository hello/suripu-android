package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class SensorHistoryPresenter extends ValuePresenter<ArrayList<SensorGraphSample>> {
    @Inject ApiService apiService;

    private String sensorName;
    private Mode mode = Mode.DAY;

    public final PresenterSubject<ArrayList<SensorGraphSample>> history = this.subject;

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
    protected Observable<ArrayList<SensorGraphSample>> provideUpdateObservable() {
        Observable<ArrayList<SensorGraphSample>> newHistory;
        if (getMode() == Mode.DAY) {
            newHistory = apiService.sensorHistoryForDay(getSensorName(), SensorGraphSample.timeForLatest());
        } else {
            newHistory = apiService.sensorHistoryForWeek(getSensorName(), SensorGraphSample.timeForLatest());
        }
        return newHistory;
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


    public enum Mode {
        WEEK,
        DAY,
    }
}
