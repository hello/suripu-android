package is.hello.sense.ui.activities;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Window;

import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.mvp.modules.SensorDetailModule;
import is.hello.sense.mvp.presenters.SensorDetailFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Windows;

public class SensorDetailActivity extends ScopedInjectionActivity
        implements FragmentNavigation {
    private static final String EXTRA_SENSOR = SensorDetailActivity.class.getName() + ".EXTRA_SENSOR";
    private FragmentNavigationDelegate navigationDelegate;

    public static void startActivity(@NonNull final Context context,
                                     @NonNull final Sensor sensor) {
        final Intent intent = new Intent(context, SensorDetailActivity.class);
        intent.putExtra(EXTRA_SENSOR, sensor);
        context.startActivity(intent);
    }

    @Override
    protected final List<Object> getModules() {
        return Collections.singletonList(new SensorDetailModule());
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        this.navigationDelegate = new FragmentNavigationDelegate(this,
                                                                 R.id.activity_navigation_container,
                                                                 stateSafeExecutor);

        final Sensor sensor = (Sensor) getIntent().getSerializableExtra(EXTRA_SENSOR);
        final int color = ContextCompat.getColor(this, sensor.getColor());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = getWindow();
            Windows.setStatusBarColor(window, Drawing.darkenColorBy(color, .2f));
        }

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(sensor.getName());
            actionBar.setBackgroundDrawable(new ColorDrawableCompat(color));
        }

        showSensorDetailFragment(sensor);

    }

    @Override
    public final void pushFragment(@NonNull final Fragment fragment, @Nullable final String title, final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public final void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment, @Nullable final String title, final boolean wantsBackStackEntry) {
        navigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public final void popFragment(@NonNull final Fragment fragment, final boolean immediate) {
        navigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public final void flowFinished(@NonNull final Fragment fragment, final int responseCode, @Nullable final Intent result) {

    }

    @Nullable
    @Override
    public final Fragment getTopFragment() {
        return navigationDelegate.getTopFragment();
    }


    private void showSensorDetailFragment(@NonNull final Sensor sensor) {
        navigationDelegate.pushFragment(SensorDetailFragment.createFragment(sensor), null, false);
    }
}
