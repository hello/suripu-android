package is.hello.sense.flows.smartalarm.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.ui.activities.ScopedInjectionActivity;

public class SmartAlarmDetailActivity extends ScopedInjectionActivity {

    public static final String EXTRA_ALARM = SmartAlarmDetailActivity.class.getName() + ".ARG_ALARM";
    public static final String EXTRA_INDEX = SmartAlarmDetailActivity.class.getName() + ".ARG_INDEX";

    public static void startActivity(@NonNull final Context context,
                                     @NonNull final Alarm alarm,
                                     final int index) {
        final Intent intent = new Intent(context, SmartAlarmDetailActivity.class);
        intent.putExtra(EXTRA_ALARM, alarm);
        intent.putExtra(EXTRA_INDEX, index);
        context.startActivity(intent);
    }

    @Override
    protected List<Object> getModules() {
        return new ArrayList<>();
    }
}
