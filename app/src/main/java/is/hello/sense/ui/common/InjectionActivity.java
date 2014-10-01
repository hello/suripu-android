package is.hello.sense.ui.common;

import android.support.v4.app.FragmentActivity;

import is.hello.sense.SenseApplication;

public class InjectionActivity extends FragmentActivity {
    public InjectionActivity() {
        SenseApplication.getInstance().inject(this);
    }
}
