package is.hello.sense.presenters.pairpill;

import android.content.DialogInterface;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.commonsense.bluetooth.errors.SensePeripheralError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.interactors.hardware.HardwareInteractor;
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

    public abstract void trackOnSkip();

    @StringRes
    public abstract int getTitleRes();

    @StringRes
    public abstract int getSubTitleRes();


    public abstract boolean showSkipButtonOnError();

    public abstract boolean wantsBackButton();

    public abstract void onHelpClick(@NonNull final View viewClicked);

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        if (!isPairing) {
            pairPill();
        }
    }

    public void skipPairingPill() {
        trackOnSkip();

        final SenseAlertDialog.Builder builder = new SenseAlertDialog.Builder();
        builder.setTitle(R.string.alert_title_skip_pair_pill)
               .setMessage(R.string.alert_message_skip_pair_pill)
               .setPositiveButton(R.string.action_skip, () -> showFinishedLoading(false))
               .setNegativeButton(android.R.string.cancel, null)
               .setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        view.showAlertDialog(builder);
    }

    public void pairPill() {
        this.isPairing = true;
        view.showPillPairingState();
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
                                 ignored -> showFinishedLoading(true),
                                 this::presentError);
            });
        }, this::presentError);

    }

    private void showFinishedLoading(final boolean success) {
        //todo analytics
        completeHardwareActivity(
                () -> view.showFinishedLoadingFragment(success ? R.string.sleep_pill_paired : R.string.action_done,
                                                       () -> execute(view::finishFlow)));

    }

    private void presentError(@NonNull final Throwable e) {
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
            view.showErrorState(showSkipButtonOnError());
            view.showErrorDialog(errorDialogBuilder);
        });
    }

    public interface Output extends BaseOutput {

        void showPillPairingState();

        void showErrorState(boolean withSkipButton);

        void animateDiagram(boolean animate);

        void showFinishedLoadingFragment(@StringRes final int messageRes, @NonNull final Runnable runnable);


    }


}
