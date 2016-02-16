package is.hello.sense.graph.presenters;


import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class TrendsV2Presenter extends ScopedValuePresenter<Trends> {
    @Inject
    ApiService apiService;
    public final PresenterSubject<Trends> trends = subject;

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
        return apiService.trends(timeScale);
    }

    public void setTimeScale(Trends.TimeScale timeScale) {
        this.timeScale = timeScale;
        update();
    }

}
