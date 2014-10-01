package is.hello.sense.ui.common;

import android.support.v4.app.Fragment;

import is.hello.sense.SenseApplication;

public class InjectionFragment extends Fragment {
    public InjectionFragment() {
        SenseApplication.getInstance().inject(this);
    }
}
