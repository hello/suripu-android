package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONObject;

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

        JSONObject properties = Analytics.createProperties(
            Analytics.Onboarding.PROP_DEVICE_SUPPORT_LEVEL, supportLevel.toString()
        );
        Analytics.trackEvent(Analytics.Onboarding.EVENT_UNSUPPORTED_DEVICE, properties);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        OnboardingSimpleStepViewBuilder builder = new OnboardingSimpleStepViewBuilder(this, inflater, container);

        switch (supportLevel) {
            case UNSUPPORTED: {
                builder.setHeadingText(R.string.onboarding_title_unsupported_device);
                builder.setSubheadingText(R.string.onboarding_message_unsupported_device);
                break;
            }

            case UNTESTED: {
                builder.setHeadingText(R.string.onboarding_title_untested_device);
                builder.setSubheadingText(R.string.onboarding_message_untested_device);
                builder.initializeSupportLinksForSubheading(getActivity());
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
