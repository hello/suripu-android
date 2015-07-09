package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
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
        return service.timelineForDate(date.toString(ApiService.DATE_FORMAT));
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

    public Observable<Void> amendEventTime(@NonNull TimelineEvent event, @NonNull LocalTime newTime) {
        return latest().flatMap(timeline -> {
            String date = timeline.getDate().toString(ApiService.DATE_FORMAT);
            TimelineEvent.TimeAmendment timeAmendment = new TimelineEvent.TimeAmendment(newTime,
                    event.getTimezone().getOffset(event.getShiftedTimestamp()));
            return service.amendTimelineEventTime(date, event.getType(),
                    event.getRawTimestamp().getMillis(), timeAmendment)
                    .doOnNext(this.timeline::onNext)
                    .map(ignored -> null);
        });
    }

    public Observable<Void> verifyEvent(@NonNull TimelineEvent event) {
        return latest().flatMap(timeline -> {
            String date = timeline.getDate().toString(ApiService.DATE_FORMAT);
            return service.verifyTimelineEvent(date, event.getType(), event.getRawTimestamp().getMillis())
                    .doOnNext(this.timeline::onNext)
                    .map(ignored -> null);
        });
    }

    public Observable<Void> deleteEvent(@NonNull TimelineEvent event) {
        return latest().flatMap(timeline -> {
            String date = timeline.getDate().toString(ApiService.DATE_FORMAT);
            return service.deleteTimelineEvent(date, event.getType(), event.getRawTimestamp().getMillis())
                    .doOnNext(this.timeline::onNext)
                    .map(ignored -> null);
        });
    }
}
