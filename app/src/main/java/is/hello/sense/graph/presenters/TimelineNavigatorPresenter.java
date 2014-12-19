package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import rx.Observable;

public class TimelineNavigatorPresenter extends Presenter {
    private static final int CACHE_LIMIT = 7;

    private static final String STATE_KEY_START_TIME = "startTime";
    private DateTime startTime;

    private final Map<Object, Runnable> posted = new HashMap<>();
    private boolean suspended = false;

    private final ApiService apiService;
    private final LruCache<DateTime, Timeline> cachedTimelines = new LruCache<>(CACHE_LIMIT);

    @Inject public TimelineNavigatorPresenter(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void onRestoreState(@NonNull Parcelable savedState) {
        super.onRestoreState(savedState);

        if (savedState instanceof Bundle) {
            Bundle state = (Bundle) savedState;
            setStartTime((DateTime) state.getSerializable(STATE_KEY_START_TIME));
        }
    }

    @Nullable
    @Override
    public Parcelable onSaveState() {
        Bundle savedState = new Bundle();
        savedState.putSerializable(STATE_KEY_START_TIME, startTime);
        return savedState;
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        cachedTimelines.evictAll();
        return false;
    }


    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public @NonNull DateTime getDateTimeAt(int position) {
        return startTime.plusDays(-position);
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


    public Observable<Timeline> timelineForDate(@NonNull DateTime date) {
        Timeline cachedTimeline = cachedTimelines.get(date);
        if (cachedTimeline != null) {
            return Observable.just(cachedTimeline);
        } else {
            return apiService.timelineForDate(date.year().getAsString(), date.monthOfYear().getAsString(), date.dayOfMonth().getAsString())
                             .flatMap(ts -> ts.isEmpty() ? Observable.error(new IllegalArgumentException()) : Observable.just(ts.get(0)))
                             .doOnNext(timeline -> cachedTimelines.put(date, timeline));
        }
    }
}
