package is.hello.sense.ui.common;

import is.hello.sense.SenseApplication;
import is.hello.sense.ui.activities.SenseActivity;

public class InjectionActivity extends SenseActivity {
    public InjectionActivity() {
        SenseApplication.getInstance().inject(this);
    }
}
