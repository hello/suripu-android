package is.hello.sense.graph.presenters;

import android.content.ComponentCallbacks2;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.LruCache;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Timeline;
import rx.Observable;
import rx.Scheduler;

public class ZoomedOutTimelinePresenter extends Presenter {
    private static final int CACHE_LIMIT = 10;

    private static final String STATE_KEY_FIRST_DATE = "firstDate";
    private LocalDate firstDate;

    private final List<DataView> dataViews = new ArrayList<>();
    private Scheduler updateScheduler = Rx.mainThreadScheduler();

    private final ApiService apiService;
    private final LruCache<LocalDate, Timeline> cachedTimelines = new LruCache<>(CACHE_LIMIT);


    //region Lifecycle

    @Inject public ZoomedOutTimelinePresenter(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        super.onRestoreState(savedState);

        final LocalDate firstDate = (LocalDate) savedState.getSerializable(STATE_KEY_FIRST_DATE);
        if (firstDate != null) {
            setFirstDate(firstDate);
        }
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle savedState = new Bundle();
        savedState.putSerializable(STATE_KEY_FIRST_DATE, firstDate);
        return savedState;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            cachedTimelines.evictAll();
        }
    }

    //endregion


    //region Dates

    public void setFirstDate(@NonNull LocalDate firstDate) {
        this.firstDate = firstDate;
        cachedTimelines.evictAll();
    }

    public @NonNull LocalDate getDateAt(int position) {
        return firstDate.minusDays(position);
    }

    public int getDatePosition(@NonNull LocalDate dateTime) {
        return Days.daysBetween(dateTime, firstDate).getDays() - 1;
    }

    //endregion


    //region Vending Timelines

    public void cacheTimeline(@NonNull LocalDate date, @Nullable Timeline timeline) {
        if (timeline != null) {
            cachedTimelines.put(date, timeline);
        } else {
            cachedTimelines.remove(date);
        }
    }

    public @Nullable Timeline getCachedTimeline(@NonNull LocalDate date) {
        return cachedTimelines.get(date);
    }

    public Observable<Timeline> retrieveTimeline(@NonNull LocalDate date) {
        return apiService.timelineForDate(date.toString(ApiService.DATE_FORMAT))
                         .observeOn(updateScheduler);
    }

    public Observable<Timeline> retrieveAndCacheTimeline(@NonNull LocalDate date) {
        Timeline existingTimeline = cachedTimelines.get(date);
        if (existingTimeline != null) {
            return Observable.just(existingTimeline);
        } else {
            return retrieveTimeline(date)
                    .map(timeline -> {
                        cacheTimeline(date, timeline);
                        return timeline;
                    });
        }
    }

    //endregion


    //region View holder hooks

    public void addDataView(@NonNull DataView presenterView) {
        dataViews.add(presenterView);
    }

    public void removeDataView(@NonNull DataView presenterView) {
        dataViews.remove(presenterView);
    }

    public void clearDataViews() {
        for(DataView dataView : dataViews){
            dataView.cancelAnimation(false);
        }
        dataViews.clear();
    }

    public void retrieveTimelines() {
        for (DataView dataView : dataViews) {
            if (!dataView.wantsUpdates()) {
                continue;
            }

            Observable<Timeline> timeline = retrieveAndCacheTimeline(dataView.getDate());
            timeline.subscribe(dataView::onUpdateAvailable, dataView::onUpdateFailed);
        }
    }

    //endregion


    //region For Tests

    @VisibleForTesting
    LruCache<LocalDate, Timeline> getCachedTimelines() {
        return cachedTimelines;
    }

    @VisibleForTesting
    void setUpdateScheduler(@NonNull Scheduler scheduler) {
        this.updateScheduler = scheduler;
    }

    //endregion


    public interface DataView {
        LocalDate getDate();
        boolean wantsUpdates();
        void onUpdateAvailable(@NonNull Timeline timeline);
        void onUpdateFailed(Throwable e);
        void cancelAnimation(boolean showLoading);
    }
}
