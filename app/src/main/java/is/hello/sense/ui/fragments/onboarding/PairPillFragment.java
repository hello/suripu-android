package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class PairPillFragment extends HardwareFragment {
    private OnboardingSimpleStepView view = null;

    @Inject
    DevicesPresenter devicesPresenter;


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        showBlockingActivity(R.string.label_searching_for_pill);
        view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.pair_pill_title)
                .setSubheadingText(R.string.pair_pill_message)
                .setPrimaryButtonText(R.string.action_pair_new_sleep_pill)
                .setSecondaryButtonText(R.string.action_do_later)
                .setToolbarWantsHelpButton(true)
                .setPrimaryOnClickListener(this::onPrimaryPress)
                .setSecondaryOnClickListener(this::onSecondaryPress)
                .setToolbarOnHelpClickListener(this::showPairingModeHelp)
                .setDiagramImage(R.drawable.onboarding_sleep_pill);
        view.setVisibility(View.INVISIBLE); // User will see a flicker before the blocking activity is shown.
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(devicesPresenter.devices,
                         this::bindDevices,
                         this::presentBindDevicesError);
        devicesPresenter.update();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (view != null) {
            view.destroy();
            view = null;
        }
    }

    private void onPrimaryPress(@NonNull final View view) {
        if (devicesPresenter.devices.getValue() == null) { // Shouldn't occur, but doesn't hurt.
            showBlockingActivity(R.string.label_searching_for_pill);
            devicesPresenter.update();
            return;
        }
        final SleepPillDevice sleepPillDevice = devicesPresenter.devices.getValue().getSleepPill();
        if (sleepPillDevice == null) { // account doesn't have a pill.
            finishWithSuccess();
            return;
        }
        showBlockingActivity(R.string.unpairing_sleep_pill);
        bindAndSubscribe(devicesPresenter.unregisterDevice(sleepPillDevice),
                         this::bindUnregisterDevice,
                         this::presentUnregisterDeviceError);
    }

    private void onSecondaryPress(@NonNull final View view) {
        final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setTitle(R.string.pair_pill_dialog_title);
        dialog.setMessage(R.string.pair_pill_dialog_message);
        dialog.setPositiveButton(R.string.action_pair_new_pill, (dialogInterface, i) -> {
            onPrimaryPress(view);
        });
        dialog.setNegativeButton(R.string.action_dont_pair, (dialogInterface, i) -> {
            finishFlow();
        });
        dialog.show();
    }

    private void bindDevices(@NonNull final Devices devices) {
        view.setVisibility(View.VISIBLE);
        hideBlockingActivityWithDelay(() -> {
        });
    }

    //We have no idea what devices exist on this account. Go back.
    private void presentBindDevicesError(@NonNull final Throwable e) {
        hideBlockingActivityWithDelay(
                () -> {
                    finishFlowWithResult(Activity.RESULT_CANCELED);
                    ErrorDialogFragment.presentError(getActivity(), e);
                });
    }

    private void bindUnregisterDevice(@NonNull final VoidResponse vr) {
        finishWithSuccess();
    }

    // Failed to unregister the users pill. Remain on Fragment and let them try again.
    private void presentUnregisterDeviceError(@NonNull final Throwable e) {
        hideBlockingActivityWithDelay(
                () -> ErrorDialogFragment.presentError(getActivity(), e));
    }

    private void finishWithSuccess() {
        hideBlockingActivity(R.string.unpaired, () -> finishFlowWithResult(Activity.RESULT_OK));
    }

    private void showPairingModeHelp(@NonNull final View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIRING_MODE_HELP, null);
        UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.PAIRING_MODE);
    }

    private void hideBlockingActivityWithDelay(@NonNull final Runnable runnable) {
        if (view == null) {
            return;
        }
        view.postDelayed(() -> hideBlockingActivity(false, runnable), 1000);
    }

}
