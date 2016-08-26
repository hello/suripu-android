package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.fragments.BaseHardwareFragment;
import is.hello.sense.util.Analytics;

public class OnboardingUnsupportedDeviceFragment extends BaseHardwareFragment {
    private OnboardingSimpleStepView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_UNSUPPORTED_DEVICE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.onboarding_title_unsupported_device)
                .setSubheadingText(R.string.onboarding_message_unsupported_device)
                .initializeSupportLinksForSubheading(getActivity())
                .setPrimaryButtonText(R.string.action_continue_anyway)
                .setPrimaryOnClickListener(ignored -> continueAnyway())
                .setToolbarWantsBackButton(true)
                .setToolbarWantsHelpButton(false)
                .setWantsSecondaryButton(false);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.view.destroy();
        this.view = null;
    }

    public void continueAnyway() {
        finishFlow();
    }
}
