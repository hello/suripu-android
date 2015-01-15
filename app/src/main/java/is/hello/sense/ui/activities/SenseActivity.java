package is.hello.sense.ui.activities;

import android.app.Activity;

import is.hello.sense.util.Analytics;

public class SenseActivity extends Activity {
    @Override
    protected void onResume() {
        super.onResume();

        Analytics.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Analytics.onPause(this);
    }
}
