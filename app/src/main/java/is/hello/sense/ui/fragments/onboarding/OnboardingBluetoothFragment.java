package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;

public class OnboardingBluetoothFragment extends InjectionFragment {
    private static final String ARG_IS_BEFORE_BIRTHDAY = OnboardingBluetoothFragment.class.getName() + ".ARG_IS_BEFORE_BIRTHDAY";

    @Inject BluetoothStack bluetoothStack;

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
        return new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.action_turn_on_ble)
                .setSubheadingText(R.string.info_turn_on_bluetooth)
                .setPrimaryButtonText(R.string.action_turn_on_ble)
                .setPrimaryOnClickListener(this::turnOn)
                .setWantsSecondaryButton(false)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.BLUETOOTH));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(bluetoothStack.enabled().filter(Functions.IS_TRUE),
                         ignored -> done(),
                         Functions.LOG_ERROR);
    }

    public void done() {
        LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
            getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null);
        });
    }

    public void turnOn(@NonNull View sender) {
        LoadingDialogFragment.show(getFragmentManager(),
                                   getString(R.string.title_turning_on),
                                   LoadingDialogFragment.OPAQUE_BACKGROUND);
        bindAndSubscribe(bluetoothStack.turnOn(),
                         Functions.NO_OP,
                         this::presentError);
    }

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        final ErrorDialogFragment errorDialogFragment =
                new ErrorDialogFragment.Builder(e, getResources())
                        .withSupportLink()
                        .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }


    public boolean isBeforeBirthday() {
        return getArguments().getBoolean(ARG_IS_BEFORE_BIRTHDAY, false);
    }
}
