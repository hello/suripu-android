package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.FragmentPageView;
import is.hello.sense.util.DateFormatter;

public class TimelineFragmentAdapter implements FragmentPageView.Adapter<TimelineFragment> {
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
}
