package is.hello.sense.flows.home.interactors;


import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

@Singleton
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
        return apiService.timelineForDate(DateFormatter.lastNight().toString(ApiService.DATE_FORMAT))
                .map(this::getValidTimeline);
    }

    @VisibleForTesting
    @Nullable
    Timeline getValidTimeline(@Nullable final Timeline timeline) {
        if(TimelineInteractor.hasValidTimeline(timeline)
                && TimelineInteractor.hasValidCondition(timeline)) {
            return timeline;
        } else {
            return null;
        }
    }

}
