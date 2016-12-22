package is.hello.sense.flows.home.ui.fragments;
import android.support.annotation.NonNull;

import is.hello.sense.api.model.v2.Trends.TimeScale;

public class WeekTrendsFragment extends TrendsFragment {
    @NonNull
    @Override
    protected TimeScale getTimeScale() {
        return TimeScale.LAST_WEEK;
    }
}
