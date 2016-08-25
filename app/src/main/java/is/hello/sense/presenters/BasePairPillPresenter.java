package is.hello.sense.presenters;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;

public abstract class BasePairPillPresenter extends BaseHardwarePresenter<BasePairPillPresenter.Output> {

    public BasePairPillPresenter(final HardwareInteractor hardwareInteractor,
                                 final UserFeaturesInteractor userFeaturesInteractor) {
        super(hardwareInteractor,
              userFeaturesInteractor);
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
        view.showPillPairing();
        if (!hardwareInteractor.hasPeripheral()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwareInteractor.rediscoverLastPeripheral(), ignored -> pairPill(), view::presentError);
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
            }, view::presentError);
            return;
        }

        showBlockingActivity(R.string.title_waiting_for_sense);
        showHardwareActivity(() -> {
            view.animateDiagram(true);
            hideBlockingActivity(false, () -> {
                bindAndSubscribe(hardwareInteractor.linkPill(),
                                 ignored -> completeHardwareActivity(() -> view.finishedPairing(true)),
                                 view::presentError);
            });
        }, view::presentError);

    }

    public interface Output extends BaseOutput {

        void showPillPairing();

        void presentError(final Throwable e);

        void finishedPairing(final boolean success);

        void animateDiagram(final boolean animate);
    }


}
