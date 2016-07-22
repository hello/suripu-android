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
import is.hello.sense.graph.presenters.PhoneBatteryPresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;
import is.hello.sense.util.Analytics;

public class UpdateIntroPillFragment extends PillHardwareFragment {
    @Inject
    BluetoothStack bluetoothStack;
    @Inject
    PhoneBatteryPresenter phoneBatteryPresenter;

    private Button primaryButton;
    private ViewAnimator viewAnimator;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Analytics.trackEvent(Analytics.PillUpdate.EVENT_START, null);
        addPresenter(phoneBatteryPresenter);
        setRetainInstance(true);

        this.viewAnimator = new ViewAnimator();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View animatedView = viewAnimator.inflateView(inflater, container, R.layout.pill_ota_view, R.id.blue_box_view);
        final ViewGroup view = new OnboardingSimpleStepView(this, inflater)
                .setAnimatedView(animatedView)
                .setHeadingText(R.string.title_update_sleep_pill)
                .setSubheadingText(R.string.info_update_sleep_pill)
                .setPrimaryOnClickListener(this::onPrimaryButtonClick)
                .setSecondaryOnClickListener(view1 -> this.cancel(false))
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

    public void onPrimaryButtonClick(@NonNull final View ignored) {
        checkPhoneBattery();
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
        if (isGranted) {
            checkPhoneBattery();
        }
    }

    private void bindAndSubscribeDevice() {
        bindAndSubscribe(
                phoneBatteryPresenter.enoughBattery.delay(LoadingDialogFragment.DURATION_DEFAULT, TimeUnit.MILLISECONDS),
                this::onPhoneCheckNext,
                this::presentError);
    }

    private void checkPhoneBattery() {
        phoneBatteryPresenter.withAnyOperation(Arrays.asList(PillHardwareFragment.pillUpdateOperationNoCharge(),
                                                             PillHardwareFragment.pillUpdateOperationWithCharge()));
        phoneBatteryPresenter.refreshAndUpdate(getActivity());
    }

    private void done() {
        getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null);
    }

    private void updateButtonUI(final boolean shouldEnable, final boolean allowRetry) {
        primaryButton.setEnabled(shouldEnable);
        primaryButton.setText(allowRetry ? R.string.action_retry : R.string.action_continue);
    }

    private void presentError(final Throwable e) {
        updateButtonUI(true, true);
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                .withSupportLink()
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);

    }

    private void onPhoneCheckNext(final boolean hasEnoughBattery) {
        stateSafeExecutor.execute(() -> {
            if (hasEnoughBattery) {
                checkBluetooth();
            } else {
                updateButtonUI(true, true); //allow user to retry by plugging in phone
                presentPhoneBatteryError();
            }
        });
    }

    private void checkBluetooth() {
        if (bluetoothStack.isEnabled()) {
            done();
        } else {
            cancel(true);
        }
    }
}
