package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class Onboarding2ndPillInfoFragment extends OnboardingHardwareFragment {
    @Inject HardwarePresenter hardwarePresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.EVENT_ONBOARDING_GET_APP, null);

        putSenseIntoPairingMode();
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_2nd_pill_info, container, false);

        Button continueButton = (Button) view.findViewById(R.id.fragment_onboarding_2nd_pill_info_continue);
        Views.setSafeOnClickListener(continueButton, ignored -> ((OnboardingActivity) getActivity()).showSenseColorsInfo());

        return view;
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
