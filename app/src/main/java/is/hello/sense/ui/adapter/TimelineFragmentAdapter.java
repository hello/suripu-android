package is.hello.sense.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.ui.fragments.TimelineFragment;

public class TimelineFragmentAdapter extends FragmentPagerAdapter {
    // Real epoch is `2015-02-14 21:28:00`
    public static final LocalDate EPOCH = new LocalDate(2014, 1, 1);

    private int count;
    private LocalDate latestDate;
    private @Nullable Timeline cachedTimeline;
    private boolean firstTimeline = true;


    //region Lifecycle

    public TimelineFragmentAdapter(@NonNull FragmentManager fragmentManager) {
        super(fragmentManager);
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
        this.latestDate = latestDate;
        this.count = Days.daysBetween(EPOCH, latestDate).getDays();
        notifyDataSetChanged();
    }

    public int getDatePosition(@NonNull LocalDate date) {
        return Days.daysBetween(EPOCH, date).getDays();
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
        return EPOCH.plusDays(position);
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
