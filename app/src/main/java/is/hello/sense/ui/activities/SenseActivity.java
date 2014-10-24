package is.hello.sense.ui.activities;

import android.app.Activity;
import android.content.Context;

import com.amplitude.api.Amplitude;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SenseActivity extends Activity {
    @Override
    protected void onResume() {
        super.onResume();
        Amplitude.startSession();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Amplitude.endSession();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }
}
