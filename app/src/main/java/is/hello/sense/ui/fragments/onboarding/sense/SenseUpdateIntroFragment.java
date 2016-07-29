package is.hello.sense.ui.fragments.onboarding.sense;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;
import is.hello.sense.util.Analytics;

public class SenseUpdateIntroFragment extends HardwareFragment {

    public static SenseUpdateIntroFragment newInstance() {
        return new SenseUpdateIntroFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_NO_BLE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_sense_update)
                .setSubheadingText(R.string.subtitle_sense_update)
                .setDiagramImage(R.drawable.onboarding_sense_ota)
                .setPrimaryButtonText(R.string.action_continue)
                .setPrimaryOnClickListener(this::onContinue)
                .setWantsSecondaryButton(false)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(
                        ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATING_SENSE));
    }


    public void onContinue(final View ignored) {
        getOnboardingActivity().showSenseUpdating();
    }
}
