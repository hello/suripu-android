package is.hello.sense.ui.common;

import android.support.v4.app.DialogFragment;

import is.hello.sense.SenseApplication;

public class InjectionDialogFragment extends DialogFragment {
    public InjectionDialogFragment() {
        SenseApplication.getInstance().inject(this);
    }
}
