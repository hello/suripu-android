package is.hello.sense.graph.presenters;

import android.text.TextUtils;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorHistory;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class SensorHistoryPresenter extends Presenter {
    public static final int MODE_WEEK = 0;
    public static final int MODE_DAY = 1;

    @Inject ApiService apiService;

    private String sensorName;
    private int mode;

    public final ReplaySubject<List<SensorHistory>> history = ReplaySubject.createWithSize(1);

    public void update() {
        if (!TextUtils.isEmpty(getSensorName())) {
            Observable<List<SensorHistory>> newHistory;
            if (getMode() == MODE_DAY) {
                newHistory = apiService.sensorHistoryForDay(getSensorName(), System.currentTimeMillis());
            } else {
                newHistory = apiService.sensorHistoryForWeek(getSensorName(), System.currentTimeMillis());
            }
            newHistory.subscribe(history::onNext, history::onError);
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
