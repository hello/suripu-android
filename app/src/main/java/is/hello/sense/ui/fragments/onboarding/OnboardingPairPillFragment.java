package is.hello.sense.ui.fragments.onboarding;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.commonsense.bluetooth.errors.SensePeripheralError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.service.SenseService;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class OnboardingPairPillFragment extends HardwareFragment {
    @Inject ApiSessionManager apiSessionManager;

    private ProgressBar activityIndicator;
    private TextView activityStatus;

    private DiagramVideoView diagram;
    private Button skipButton;
    private Button retryButton;

    private boolean isPairing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isPairOnlySession()) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_PILL_IN_APP, null);
        } else {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_PILL, null);
        }

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_activity);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);

        this.diagram = (DiagramVideoView) view.findViewById(R.id.fragment_onboarding_pair_pill_diagram);

        this.skipButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_skip);
        Views.setSafeOnClickListener(skipButton, ignored -> skipPairingPill());

        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_retry);
        Views.setSafeOnClickListener(retryButton, ignored -> pairPill());


        OnboardingToolbar.of(this, view)
                         .setWantsBackButton(isPairOnlySession())
                         .setOnHelpClickListener(this::help);

        if (BuildConfig.DEBUG) {
            diagram.setOnLongClickListener(ignored -> {
                skipPairingPill();
                return true;
            });
            diagram.setBackgroundResource(R.drawable.selectable_dark);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isPairing) {
            pairPill();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        diagram.destroy();

        this.activityIndicator = null;
        this.activityStatus = null;

        this.diagram = null;

        this.skipButton = null;
        this.retryButton = null;
    }

    private void beginPairing() {
        this.isPairing = true;

        activityIndicator.setVisibility(View.VISIBLE);
        activityStatus.setVisibility(View.VISIBLE);

        skipButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    private void finishedPairing(boolean success) {
        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        getFragmentManager().executePendingTransactions();
        LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
            stateSafeExecutor.execute(() -> {
                if (isPairOnlySession()) {
                    Analytics.trackEvent(Analytics.Onboarding.EVENT_PILL_PAIRED_IN_APP, null);
                    getOnboardingActivity().finish();
                } else {
                    serviceConnection.perform(SenseService::disconnect)
                                     .subscribe(Functions.NO_OP, Functions.LOG_ERROR);

                    if (success) {
                        Analytics.trackEvent(Analytics.Onboarding.EVENT_PILL_PAIRED, null);
                        getOnboardingActivity().showPillInstructions();
                    } else {
                        getOnboardingActivity().showSenseColorsInfo();
                    }
                }
            });
        });
    }


    public void skipPairingPill() {
        final Properties properties =
                Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "pill_pairing");
        if (isPairOnlySession()){
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP_IN_APP, properties);
        }else {
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

    public void help(@NonNull View sender) {
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.PILL_PAIRING);
    }

    public void pairPill() {
        beginPairing();

        if (sensePresenter.shouldScan()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(sensePresenter.peripheral.take(1),
                             ignored -> pairPill(),
                             this::presentError);
            sensePresenter.scanForLastConnectedSense();

            return;
        }

        if (!serviceConnection.isConnectedToSense()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(sensePresenter.connectToPeripheral(), status -> {
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
                final String accessToken = apiSessionManager.getAccessToken();
                final Observable<SenseService> linkPill = serviceConnection.perform(s -> s.linkPill(accessToken));
                bindAndSubscribe(linkPill,
                                 ignored -> completeHardwareActivity(() -> finishedPairing(true)),
                                 this::presentError);
            });
        }, this::presentError);
    }

    public void presentError(Throwable e) {
        this.isPairing = false;

        diagram.suspendPlayback(true);
        hideAllActivityForFailure(() -> {
            activityIndicator.setVisibility(View.GONE);
            activityStatus.setVisibility(View.GONE);

            if (!isPairOnlySession()) {
                skipButton.setVisibility(View.VISIBLE);
            }
            retryButton.setVisibility(View.VISIBLE);

            final ErrorDialogFragment.Builder errorDialogBuilder =
                    new ErrorDialogFragment.Builder(e, getResources());
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
                errorDialogBuilder.withUnstableBluetoothHelp(getResources());
            }

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }
}
