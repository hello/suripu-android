package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.segment.analytics.Properties;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.R;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.util.Analytics;

public class OnboardingUnsupportedDeviceFragment extends HardwareFragment {
    private BluetoothStack.SupportLevel supportLevel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.supportLevel = hardwarePresenter.getDeviceSupportLevel();

        Properties properties = Analytics.createProperties(
            Analytics.Onboarding.PROP_DEVICE_SUPPORT_LEVEL, supportLevel.toString()
        );
        Analytics.trackEvent(Analytics.Onboarding.EVENT_UNSUPPORTED_DEVICE, properties);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        OnboardingSimpleStepView stepView = new OnboardingSimpleStepView(this, inflater);

        switch (supportLevel) {
            case UNSUPPORTED: {
                stepView.setHeadingText(R.string.onboarding_title_unsupported_device);
                stepView.setSubheadingText(R.string.onboarding_message_unsupported_device);
                break;
            }

            case UNTESTED: {
                stepView.setHeadingText(R.string.onboarding_title_untested_device);
                stepView.setSubheadingText(R.string.onboarding_message_untested_device);
                stepView.initializeSupportLinksForSubheading(getActivity());
                break;
            }

            default: {
                continueAnyway();
                break;
            }
        }

        stepView.setPrimaryButtonText(R.string.action_continue_anyway);
        stepView.setPrimaryOnClickListener(ignored -> continueAnyway());
        stepView.setToolbarWantsBackButton(true);
        stepView.setToolbarWantsHelpButton(false);
        stepView.setWantsSecondaryButton(false);

        return stepView;
    }

    public void continueAnyway() {
        getOnboardingActivity().showGetStarted(true);
    }
}
