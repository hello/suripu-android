package is.hello.sense.bluetooth;

import android.app.Activity;

import is.hello.sense.ui.activities.PillUpdateActivity;
import no.nordicsemi.android.dfu.DfuBaseService;

public class DfuService extends DfuBaseService {
    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return PillUpdateActivity.class;
    }
}