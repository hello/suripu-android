package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.util.Analytics;

public class OnboardingBluetoothFragment extends HardwareFragment {
    private static final String ARG_IS_BEFORE_BIRTHDAY = OnboardingBluetoothFragment.class.getName() + ".ARG_IS_BEFORE_BIRTHDAY";

    public static OnboardingBluetoothFragment newInstance(boolean isBeforeBirthday) {
        OnboardingBluetoothFragment fragment = new OnboardingBluetoothFragment();

        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_IS_BEFORE_BIRTHDAY, isBeforeBirthday);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_NO_BLE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepViewBuilder(this, inflater, container)
                .setHeadingText(R.string.action_turn_on_ble)
                .setSubheadingText(R.string.info_turn_on_bluetooth)
                .setPrimaryButtonText(R.string.action_turn_on_ble)
                .setPrimaryOnClickListener(this::turnOn)
                .setWantsSecondaryButton(false)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.BLUETOOTH))
                .create();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(hardwarePresenter.bluetoothEnabled.filter(Functions.IS_TRUE),
                         ignored -> done(),
                         Functions.LOG_ERROR);
    }

    public void done() {
        hideBlockingActivity(true, () -> {
            if (getArguments().getBoolean(ARG_IS_BEFORE_BIRTHDAY, false)) {
                getOnboardingActivity().showBirthday(null);
            } else {
                getOnboardingActivity().showSetupSense(false);
            }
        });
    }

    public void turnOn(@NonNull View sender) {
        showBlockingActivity(R.string.title_turning_on);
        bindAndSubscribe(hardwarePresenter.turnOnBluetooth(), ignored -> {}, this::presentError);
    }

    public void presentError(Throwable e) {
        hideBlockingActivity(false, () -> {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        });
    }
}
