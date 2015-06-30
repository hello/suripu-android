package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.api.model.v2.TimelineFeedback;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class TimelinePresenter extends ValuePresenter<Timeline> {
    @Inject ApiService service;

    private DateTime date;

    public final PresenterSubject<Timeline> timeline = subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return (getDate() != null);
    }

    @Override
    protected Observable<Timeline> provideUpdateObservable() {
        return service.timelineForDate_v2(date.year().getAsString(),
                                          date.monthOfYear().getAsString(),
                                          date.dayOfMonth().getAsString());
    }


    public DateTime getDate() {
        return date;
    }

    public void setDateWithTimeline(@NonNull DateTime date, @Nullable Timeline timeline) {
        this.date = date;
        if (timeline != null) {
            this.timeline.onNext(timeline);
        } else {
            update();
        }
    }

    public Observable<VoidResponse> amendEventTime(@NonNull TimelineEvent event, @NonNull LocalTime newTime) {
        return latest().flatMap(timeline -> {
            DateTime timelineDate = timeline.getDate();
            String year = timelineDate.year().getAsString();
            String month = timelineDate.monthOfYear().getAsString();
            String day = timelineDate.dayOfMonth().getAsString();
            TimelineFeedback amendment = TimelineFeedback.amendTime(event, newTime);
            return service.amendTimelineEventTime_v2(year, month, day, amendment);
        });
    }

    public Observable<VoidResponse> verifyEvent(@NonNull TimelineEvent event) {
        return latest().flatMap(timeline -> {
            DateTime timelineDate = timeline.getDate();
            String year = timelineDate.year().getAsString();
            String month = timelineDate.monthOfYear().getAsString();
            String day = timelineDate.dayOfMonth().getAsString();
            TimelineFeedback feedback = TimelineFeedback.from(event);
            return service.verifyTimelineEvent_v2(year, month, day, feedback);
        });
    }

    public Observable<VoidResponse> deleteEvent(@NonNull TimelineEvent event) {
        return latest().flatMap(timeline -> {
            DateTime timelineDate = timeline.getDate();
            String year = timelineDate.year().getAsString();
            String month = timelineDate.monthOfYear().getAsString();
            String day = timelineDate.dayOfMonth().getAsString();
            TimelineFeedback feedback = TimelineFeedback.from(event);
            return service.verifyTimelineEvent_v2(year, month, day, feedback);
        });
    }
}
