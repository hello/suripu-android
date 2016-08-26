package is.hello.sense.presenters;

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

public class UnpairPillPresenter extends BasePresenter<UnpairPillPresenter.Output> {
    private final static int ONE_SECOND_DELAY = 1000;
    @Inject
    DevicesInteractor devicesPresenter;

    @Override
    public void onDestroy() {
        devicesPresenter = null;
    }

    public void onViewCreated() {
        bindAndSubscribe(devicesPresenter.devices,
                         this::bindDevices,
                         view::presentError);
    }


    private void bindDevices(@NonNull final Devices devices) {
        Log.e(getClass().getSimpleName(), "bindDevices: " + (devices.toString()));
        if (view == null) {
            return;
        }
        view.postDelayed(() -> {
            final SleepPillDevice sleepPillDevice = devices.getSleepPill();
            if (sleepPillDevice == null) { // account doesn't have a pill.
                devicesPresenter.devices.forget();
                view.finishWithSuccess();
                return;
            }
            view.showBlockingActivity(R.string.unpairing_sleep_pill);
            bindAndSubscribe(devicesPresenter.unregisterDevice(sleepPillDevice),
                             this::bindUnregisterDevice,
                             this::presentError);
        }, ONE_SECOND_DELAY);

    }

    @SuppressWarnings("unused")
    public void onPrimaryClick(@NonNull final View clickedView) {
        view.showBlockingActivity(R.string.unpairing_sleep_pill);
        devicesPresenter.update();
    }

    private void finishWithSuccess() {
        view.hideBlockingActivity(R.string.unpaired, view::finishWithSuccess);
    }

    private void hideBlockingActivityWithDelay(@NonNull final Runnable runnable) {
        if (view == null) {
            return;
        }
        view.postDelayed(() -> view.hideBlockingActivity(false, runnable), ONE_SECOND_DELAY);
    }

    private void presentError(@NonNull final Throwable e) {
        hideBlockingActivityWithDelay(
                () -> view.presentError(e));
    }

    private void bindUnregisterDevice(@NonNull final VoidResponse vr) {
        finishWithSuccess();
    }

    public interface Output extends BaseOutput {
        void postDelayed(@NonNull final Runnable runnable, int time);

        void finishWithSuccess();

        void presentError(@NonNull final Throwable throwable);

    }
}
