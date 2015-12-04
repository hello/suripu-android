package is.hello.sense.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import javax.inject.Inject;

import is.hello.sense.SenseApplication;
import is.hello.sense.api.sessions.ApiSessionManager;

public class UpdateReceiver extends BroadcastReceiver {

    @Inject
    ApiSessionManager sessionManager;

    public UpdateReceiver() {
        if (!"robolectric".equals(Build.FINGERPRINT)) {
            SenseApplication.getInstance().inject(this);
        }
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (sessionManager.hasSession()) {
            Analytics.trackUserIdentifier(sessionManager.getSession().getAccountId());
        }
    }
}
