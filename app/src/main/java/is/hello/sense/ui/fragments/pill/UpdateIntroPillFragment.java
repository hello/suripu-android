package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.R;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.graph.presenters.PhoneBatteryPresenter;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;
import is.hello.sense.util.Analytics;

public class UpdateIntroPillFragment extends PillHardwareFragment {
    @Inject
    BluetoothStack bluetoothStack;
    @Inject
    PhoneBatteryPresenter phoneBatteryPresenter;

    private static final String ARG_NEXT_SCREEN_ID = UpdateIntroPillFragment.class.getName() + ".ARG_NEXT_SCREEN_ID";

    private Button primaryButton;

    private ViewAnimator viewAnimator;

    public static UpdateIntroPillFragment newInstance(final int nextScreenId) {
        final UpdateIntroPillFragment fragment = new UpdateIntroPillFragment();

        final Bundle arguments = new Bundle();
        arguments.putInt(ARG_NEXT_SCREEN_ID, nextScreenId);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.PillUpdate.EVENT_START, null);
        addPresenter(devicesPresenter);
        addPresenter(phoneBatteryPresenter);
        setRetainInstance(true);

        this.viewAnimator = new ViewAnimator();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        final View animatedView = viewAnimator.onCreateView(inflater, container, R.layout.pill_ota_view, R.id.blue_box_view);

        final ViewGroup view = new OnboardingSimpleStepView(this, inflater)
                .setAnimatedView(animatedView)
                .setHeadingText(R.string.title_update_sleep_pill)
                .setSubheadingText(R.string.info_update_sleep_pill)
                .setPrimaryOnClickListener(this::onPrimaryButtonClick)
                .setSecondaryOnClickListener(this::onCancel)
                .setSecondaryButtonText(R.string.action_cancel)
                .setWantsSecondaryButton(true)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATE_PILL));

        this.primaryButton = (Button) view.findViewById(R.id.view_onboarding_simple_step_primary);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribeDevice();
        viewAnimator.onViewCreated(getActivity(), R.animator.bluetooth_sleep_pill_ota_animator);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewAnimator.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewAnimator.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        primaryButton.setOnClickListener(null);
        primaryButton = null;
        viewAnimator.onDestroyView();
        viewAnimator = null;
    }

    @Override
    void onLocationPermissionGranted(final boolean isGranted) {
        if(isGranted){
            checkPillBattery();
        }
    }

    public void onCancel(final View ignored) {
        finishWithResult(Activity.RESULT_CANCELED, null);
    }

    public void onPrimaryButtonClick(@NonNull final View ignored){
        if(isLocationPermissionGranted()){
            checkPillBattery();
        } else{
            requestLocationPermission();
        }
    }

    private void bindAndSubscribeDevice() {
        //Todo locate or use default delays
        bindAndSubscribe(
                devicesPresenter.devices.delay(1200, TimeUnit.MILLISECONDS)
                                        .map(devices -> {
                                            final SleepPillDevice sleepPillDevice = devices.getSleepPill();
                                            return sleepPillDevice != null && !sleepPillDevice.hasLowBattery();
                                        })
                , this::onPillCheckNext
                , this::presentError);

        bindAndSubscribe(
                phoneBatteryPresenter.enoughBattery.delay(1200, TimeUnit.MILLISECONDS)
                , this::onPhoneCheckNext
                , this::presentError);
    }

    private void done() {
        final int nextScreenId = getArguments().getInt(ARG_NEXT_SCREEN_ID,-1);
        if(nextScreenId != -1 && getActivity() instanceof FragmentNavigation){
            ((FragmentNavigation) getActivity()).flowFinished(this, nextScreenId, null);
        } else{
            onCancel(null);
        }
    }

    private void checkPillBattery() {
        showBlockingActivity(R.string.title_checking_pill_battery);
        updateButtonUI(false, false); //don't want to let user accidentally trigger checks again
        devicesPresenter.update();
    }

    private void checkPhoneBattery(){
        showBlockingActivity(R.string.title_checking_phone_battery);
        phoneBatteryPresenter.withAnyOperation(Arrays.asList(PillHardwareFragment.pillUpdateOperationNoCharge(),
                                                             PillHardwareFragment.pillUpdateOperationWithCharge()));
        phoneBatteryPresenter.refreshAndUpdate(getActivity());
    }

    private void updateButtonUI(final boolean shouldEnable, final boolean allowRetry){
        primaryButton.setEnabled(shouldEnable);
        primaryButton.setText( allowRetry ? R.string.action_retry : R.string.action_continue);
    }

    private void presentError(final Throwable e) {
        hideBlockingActivity(false, () -> {
            updateButtonUI(true, true);
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                    .withSupportLink()
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    private void onPillCheckNext(final boolean hasEnoughBattery){
        stateSafeExecutor.execute( () -> {
            if(hasEnoughBattery){
                checkPhoneBattery();
            } else{
                hideBlockingActivity();
                presentPillBatteryError();
            }
        });
    }

    private void onPhoneCheckNext(final boolean hasEnoughBattery) {
        stateSafeExecutor.execute( () -> {
            if(hasEnoughBattery){
                checkBluetooth();
            } else{
                updateButtonUI(true, true); //allow user to retry by plugging in phone
                hideBlockingActivity();
                presentPhoneBatteryError();
            }
        });
    }

    private void checkBluetooth(){
        showBlockingActivity(R.string.title_checking_bluetooth_connectivity);
        hideBlockingActivity();
        if (!bluetoothStack.isEnabled()) {
            ((FragmentNavigation) getActivity()).flowFinished(this, PillUpdateActivity.FLOW_BLUETOOTH_CHECK, null);
        } else{
            done();
        }
    }
}
