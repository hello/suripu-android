package is.hello.sense.ui.common;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import is.hello.sense.SenseApplication;
import rx.Observable;
import rx.Subscription;
import rx.android.observables.AndroidObservable;

public class InjectionFragment extends Fragment {
    public InjectionFragment() {
        SenseApplication.getInstance().inject(this);
    }
}
