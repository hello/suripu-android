package is.hello.sense.ui.fragments.onboarding;

import android.content.DialogInterface;
import android.os.Bundle;

import com.segment.analytics.Properties;

import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.pill.BasePairPillFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public class OnboardingPairPillFragment extends BasePairPillFragment {

    // region lifecycle
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getOnboardingActivity() == null) {
            // todo Complain. Crash. Error
        }
        if (isPairOnlySession()) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_PILL_IN_APP, null);
        } else {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_PILL, null);
        }

        setRetainInstance(true);
    }
    // endregion

    // region BasePairPillFragment
    @Override
    protected boolean wantsBackButton() {
        return isPairOnlySession();
    }

    @Override
    protected void finishedPairing(final boolean success) {
        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        getFragmentManager().executePendingTransactions();
        LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> stateSafeExecutor.execute(() -> {
            if (isPairOnlySession()) {
                Analytics.trackEvent(Analytics.Onboarding.EVENT_PILL_PAIRED_IN_APP, null);
                getOnboardingActivity().finish();
            } else {
                hardwarePresenter.clearPeripheral();
                if (success) {
                    Analytics.trackEvent(Analytics.Onboarding.EVENT_PILL_PAIRED, null);
                    getOnboardingActivity().showPillInstructions();
                } else {
                    getOnboardingActivity().showSenseColorsInfo();
                }
            }
        }));
    }

    @Override
    protected int getTitleRes() {
        return R.string.title_pair_pill;
    }

    @Override
    protected int getSubTitleRes() {
        return R.string.info_pair_pill;
    }

    // endregion

    // region methods
    public void skipPairingPill() {
        final Properties properties =
                Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "pill_pairing");
        if (isPairOnlySession()) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP_IN_APP, properties);
        } else {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, properties);
        }

        final SenseAlertDialog confirmation = new SenseAlertDialog(getActivity());
        confirmation.setTitle(R.string.alert_title_skip_pair_pill);
        confirmation.setMessage(R.string.alert_message_skip_pair_pill);
        confirmation.setPositiveButton(R.string.action_skip, (dialog, which) -> {
            completeHardwareActivity(() -> finishedPairing(false));
        });
        confirmation.setNegativeButton(android.R.string.cancel, null);
        confirmation.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        confirmation.show();
    }

    public void pairPill() {
        beginPairing();

        if (!hardwarePresenter.hasPeripheral()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(), ignored -> pairPill(), this::presentError);
            return;
        }

        if (!hardwarePresenter.isConnected()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                if (status == ConnectProgress.CONNECTED) {
                    pairPill();
                } else {
                    showBlockingActivity(Styles.getConnectStatusMessage(status));
                }
            }, this::presentError);
            return;
        }

        showBlockingActivity(R.string.title_waiting_for_sense);
        showHardwareActivity(() -> {
            diagram.startPlayback();
            hideBlockingActivity(false, () -> {
                bindAndSubscribe(hardwarePresenter.linkPill(),
                                 ignored -> completeHardwareActivity(() -> finishedPairing(true)),
                                 this::presentError);
            });
        }, this::presentError);
    }


    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    //end region
}
