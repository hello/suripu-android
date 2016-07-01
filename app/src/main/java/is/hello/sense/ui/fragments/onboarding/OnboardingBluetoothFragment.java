package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.util.Analytics;

public class OnboardingBluetoothFragment extends HardwareFragment {
    private static final String ARG_NEXT_SCREEN_ID = OnboardingBluetoothFragment.class.getName() + ".ARG_NEXT_SCREEN_ID";
    public static final int BIRTHDAY_SCREEN = 1;
    public static final int SETUP_SENSE_SCREEN = 2;
    public static final int UPDATE_PILL_SCREEN = 3;

    public static OnboardingBluetoothFragment newInstance(final int nextScreenId) {
        final OnboardingBluetoothFragment fragment = new OnboardingBluetoothFragment();

        final Bundle arguments = new Bundle();
        arguments.putInt(ARG_NEXT_SCREEN_ID, nextScreenId);
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

        bindAndSubscribe(hardwarePresenter.bluetoothEnabled.filter(Functions.IS_TRUE),
                         ignored -> done(),
                         Functions.LOG_ERROR);
    }

    public void done() {
        hideBlockingActivity(true, () -> {
            final int nextScreenId = getArguments().getInt(ARG_NEXT_SCREEN_ID,-1);
            //Todo refactor so onboarding activity can execute an int action mapped to a runnable instead.
            switch(nextScreenId){
                case BIRTHDAY_SCREEN:
                    getOnboardingActivity().showBirthday(null, true);
                    break;
                case SETUP_SENSE_SCREEN:
                    getOnboardingActivity().showSetupSense();
                    break;
                case UPDATE_PILL_SCREEN:
                    ((PillUpdateActivity) getActivity()).showUpdateIntroPill();
                    break;
                default:
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
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                    .withSupportLink()
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }
}
