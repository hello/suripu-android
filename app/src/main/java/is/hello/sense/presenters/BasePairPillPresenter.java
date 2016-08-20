package is.hello.sense.presenters;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.presenters.outputs.BaseHardwareOutput;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;

public abstract class BasePairPillPresenter extends BaseHardwarePresenter<BasePairPillPresenter.Output> {
    protected LoadingDialogFragment loadingDialogFragment;


    public abstract void trackOnCreate();

    public abstract void trackOnSkip();

    @StringRes
    public abstract int getTitleRes();

    @StringRes
    public abstract int getSubTitleRes();


    public abstract boolean showSkipButtonOnError();

    public abstract boolean wantsBackButton();

    public abstract void finishedPairingAction(@NonNull final Activity activity, final boolean success);


    public void skipPairingPill(@NonNull final Activity activity) {
        trackOnSkip();
        final SenseAlertDialog confirmation = new SenseAlertDialog(activity);
        confirmation.setTitle(R.string.alert_title_skip_pair_pill);
        confirmation.setMessage(R.string.alert_message_skip_pair_pill);
        confirmation.setPositiveButton(R.string.action_skip, (dialog, which) -> {
            completeHardwareActivity(() -> viewOutput.finishedPairing(false));
        });
        confirmation.setNegativeButton(android.R.string.cancel, null);
        confirmation.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        confirmation.show();
    }

    @Override
    public boolean isResumed() {
        return viewOutput != null && viewOutput.isResumed();
    }

    public void pairPill() {
        viewOutput.showPillPairing();
        if (!hardwareInteractor.hasPeripheral()) {
            viewOutput.showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwareInteractor.rediscoverLastPeripheral(), ignored -> pairPill(), viewOutput::presentError);
            return;
        }

        if (!hardwareInteractor.isConnected()) {
            viewOutput.showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwareInteractor.connectToPeripheral(), status -> {
                if (status == ConnectProgress.CONNECTED) {
                    pairPill();
                } else {
                    viewOutput.showBlockingActivity(Styles.getConnectStatusMessage(status));
                }
            }, viewOutput::presentError);
            return;
        }

        viewOutput.showBlockingActivity(R.string.title_waiting_for_sense);
        showHardwareActivity(() -> {
            viewOutput.animateDiagram(true);
            viewOutput.hideBlockingActivity(false, () -> {
                bindAndSubscribe(hardwareInteractor.linkPill(),
                                 ignored -> completeHardwareActivity(() -> viewOutput.finishedPairing(true)),
                                 viewOutput::presentError);
            });
        }, viewOutput::presentError);

    }

    public interface Output extends BaseHardwareOutput {

        void showPillPairing();

        void presentError(final Throwable e);

        void finishedPairing(final boolean success);

        void animateDiagram(final boolean animate);
    }


}
