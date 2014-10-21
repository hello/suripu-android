package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;

import javax.inject.Inject;

import is.hello.sense.graph.presenters.WifiNetworkPresenter;
import is.hello.sense.ui.common.InjectionFragment;

public class OnboardingWifiNetworkFragment extends InjectionFragment {
    @Inject WifiNetworkPresenter networkPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        networkPresenter.update();
        addPresenter(networkPresenter);

        setRetainInstance(true);
    }
}
