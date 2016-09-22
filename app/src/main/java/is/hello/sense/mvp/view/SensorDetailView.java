package is.hello.sense.mvp.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.ui.widget.SelectorView;

@SuppressLint("ViewConstructor")
public final class SensorDetailView extends PresenterView {
    private final SelectorView subNavSelector;

    public SensorDetailView(@NonNull final Activity activity,
                            final int color) {
        super(activity);
        this.subNavSelector = (SelectorView) findViewById(R.id.fragment_sensor_detail_selector);
        this.subNavSelector.setToggleButtonColor(R.color.white);
        this.subNavSelector.addOption(R.string.sensor_detail_last_day, false)
                           .setBackgroundColor(color);
        this.subNavSelector.addOption(R.string.sensor_detail_past_week, false)
                           .setBackgroundColor(color);
        this.subNavSelector.setBackgroundColor(color);
    }


    @Override
    protected final int getLayoutRes() {
        return R.layout.fragment_sensor_detail;
    }

    @Override
    public final void releaseViews() {

    }
}
