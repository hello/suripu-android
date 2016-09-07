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
        if (view == null) {
            return;
        }
        view.postDelayed(() -> {
            final SleepPillDevice sleepPillDevice = devices.getSleepPill();
            if (sleepPillDevice == null) { // account doesn't have a pill.
                devicesInteractor.devices.forget();
                finishWithSuccess();
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
        view.showBlockingActivity(R.string.unpairing_sleep_pill);
        devicesInteractor.update();
    }

    public void onSecondaryClick(@NonNull final View clickedView) {
        final SenseAlertDialog dialog = new SenseAlertDialog(view.getActivity());
        dialog.setTitle(R.string.unpair_pill_dialog_title);
        dialog.setMessage(R.string.unpair_pill_dialog_message);
        dialog.setPositiveButton(R.string.action_pair_new_pill, (dialogInterface, i) -> {
            onPrimaryClick(clickedView);
        });
        dialog.setNegativeButton(R.string.action_dont_pair, (dialogInterface, i) -> {
            execute(() -> view.cancelFlow());
        });
        dialog.show();
    }

    @SuppressWarnings("unused")
    public void onHelpClick(@NonNull final View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIRING_MODE_HELP, null);
        UserSupport.showForHelpStep(view.getActivity(), UserSupport.HelpStep.PAIRING_MODE);
    }

    private void finishWithSuccess() {
        hideBlockingActivity(R.string.unpaired, view::finishFlow);
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
        finishWithSuccess();
    }

    public interface Output extends BaseOutput {
        void postDelayed(@NonNull final Runnable runnable, int time);

        Activity getActivity();
    }
}
