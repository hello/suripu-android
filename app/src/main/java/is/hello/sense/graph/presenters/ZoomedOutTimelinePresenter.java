package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import rx.Observable;

public class ZoomedOutTimelinePresenter extends Presenter {
    private static final int CACHE_LIMIT = 7;

    private static final String STATE_KEY_FIRST_DATE = "firstDate";
    private DateTime firstDate;

    private final Map<Object, Runnable> posted = new HashMap<>();
    private boolean suspended = false;

    private final ApiService apiService;
    private final LruCache<DateTime, Timeline> cachedTimelines = new LruCache<>(CACHE_LIMIT);

    @Inject public ZoomedOutTimelinePresenter(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        super.onRestoreState(savedState);

        setFirstDate((DateTime) savedState.getSerializable(STATE_KEY_FIRST_DATE));
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle savedState = new Bundle();
        savedState.putSerializable(STATE_KEY_FIRST_DATE, firstDate);
        return savedState;
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        cachedTimelines.evictAll();
        return false;
    }


    public void setFirstDate(@NonNull DateTime firstDate) {
        this.firstDate = firstDate;
        cachedTimelines.evictAll();
    }

    public @NonNull DateTime getDateTimeAt(int position) {
        return firstDate.plusDays(-position).withTimeAtStartOfDay();
    }

    public int getDateTimePosition(@NonNull DateTime dateTime) {
        return Days.daysBetween(dateTime.toLocalDate(), firstDate.toLocalDate()).getDays() - 1;
    }


    public void suspend() {
        this.suspended = true;
    }

    public void resume() {
        this.suspended = false;

        for (Runnable task : posted.values()) {
            task.run();
        }
        posted.clear();
    }

    public void post(@NonNull Object tag, @NonNull Runnable task) {
        if (suspended) {
            posted.put(tag, task);
        } else {
            task.run();
        }
    }

    public void cancel(@NonNull Object tag) {
        posted.remove(tag);
    }

    public void cacheSingleTimeline(@NonNull DateTime date, @Nullable Timeline timeline) {
        if (timeline != null) {
            cachedTimelines.put(date, timeline);
        } else {
            cachedTimelines.remove(date);
        }
    }

    public @Nullable Timeline retrieveCachedTimeline(@NonNull DateTime date) {
        return cachedTimelines.get(date);
    }


    public Observable<Timeline> timelineForDate(@NonNull DateTime date) {
        Timeline cachedTimeline = retrieveCachedTimeline(date);
        if (cachedTimeline != null) {
            return Observable.just(cachedTimeline);
        } else if (suspended) {
            return Observable.error(new IllegalStateException("Cannot use timelineForDate when suspended"));
        } else {
            return apiService.timelineForDate(date.year().getAsString(), date.monthOfYear().getAsString(), date.dayOfMonth().getAsString())
                             .flatMap(ts -> ts.isEmpty() ? Observable.error(new IllegalArgumentException()) : Observable.just(ts.get(0)))
                             .doOnNext(timeline -> cacheSingleTimeline(date, timeline));
        }
    }


    //region For Tests

    LruCache<DateTime, Timeline> getCachedTimelines() {
        return cachedTimelines;
    }

    //endregion
}
