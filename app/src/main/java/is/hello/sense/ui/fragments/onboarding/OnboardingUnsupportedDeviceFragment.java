package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.HardwareFragment;

public class OnboardingUnsupportedDeviceFragment extends HardwareFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        OnboardingSimpleStepViewBuilder builder = new OnboardingSimpleStepViewBuilder(this, inflater, container);

        if (hardwarePresenter.getDeviceSupportLevel() == BluetoothStack.SupportLevel.UNSUPPORTED_OS) {
            builder.setHeadingText(R.string.onboarding_title_unsupported_os);
            builder.setSubheadingText(R.string.onboarding_message_unsupported_os);

            builder.setSecondaryButtonText(R.string.action_open_phone_settings);
            builder.setSecondaryOnClickListener(this::checkForUpdates);
        } else {
            builder.setHeadingText(R.string.onboarding_title_unsupported_device);
            builder.setSubheadingText(R.string.onboarding_message_unsupported_device);

            builder.setWantsSecondaryButton(false);
        }

        builder.setPrimaryButtonText(R.string.action_continue_anyway);
        builder.setPrimaryOnClickListener(this::continueAnyway);
        builder.setToolbarOnHelpClickListener(this::showHelp);

        return builder.create();
    }

    public void checkForUpdates(@NonNull View sender) {
        startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
    }

    public void continueAnyway(@NonNull View sender) {
        getOnboardingActivity().showSetupSense(true);
    }

    public void showHelp(@NonNull View sender) {
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UNSUPPORTED_DEVICE);
    }
}
