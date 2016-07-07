package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.util.Analytics;

public class BluetoothFragment extends HardwareFragment {
    private static final String ARG_NEXT_SCREEN_ID = BluetoothFragment.class.getName() + ".ARG_NEXT_SCREEN_ID";

    public static BluetoothFragment newInstance(final int nextScreenId) {
        final BluetoothFragment fragment = new BluetoothFragment();

        final Bundle arguments = new Bundle();
        arguments.putInt(ARG_NEXT_SCREEN_ID, nextScreenId);
        fragment.setArguments(arguments);

        return fragment;
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
                .setHeadingText(R.string.action_turn_on_ble)
                .setSubheadingText(R.string.info_turn_on_bluetooth)
                .setPrimaryButtonText(R.string.action_turn_on_ble)
                .setPrimaryOnClickListener(this::turnOn)
                .setWantsSecondaryButton(false)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.BLUETOOTH));
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(hardwarePresenter.bluetoothEnabled.filter(Functions.IS_TRUE),
                         ignored -> done(),
                         Functions.LOG_ERROR);
    }



    public void done() {
        hideBlockingActivity(true, () -> {
            final int nextScreenId = getArguments().getInt(ARG_NEXT_SCREEN_ID,-1);
            if(nextScreenId != -1 && getActivity() instanceof FragmentNavigation){
                ((FragmentNavigation) getActivity()).flowFinished(this, nextScreenId, null);
            } else{
                finishWithResult(Activity.RESULT_CANCELED, null);
            }
        });
    }

    public void turnOn(@NonNull final View sender) {
        showBlockingActivity(R.string.title_turning_on);
        bindAndSubscribe(hardwarePresenter.turnOnBluetooth(), ignored -> {}, this::presentError);
    }

    public void presentError(Throwable e) {
        hideBlockingActivity(false, () -> {
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                    .withSupportLink()
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }
}
