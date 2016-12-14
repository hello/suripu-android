package is.hello.sense.flows.home.interactors;


import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

public class LastNightInteractor extends ValueInteractor<Timeline> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<Timeline> timeline = subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Timeline> provideUpdateObservable() {
        // todo this is a temporary fix
        return apiService.timelineForDate(DateFormatter.todayForTimeline().minusDays(1).toString(ApiService.DATE_FORMAT));
    }

}
