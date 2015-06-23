package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.fragments.HardwareFragment;

public class OnboardingUnsupportedDeviceFragment extends HardwareFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        OnboardingSimpleStepViewBuilder builder = new OnboardingSimpleStepViewBuilder(this, inflater, container);

        switch (hardwarePresenter.getDeviceSupportLevel()) {
            case UNSUPPORTED: {
                builder.setHeadingText(R.string.onboarding_title_unsupported_device);
                builder.setSubheadingText(R.string.onboarding_message_unsupported_device);
                break;
            }

            case UNTESTED: {
                builder.setHeadingText(R.string.onboarding_title_untested_device);
                builder.setSubheadingText(R.string.onboarding_message_untested_device);
                builder.initializeSubheadingSupportLinks(getActivity());

                break;
            }

            default: {
                continueAnyway();

                break;
            }
        }

        builder.setPrimaryButtonText(R.string.action_continue_anyway);
        builder.setPrimaryOnClickListener(ignored -> continueAnyway());
        builder.setToolbarWantsBackButton(true);
        builder.setToolbarWantsHelpButton(false);
        builder.setWantsSecondaryButton(false);

        return builder.create();
    }

    public void continueAnyway() {
        getOnboardingActivity().showRegistration(true);
    }
}
