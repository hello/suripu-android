package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;

import javax.inject.Inject;

import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.common.InjectionFragment;

public class MyInfoFragment extends InjectionFragment {
    @Inject AccountPresenter accountPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(accountPresenter);
        setRetainInstance(true);
    }
}
