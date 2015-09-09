package is.hello.sense.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.ui.fragments.TimelineFragment;

public class TimelineFragmentAdapter extends FragmentPagerAdapter {
    private final LocalDate oldestDate;
    private int count;
    @VisibleForTesting LocalDate latestDate;
    @VisibleForTesting @Nullable Timeline cachedTimeline;
    @VisibleForTesting boolean firstTimeline = true;


    //region Lifecycle

    public TimelineFragmentAdapter(@NonNull FragmentManager fragmentManager,
                                   @NonNull LocalDate oldestDate) {
        super(fragmentManager);
        this.oldestDate = oldestDate;
        setLatestDate(LocalDate.now());
    }

    @Override
    public Parcelable saveState() {
        Bundle savedState = new Bundle();
        savedState.putBoolean("firstTimeline", firstTimeline);
        return savedState;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        Bundle savedState = (Bundle) state;
        this.firstTimeline = savedState.getBoolean("firstTimeline", false);
    }

    //endregion


    //region Attributes

    public void setCachedTimeline(@Nullable Timeline cachedTimeline) {
        this.cachedTimeline = cachedTimeline;
    }

    public void ensureLatestDateIsToday() {
        LocalDate today = LocalDate.now();
        if (latestDate.isBefore(today)) {
            setLatestDate(today);
        }
    }

    public void setLatestDate(@NonNull LocalDate latestDate) {
        if (latestDate.equals(oldestDate)) {
            this.latestDate = oldestDate.plusDays(1);
        } else {
            this.latestDate = latestDate;
        }
        this.count = Days.daysBetween(oldestDate, this.latestDate).getDays();
        notifyDataSetChanged();
    }

    public int getDatePosition(@NonNull LocalDate date) {
        return Days.daysBetween(oldestDate, date).getDays();
    }

    public int getLastNight() {
        ensureLatestDateIsToday();
        return getCount() - 1;
    }

    //endregion


    //region Binding

    @Override
    public int getCount() {
        return count;
    }

    public LocalDate getItemDate(int position) {
        return oldestDate.plusDays(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        final LocalDate timelineDate = getItemDate(position);
        final boolean firstTimeline = this.firstTimeline;
        this.firstTimeline = false;

        final Timeline cachedTimeline;
        if (this.cachedTimeline == null || !timelineDate.equals(this.cachedTimeline.getDate())) {
            cachedTimeline = null;
        } else {
            cachedTimeline = this.cachedTimeline;
            this.cachedTimeline = null;
        }

        return TimelineFragment.newInstance(timelineDate,
                                            cachedTimeline,
                                            firstTimeline);
    }

    //endregion
}
