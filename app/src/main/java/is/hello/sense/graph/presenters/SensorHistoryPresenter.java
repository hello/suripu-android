package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import rx.Observable;

public class SensorHistoryPresenter extends Presenter {
    public static final int MODE_WEEK = 0;
    public static final int MODE_DAY = 1;

    @Inject ApiService apiService;
    @Inject UnitFormatter unitFormatter;

    private String sensorName;
    private int mode;

    public final PresenterSubject<Pair<List<SensorHistory>, UnitSystem>> history = PresenterSubject.create();

    @Override
    public void onRestoreState(@NonNull Parcelable savedState) {
        super.onRestoreState(savedState);

        if (savedState instanceof Bundle) {
            Bundle state = ((Bundle) savedState);
            this.mode = state.getInt("mode");
            this.sensorName = state.getString("sensorName");
            update();
        }
    }

    @Nullable
    @Override
    public Parcelable onSaveState() {
        Bundle state = new Bundle();
        state.putString("sensorName", sensorName);
        state.putInt("mode", mode);
        return state;
    }

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        history.forget();
        return true;
    }

    public void update() {
        if (!TextUtils.isEmpty(getSensorName())) {
            Observable<List<SensorHistory>> newHistory;
            if (getMode() == MODE_DAY) {
                newHistory = apiService.sensorHistoryForDay(getSensorName(), System.currentTimeMillis());
            } else {
                newHistory = apiService.sensorHistoryForWeek(getSensorName(), System.currentTimeMillis());
            }
            Observable<Pair<List<SensorHistory>, UnitSystem>> result = Observable.combineLatest(newHistory, unitFormatter.unitSystem, Pair::new);
            result.subscribe(history);
        }
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
}
