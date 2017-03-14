package is.hello.sense.ui.adapter;

import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.generic.ui.adapters.BaseFragmentPagerAdapter;
import is.hello.sense.flows.home.ui.fragments.TimelineFragment;
import is.hello.sense.util.DateFormatter;

public class TimelineFragmentAdapter extends BaseFragmentPagerAdapter {
    private final LocalDate oldestDate;
    private int count;
    @VisibleForTesting
    LocalDate latestDate;
    @VisibleForTesting
    @Nullable
    Timeline cachedTimeline;


    public TimelineFragmentAdapter(@NonNull final FragmentManager fragmentManager,
                                   final int containerId,
                                   @NonNull final LocalDate oldestDate) {
        super(fragmentManager, containerId);

        final LocalDate today = DateFormatter.todayForTimeline();
        final int dateRelation = today.compareTo(oldestDate);
        if (dateRelation < 0) {
            this.oldestDate = today.minusDays(1);
        } else if (dateRelation == 0) {
            this.oldestDate = oldestDate.minusDays(1);
        } else {
            this.oldestDate = oldestDate;
        }
        setLatestDate(today);
    }

    //region DavidsBaseFragmentPagerAdapter
    @Override
    public int getCount() {
        return count;
    }

    @NonNull
    @Override
    public TimelineFragment getItem(final int position) {
        final LocalDate timelineDate = getItemDate(position);

        final Timeline cachedTimeline;
        if (this.cachedTimeline == null || !timelineDate.equals(this.cachedTimeline.getDate())) {
            cachedTimeline = null;
        } else {
            cachedTimeline = this.cachedTimeline;
            this.cachedTimeline = null;
        }

        return TimelineFragment.newInstance(timelineDate,
                                            cachedTimeline);
    }
    //endregion

    //region Attributes

    public void setCachedTimeline(@Nullable final Timeline cachedTimeline) {
        this.cachedTimeline = cachedTimeline;
    }

    public void ensureLatestDateIsLastNight() {
        LocalDate today = DateFormatter.todayForTimeline();
        if (latestDate.isBefore(today)) {
            setLatestDate(today);
        }
    }

    public void setLatestDate(@NonNull final LocalDate latestDate) {
        this.latestDate = latestDate;
        this.count = Days.daysBetween(oldestDate, this.latestDate).getDays();
        notifyDataSetChanged();
    }

    public int getDatePosition(@NonNull final LocalDate date) {
        return Days.daysBetween(oldestDate, date).getDays();
    }

    public int getLastNight() {
        ensureLatestDateIsLastNight();
        return getCount() - 1;
    }

    //endregion


    //region Binding

    public LocalDate getItemDate(final int position) {
        return oldestDate.plusDays(position);
    }


    @Nullable
    public TimelineFragment getCurrentTimeline() {
        return (TimelineFragment) findCurrentFragment();
    }

    //endregion
}
