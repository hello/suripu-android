package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class Onboarding2ndPillInfoFragment extends HardwareFragment {
    @Inject HardwarePresenter hardwarePresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_GET_APP, null);

        putSenseIntoPairingMode();
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepViewBuilder(this, inflater, container)
                .setHeadingText(R.string.title_onboarding_2nd_pill_info)
                .setSubheadingText(R.string.info_onboarding_2nd_pill_info)
                .setPrimaryOnClickListener(ignored -> getOnboardingActivity().showDone())
                .setWantsSecondaryButton(false)
                .hideToolbar()
                .create();
    }


    public void putSenseIntoPairingMode() {
        if (hardwarePresenter.getPeripheral() == null) {
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(), ignored -> putSenseIntoPairingMode(), Functions.LOG_ERROR);
            return;
        }

        if (!hardwarePresenter.getPeripheral().isConnected()) {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(hardwarePresenter.getPeripheral()), ignored -> putSenseIntoPairingMode(), Functions.LOG_ERROR);
            return;
        }

        bindAndSubscribe(hardwarePresenter.putIntoPairingMode(),
                ignored -> Logger.info(Onboarding2ndPillInfoFragment.class.getSimpleName(), "Sense is now in pairing mode"),
                Functions.LOG_ERROR);
    }
}
