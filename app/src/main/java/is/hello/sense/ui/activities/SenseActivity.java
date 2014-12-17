package is.hello.sense.ui.activities;

import android.app.Activity;
import android.content.Context;

import is.hello.sense.util.Analytics;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SenseActivity extends Activity {
    @Override
    protected void onResume() {
        super.onResume();
        Analytics.startSession();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Analytics.endSession();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }
}
