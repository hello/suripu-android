package is.hello.sense.ui.adapter;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.FragmentPageView;
import is.hello.sense.ui.widget.timeline.TimelinePlaceholderDrawable;
import is.hello.sense.util.DateFormatter;

public class TimelineFragmentAdapter implements FragmentPageView.Adapter<TimelineFragment> {
    private final Resources resources;
    private @Nullable Drawable placeholder;

    public TimelineFragmentAdapter(@NonNull Resources resources) {
        this.resources = resources;
    }


    //region Providing Fragments

    @Override
    public boolean hasFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return true;
    }

    @Override
    public TimelineFragment getFragmentBeforeFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDate().minusDays(1), null, false);
    }


    @Override
    public boolean hasFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        DateTime fragmentTime = fragment.getDate();
        return fragmentTime.isBefore(DateFormatter.lastNight().withTimeAtStartOfDay());
    }

    @Override
    public TimelineFragment getFragmentAfterFragment(@NonNull TimelineFragment fragment) {
        return TimelineFragment.newInstance(fragment.getDate().plusDays(1), null, false);
    }

    //endregion


    //region Chrome Callbacks

    @Nullable
    @Override
    public CharSequence getFragmentTitle(@NonNull TimelineFragment fragment) {
        return fragment.getTitle();
    }

    @Nullable
    @Override
    public Drawable getFragmentPlaceholder(@NonNull TimelineFragment fragment, @NonNull FragmentPageView.Position position) {
        if (placeholder == null) {
            this.placeholder = new TimelinePlaceholderDrawable(resources);
        }

        return placeholder;
    }

    public void clearPlaceholder() {
        this.placeholder = null;
    }

    //endregion
}
