package is.hello.sense.presenters.pairpill;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.commonsense.bluetooth.errors.SensePeripheralError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.presenters.BaseHardwarePresenter;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;

public abstract class BasePairPillPresenter extends BaseHardwarePresenter<BasePairPillPresenter.Output> {
    protected boolean isPairing = false;

    public BasePairPillPresenter(final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
    }

    public abstract void trackOnCreate();

    public abstract void trackOnSkip();

    @StringRes
    public abstract int getTitleRes();

    @StringRes
    public abstract int getSubTitleRes();


    public abstract boolean showSkipButtonOnError();

    public abstract boolean wantsBackButton();

    public abstract void finishedPairingAction(@NonNull final Activity activity, final boolean success);

    public abstract void onHelpClick(@NonNull final View viewClicked);

    @Override
    public void onResume() {
        super.onResume();
        if (!isPairing) {
            pairPill();
        }
    }

    public void skipPairingPill(@NonNull final Activity activity) {
        trackOnSkip();
        final SenseAlertDialog confirmation = new SenseAlertDialog(activity);
        confirmation.setTitle(R.string.alert_title_skip_pair_pill);
        confirmation.setMessage(R.string.alert_message_skip_pair_pill);
        confirmation.setPositiveButton(R.string.action_skip, (dialog, which) -> {
            completeHardwareActivity(() -> view.finishedPairing(false));
        });
        confirmation.setNegativeButton(android.R.string.cancel, null);
        confirmation.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        confirmation.show();
    }

    @Override
    public boolean isResumed() {
        return view != null && view.isResumed();
    }

    public void pairPill() {
        this.isPairing = true;
        view.showPillPairing();
        if (!hardwareInteractor.hasPeripheral()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwareInteractor.rediscoverLastPeripheral(), ignored -> pairPill(), this::presentError);
            return;
        }

        if (!hardwareInteractor.isConnected()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwareInteractor.connectToPeripheral(), status -> {
                if (status == ConnectProgress.CONNECTED) {
                    pairPill();
                } else {
                    view.showBlockingActivity(Styles.getConnectStatusMessage(status));
                }
            }, this::presentError);
            return;
        }

        showBlockingActivity(R.string.title_waiting_for_sense);
        showHardwareActivity(() -> {
            view.animateDiagram(true);
            hideBlockingActivity(false, () -> {
                bindAndSubscribe(hardwareInteractor.linkPill(),
                                 ignored -> completeHardwareActivity(() -> view.finishedPairing(true)),
                                 this::presentError);
            });
        }, this::presentError);

    }

    public void presentError(@NonNull final Throwable e) {
        hideAllActivityForFailure(() -> {
            this.isPairing = false;
            final ErrorDialogFragment.PresenterBuilder errorDialogBuilder = ErrorDialogFragment.newInstance(e);
            errorDialogBuilder.withOperation("Pair Pill");
            if (e instanceof OperationTimeoutException ||
                    SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.TIME_OUT)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_message_sleep_pill_scan_timeout));
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.NETWORK_ERROR)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_network_failure_pair_pill));
                errorDialogBuilder.withSupportLink();
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_pill_already_paired));
                errorDialogBuilder.withSupportLink();
            } else {
                errorDialogBuilder.withUnstableBluetoothHelp();
            }
            view.showError();
            view.showErrorDialog(errorDialogBuilder);
        });
    }

    public interface Output extends BaseOutput {

        void showPillPairing();

        void showError();

        void finishedPairing(final boolean success);

        void animateDiagram(final boolean animate);

        void finishFlow();
    }


}
