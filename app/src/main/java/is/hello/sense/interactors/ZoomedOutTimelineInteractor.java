package is.hello.sense.interactors;

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

public class ZoomedOutTimelineInteractor extends Interactor {
    private static final int CACHE_LIMIT = 10;

    private static final String STATE_KEY_FIRST_DATE = "firstDate";
    private LocalDate firstDate;

    private final List<DataView> dataViews = new ArrayList<>();
    private Scheduler updateScheduler = Rx.mainThreadScheduler();

    private final ApiService apiService;
    private final LruCache<LocalDate, Timeline> cachedTimelines = new LruCache<>(CACHE_LIMIT);


    //region Lifecycle

    @Inject
    public ZoomedOutTimelineInteractor(@NonNull final ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);

        final LocalDate firstDate = (LocalDate) savedState.getSerializable(STATE_KEY_FIRST_DATE);
        if (firstDate != null) {
            setFirstDate(firstDate);
        }
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        final Bundle savedState = new Bundle();
        savedState.putSerializable(STATE_KEY_FIRST_DATE, firstDate);
        return savedState;
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            this.cachedTimelines.evictAll();
        }
    }

    //endregion


    //region Dates

    public void setFirstDate(@NonNull final LocalDate firstDate) {
        this.firstDate = firstDate;
        this.cachedTimelines.evictAll();
    }

    @NonNull
    public LocalDate getDateAt(final int position) {
        return this.firstDate.minusDays(position);
    }

    public int getDatePosition(@NonNull final LocalDate dateTime) {
        return Days.daysBetween(dateTime, this.firstDate).getDays() - 1;
    }

    //endregion


    //region Vending Timelines

    public void cacheTimeline(@NonNull final LocalDate date,
                              @Nullable final Timeline timeline) {
        if (timeline != null) {
            this.cachedTimelines.put(date, timeline);
        } else {
            this.cachedTimelines.remove(date);
        }
    }

    @Nullable
    public Timeline getCachedTimeline(@NonNull final LocalDate date) {
        return this.cachedTimelines.get(date);
    }

    private Observable<Timeline> retrieveTimeline(@NonNull final LocalDate date) {
        return this.apiService.timelineForDate(date.toString(ApiService.DATE_FORMAT))
                              .observeOn(this.updateScheduler);
    }

    @VisibleForTesting
    Observable<Timeline> retrieveAndCacheTimeline(@NonNull final LocalDate date) {
        final Timeline existingTimeline = this.cachedTimelines.get(date);
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

    public void addDataView(@NonNull final DataView presenterView) {
        this.dataViews.add(presenterView);
    }

    public void removeDataView(@NonNull final DataView presenterView) {
        this.dataViews.remove(presenterView);
    }

    public void clearDataViews() {
        for (final DataView dataView : this.dataViews) {
            dataView.cancelAnimation(false);
        }
        this.dataViews.clear();
    }

    public void retrieveTimelines() {
        for (final DataView dataView : this.dataViews) {
            if (!dataView.wantsUpdates()) {
                continue;
            }

            final Observable<Timeline> timeline = retrieveAndCacheTimeline(dataView.getDate());
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
    void setUpdateScheduler(@NonNull final Scheduler scheduler) {
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
