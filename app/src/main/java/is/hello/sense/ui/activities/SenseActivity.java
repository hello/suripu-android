package is.hello.sense.ui.activities;

import android.app.Activity;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.Analytics;

public abstract class SenseActivity extends Activity {
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

    public void setStatusBarColor() {
        setStatusBarColorRes(R.color.primary);
    }

    public void setStatusBarColorRes(@ColorRes final int colorRes) {
        setStatusBarColor(ContextCompat.getColor(this, colorRes));
    }

    public void setStatusBarColor(final int color) {
        Windows.setStatusBarColor(getWindow(), color);
    }
}
