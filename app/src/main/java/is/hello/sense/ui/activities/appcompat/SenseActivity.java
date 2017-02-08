package is.hello.sense.ui.activities.appcompat;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.Analytics;

public abstract class SenseActivity extends AppCompatActivity {
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

    public void setStatusBarColorPrimary() {
        setStatusBarColorRes(R.color.primary);
    }

    public void setStatusBarColorRes(@ColorRes final int colorRes) {
        setStatusBarColor(ContextCompat.getColor(this, colorRes));
    }

    public void setStatusBarColor(final int color) {
        Windows.setStatusBarColor(getWindow(), color);
    }

    protected final void setActionBarText(@StringRes final int stringRes) {
        if (getSupportActionBar() == null) {
            return;
        }
        getSupportActionBar().setTitle(stringRes);
    }

    protected final void setActionBarHomeUpImage(@DrawableRes final int drawableRes) {
        if (getSupportActionBar() == null) {
            return;
        }
        getSupportActionBar().setHomeAsUpIndicator(drawableRes);
    }
}
