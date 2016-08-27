package is.hello.sense.interactors;


import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class TrendsInteractor extends ScopedValueInteractor<Trends> {
    @Inject ApiService apiService;

    public final InteractorSubject<Trends> trends = this.subject;

    private Trends.TimeScale timeScale = Trends.TimeScale.LAST_WEEK;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Trends> provideUpdateObservable() {
        return apiService.trendsForTimeScale(timeScale);
    }

    public void setTimeScale(@NonNull Trends.TimeScale timeScale) {
        this.timeScale = timeScale;
        update();
    }

    public Trends.TimeScale getTimeScale() {
        return timeScale;
    }
}
