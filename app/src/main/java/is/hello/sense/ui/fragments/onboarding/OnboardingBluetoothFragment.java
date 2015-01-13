package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.util.Views;
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

        Analytics.trackEvent(Analytics.EVENT_ONBOARDING_NO_BLE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_bluetooth, container, false);

        Button turnOn = (Button) view.findViewById(R.id.fragment_onboarding_bluetooth_turn_on);
        Views.setSafeOnClickListener(turnOn, this::turnOn);

        OnboardingToolbar.of(this, view)
                .setWantsBackButton(false)
                .setOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.BLUETOOTH));

        return view;
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
                getOnboardingActivity().showSetupSense();
            }
        });
    }

    public void turnOn(@NonNull View sender) {
        showBlockingActivity(R.string.title_turning_on);
        bindAndSubscribe(hardwarePresenter.turnOnBluetooth(), ignored -> {}, this::presentError);
    }

    public void presentError(Throwable e) {
        hideBlockingActivity(false, () -> {
            if (hardwarePresenter.isErrorFatal(e)) {
                UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
                fragment.show(getFragmentManager(), R.id.activity_onboarding_container);
            } else {
                ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
            }
        });
    }
}
