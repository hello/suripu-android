package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;

import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.util.Analytics;

public class NotificationsSettingsFragment extends SenseFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_NOTIFICATIONS, null);
        }
    }
}
