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

import is.hello.buruberi.bluetooth.devices.SensePeripheral;
import is.hello.buruberi.bluetooth.devices.SensePeripheralError;
import is.hello.buruberi.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.buruberi.bluetooth.errors.OperationTimeoutError;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingPairPillFragment extends HardwareFragment {
    private ProgressBar activityIndicator;
    private TextView activityStatus;

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
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_activity);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);

        this.skipButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_skip);
        Views.setSafeOnClickListener(skipButton, ignored -> skipPairingPill());

        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_retry);
        Views.setSafeOnClickListener(retryButton, ignored -> pairPill());

        OnboardingToolbar.of(this, view)
                .setWantsBackButton(false)
                .setOnHelpClickListener(this::help);

        if (BuildConfig.DEBUG) {
            View diagram = view.findViewById(R.id.fragment_onboarding_pair_pill_diagram);
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
                    getOnboardingActivity().finish();
                } else {
                    if (success) {
                        getOnboardingActivity().showPillInstructions();
                    } else {
                        getOnboardingActivity().showSenseColorsInfo();
                    }
                }
            });
        });
    }


    public void skipPairingPill() {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "pill_pairing"));

        SenseAlertDialog confirmation = new SenseAlertDialog(getActivity());
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

        if (!hardwarePresenter.hasPeripheral()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(), ignored -> pairPill(), this::presentError);
            return;
        }

        if (!hardwarePresenter.isConnected()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                if (status == SensePeripheral.ConnectStatus.CONNECTED) {
                    pairPill();
                } else {
                    showBlockingActivity(Styles.getWiFiConnectStatusMessage(status));
                }
            }, this::presentError);
            return;
        }

        showBlockingActivity(R.string.title_waiting_for_sense);
        showHardwareActivity(() -> {
            hideBlockingActivity(false, () -> {
                bindAndSubscribe(hardwarePresenter.linkPill(),
                                 ignored -> completeHardwareActivity(() -> finishedPairing(true)),
                                 this::presentError);
            });
        }, this::presentError);
    }

    public void presentError(Throwable e) {
        this.isPairing = false;

        hideAllActivityForFailure(() -> {
            activityIndicator.setVisibility(View.GONE);
            activityStatus.setVisibility(View.GONE);

            skipButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.VISIBLE);

            ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e);
            errorDialogBuilder.withOperation("Pair Pill");
            if (e instanceof OperationTimeoutError || SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.TIME_OUT)) {
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

            ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }
}
