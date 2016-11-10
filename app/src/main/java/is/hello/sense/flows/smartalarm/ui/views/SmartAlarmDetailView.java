package is.hello.sense.flows.smartalarm.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;

@SuppressLint("ViewConstructor")
public class SmartAlarmDetailView extends PresenterView {
    public SmartAlarmDetailView(@NonNull final Activity activity) {
        super(activity);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_smart_alarm_detail;
    }

    @Override
    public void releaseViews() {

    }
}
