package is.hello.sense.presenters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class UnpairPillPresenter extends BasePresenter<UnpairPillPresenter.Output> {
    private final static int ONE_SECOND_DELAY = 1000;
    /**
     * When we bindAndSubscribe to devicesInteractor.devices during onViewCreated the value from
     * SenseUpgradeActivity will immediately be used and force the user into the next fragment.
     * Use this boolean to determine if the user is ready.
     */
    private boolean handlePrimaryClick = false;

    @Inject
    DevicesInteractor devicesInteractor;

    @Override
    public void onDetach() {
        devicesInteractor = null;
    }

    @Override
    public void onViewCreated() {
        bindAndSubscribe(devicesInteractor.devices,
                         this::bindDevices,
                         this::presentError);
    }


    private void bindDevices(@NonNull final Devices devices) {
        Log.e(getClass().getSimpleName(), "bindDevices: " + (devices.toString()));
        if (!handlePrimaryClick) {
            return;
        }
        handlePrimaryClick = false;
        if (view == null) {
            return;
        }
        view.postDelayed(() -> {
            final SleepPillDevice sleepPillDevice = devices.getSleepPill();
            if (sleepPillDevice == null) { // account doesn't have a pill.
                finishWithSuccess(false);
                return;
            }
            view.showBlockingActivity(R.string.unpairing_sleep_pill);
            bindAndSubscribe(devicesInteractor.unregisterDevice(sleepPillDevice),
                             this::bindUnregisterDevice,
                             this::presentError);
        }, ONE_SECOND_DELAY);

    }

    @SuppressWarnings("unused")
    public void onPrimaryClick(@NonNull final View clickedView) {
        handlePrimaryClick = true;
        if (devicesInteractor.devices.hasValue()) {
            if (devicesInteractor.devices.getValue().getSleepPill() == null) {
                // no sleep pill with account.
                finishWithSuccess(false);
            } else {
                view.showBlockingActivity(R.string.unpairing_sleep_pill);
                bindDevices(devicesInteractor.devices.getValue());
            }
        } else {
            view.showBlockingActivity(R.string.unpairing_sleep_pill);
            devicesInteractor.update();
        }
    }

    public void onSecondaryClick(@NonNull final View clickedView) {
        final SenseAlertDialog dialog = new SenseAlertDialog(view.getActivity());
        dialog.setTitle(R.string.unpair_pill_dialog_title);
        dialog.setMessage(R.string.unpair_pill_dialog_message);
        dialog.setPositiveButton(R.string.action_pair_new_pill, (dialogInterface, i) -> {
            onPrimaryClick(clickedView);
        });
        dialog.setNegativeButton(R.string.action_dont_pair, (dialogInterface, i) -> {
            execute(view::cancelFlow);
        });
        dialog.show();
    }

    @SuppressWarnings("unused")
    public void onHelpClick(@NonNull final View sender) {
        Analytics.trackEvent(Analytics.General.EVENT_HELP, null);
        UserSupport.showForHelpStep(view.getActivity(), UserSupport.HelpStep.PAIRING_MODE);
    }

    private void finishWithSuccess(final boolean unpairedOldPill) {
        if (unpairedOldPill) {
            hideBlockingActivity(R.string.unpaired, view::finishFlow);
        } else {
            view.finishFlow();
        }
    }

    private void hideBlockingActivityWithDelay(@NonNull final Runnable runnable) {
        if (view == null) {
            return;
        }
        view.postDelayed(() -> hideBlockingActivity(false, runnable), ONE_SECOND_DELAY);
    }

    private void presentError(@NonNull final Throwable e) {
        hideBlockingActivityWithDelay(
                () -> view.showErrorDialog(ErrorDialogFragment.newInstance(e)));
    }

    private void bindUnregisterDevice(@NonNull final VoidResponse vr) {
        finishWithSuccess(true);
    }

    public interface Output extends BaseOutput {
        void postDelayed(@NonNull final Runnable runnable, int time);

        Activity getActivity();
    }
}
