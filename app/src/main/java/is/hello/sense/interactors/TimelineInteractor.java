package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class TimelineInteractor extends ValueInteractor<Timeline> {
    @Inject ApiService apiService;

    /**
     * The current date of timeline calling {@link TimelineInteractor#update()} will fetch
     */
    private LocalDate date;

    private static final LruCache<LocalDate, Boolean> validTimelineCache = new LruCache<>(3);

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
        return apiService.timelineForDate(date.toString(ApiService.DATE_FORMAT))
                         .doOnNext(this::saveToCache);
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDateWithTimeline(@NonNull LocalDate date, @Nullable Timeline timeline) {
        this.date = date;
        if (timeline != null) {
            this.timeline.onNext(timeline);
            this.saveToCache(timeline);
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

    public boolean hasValidTimeline() {
        return hasValidTimeline(timeline.getValue());
    }

    public synchronized boolean hasValidTimeline(@NonNull final LocalDate localDate) {
        final Boolean isValid = validTimelineCache.get(localDate);
        return isValid == null ? false : isValid;
    }

    public static boolean hasValidCondition(@NonNull final Timeline timeline){
        return timeline.getScoreCondition() != ScoreCondition.UNAVAILABLE &&
                timeline.getScoreCondition() != ScoreCondition.INCOMPLETE;
    }

    public synchronized void clearCache(){
        validTimelineCache.evictAll();
    }

    private static boolean hasValidTimeline(@Nullable final Timeline t) {
        return t != null
                && t.getScore() != null
                && t.getScore() > 0;
    }

    private void saveToCache(@Nullable final Timeline timeline) {
        final Boolean isValid = TimelineInteractor.hasValidTimeline(timeline);

        TimelineInteractor.this.saveToCache(date,
                                            isValid);

    }

    private synchronized void saveToCache(@NonNull final LocalDate date,
                                          @NonNull final Boolean isValidTimeline){
        validTimelineCache.put(date, isValidTimeline);
    }
}
