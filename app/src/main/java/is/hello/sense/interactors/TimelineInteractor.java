package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class TimelineInteractor extends ValueInteractor<Timeline> {
    @Inject ApiService apiService;

    private LocalDate date;

    public final InteractorSubject<Timeline> timeline = subject;

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
        return apiService.timelineForDate(date.toString(ApiService.DATE_FORMAT));
    }


    public LocalDate getDate() {
        return date;
    }

    public void setDateWithTimeline(@NonNull LocalDate date, @Nullable Timeline timeline) {
        this.date = date;
        if (timeline != null) {
            this.timeline.onNext(timeline);
        }
    }

    public Observable<Void> amendEventTime(@NonNull TimelineEvent event, @NonNull LocalTime newTime) {
        return latest().flatMap(timeline -> {
            String date = timeline.getDate().toString(ApiService.DATE_FORMAT);
            TimelineEvent.TimeAmendment timeAmendment = new TimelineEvent.TimeAmendment(newTime,
                    event.getTimezone().getOffset(event.getShiftedTimestamp()));
            return apiService.amendTimelineEventTime(date, event.getType(),
                    event.getRawTimestamp().getMillis(), timeAmendment)
                    .doOnNext(this.timeline::onNext)
                    .map(ignored -> null);
        });
    }

    public Observable<Void> verifyEvent(@NonNull TimelineEvent event) {
        return latest().flatMap(timeline -> {
            String date = timeline.getDate().toString(ApiService.DATE_FORMAT);
            return apiService.verifyTimelineEvent(date, event.getType(), event.getRawTimestamp().getMillis(), "")
                    .map(ignored -> null);
        });
    }

    public Observable<Void> deleteEvent(@NonNull TimelineEvent event) {
        return latest().flatMap(timeline -> {
            String date = timeline.getDate().toString(ApiService.DATE_FORMAT);
            return apiService.deleteTimelineEvent(date, event.getType(), event.getRawTimestamp().getMillis())
                    .doOnNext(this.timeline::onNext)
                    .map(ignored -> null);
        });
    }
}
