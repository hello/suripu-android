package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.PromptForHighPowerDialogFragment;
import is.hello.sense.ui.dialogs.TroubleshootSenseDialogFragment;
import is.hello.sense.ui.fragments.sense.BasePairSenseFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

public class PairSenseFragment extends BasePairSenseFragment
        implements FragmentCompat.OnRequestPermissionsResultCallback {
    public static final int REQUEST_CODE_EDIT_WIFI = 0xf1;
    private static final int REQUEST_CODE_HIGH_POWER_RETRY = 0x88;
    private static final int REQUEST_CODE_SHOW_RATIONALE_DIALOG = 0xb2;

    private static final int RESULT_EDIT_WIFI = 0x99;

    private static final int LINK_ACCOUNT_FAILURES_BEFORE_EDIT_WIFI = 3;

    private int linkAccountFailures = 0;

    private final LocationPermission locationPermission = new LocationPermission(this);
    private OnboardingSimpleStepView view;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sendOnCreateAnalytics(isPairOnlySession());

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {

        //todo refactor so there are no conditional statements here
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(isPairUpgradedSenseSession() ?  R.string.title_pair_new_sense : R.string.title_pair_sense)
                .setSubheadingText(isPairUpgradedSenseSession() ? R.string.info_pair_new_sense : R.string.info_pair_sense)
                .setDiagramImage(R.drawable.onboarding_pair_sense)
                .setSecondaryButtonText(R.string.action_sense_pairing_mode_help)
                .setSecondaryOnClickListener(this::showPairingModeHelp)
                .setPrimaryOnClickListener(ignored -> next())
                .setToolbarWantsBackButton(true)
                .setToolbarOnHelpClickListener(ignored -> {
                    UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.PAIRING_SENSE_BLE);
                })
                .setToolbarOnHelpLongClickListener(ignored -> {
                    showSupportOptions();
                    return true;
                })
                .configure(b -> subscribe(hardwarePresenter.bluetoothEnabled, b.primaryButton::setEnabled, Functions.LOG_ERROR));

        return view;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_HIGH_POWER_RETRY && resultCode == Activity.RESULT_OK) {
            hardwarePresenter.setWantsHighPowerPreScan(true);
            next();
        } else if (requestCode == REQUEST_CODE_EDIT_WIFI && resultCode == RESULT_EDIT_WIFI) {
            showSelectWifiNetwork();
        } else if (requestCode == REQUEST_CODE_SHOW_RATIONALE_DIALOG && resultCode == Activity.RESULT_OK) {
            locationPermission.requestPermissionWithDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (locationPermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            next();
        } else {
            locationPermission.showEnableInstructionsDialog();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.view.destroy();
        this.view = null;
    }

    private void checkConnectivityAndContinue() {
        showBlockingActivity(R.string.title_checking_connectivity);
        showHardwareActivity(() -> {
            bindAndSubscribe(hardwarePresenter.currentWifiNetwork(), network -> {
                if (network.connectionState == SenseCommandProtos.wifi_connection_state.IP_RETRIEVED) {
                    super.linkAccount();
                } else {
                    continueToWifi();
                }
            }, e -> {
                Logger.error(PairSenseFragment.class.getSimpleName(), "Could not get Sense's wifi network", e);
                continueToWifi();
            });
        }, e -> presentError(e, "Turning on LEDs"));
    }

    private void continueToWifi() {
        hideAllActivityForSuccess(getOnFinishedSuccessMessage(),
                                  this::showSelectWifiNetwork,
                                  e -> presentError(e, "Turning off LEDs"));
    }

    private void showSelectWifiNetwork() {
        getFragmentNavigation().flowFinished(this, REQUEST_CODE_EDIT_WIFI, null);
    }

    @Override
    protected void onFinished() {
        hideAllActivityForSuccess( getOnFinishedSuccessMessage(),
                                   () -> {
                                       sendOnFinishedAnalytics(isPairOnlySession());
                                       if (isPairOnlySession()) {
                                          if (shouldReleasePeripheralOnPair()) {
                                              hardwarePresenter.clearPeripheral();
                                          }
                                           getActivity().finish();
                                       } else {
                                           finishFlow();
                                       }
                                  },
                                  e -> {
                                      Log.e("Error", "E: " + e.getLocalizedMessage());
                                      presentError(e, "Turning off LEDs");
                                  });
    }

    @Override
    public void presentError(final Throwable e, @NonNull final String operation) {
        hideAllActivityForFailure(() -> {
            if (OPERATION_LINK_ACCOUNT.equals(operation)) {
                this.linkAccountFailures++;
                if (linkAccountFailures >= LINK_ACCOUNT_FAILURES_BEFORE_EDIT_WIFI) {
                    final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder()
                            .withMessage(StringRef.from(R.string.error_link_account_failed_multiple_times))
                            .withAction(RESULT_EDIT_WIFI, R.string.action_select_wifi_network)
                            .withOperation(operation)
                            .withSupportLink()
                            .build();

                    dialogFragment.setTargetFragment(this, REQUEST_CODE_EDIT_WIFI);
                    dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);

                    Analytics.trackError(e, operation);
                    return;
                }
            }

            if (e instanceof SenseNotFoundError) {
                hardwarePresenter.trackPeripheralNotFound();

                if (hardwarePresenter.shouldPromptForHighPowerScan()) {
                    final PromptForHighPowerDialogFragment dialogFragment = new PromptForHighPowerDialogFragment();
                    dialogFragment.setTargetFragment(this, REQUEST_CODE_HIGH_POWER_RETRY);
                    dialogFragment.show(getFragmentManager(), PromptForHighPowerDialogFragment.TAG);
                } else {
                    final TroubleshootSenseDialogFragment dialogFragment = new TroubleshootSenseDialogFragment();
                    dialogFragment.show(getFragmentManager(), TroubleshootSenseDialogFragment.TAG);
                }

                Analytics.trackError(e, operation);
            } else {
                final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                        .withUnstableBluetoothHelp(getActivity())
                        .withOperation(operation)
                        .build();
                dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            }
        });
    }

    public void showPairingModeHelp(@NonNull final View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIRING_MODE_HELP, null);
        UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.PAIRING_MODE);
    }

    public void next() {
        if (!locationPermission.isGranted()) {
            locationPermission.requestPermissionWithDialog();
            return;
        }

        showBlockingActivity(R.string.title_scanning_for_sense);
        final Observable<SensePeripheral> device = hardwarePresenter.closestPeripheral();
        bindAndSubscribe(device, this::tryToPairWith, e -> {
            hardwarePresenter.clearPeripheral();
            presentError(e, "Discovering Sense");
        });
    }

    public void tryToPairWith(@NonNull final SensePeripheral device) {
        if (BuildConfig.DEBUG || isPairUpgradedSenseSession()) {
            final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
            dialog.setTitle(R.string.debug_title_confirm_sense_pair);
            dialog.setMessage(getString(R.string.debug_message_confirm_sense_pair_fmt, device.getName()));
            dialog.setPositiveButton(android.R.string.ok, (sender, which) -> completePeripheralPair());
            dialog.setNegativeButton(android.R.string.cancel, (sender, which) -> hideBlockingActivity(false, hardwarePresenter::clearPeripheral));
            dialog.setCancelable(false);
            dialog.show();
        } else {
            completePeripheralPair();
        }
    }

    public void completePeripheralPair() {
        Analytics.setSenseId(hardwarePresenter.getDeviceId());

        if (hardwarePresenter.getBondStatus() == GattPeripheral.BOND_BONDED) {
            showBlockingActivity(R.string.title_clearing_bond);
            bindAndSubscribe(hardwarePresenter.clearBond(),
                             ignored -> {
                                 completePeripheralPair();
                             },
                             e -> presentError(e, "Clearing Bond"));
        } else {
            showBlockingActivity(getPairingMessage());
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                if (status == ConnectProgress.CONNECTED) {
                    checkConnectivityAndContinue();
                } else {
                    showBlockingActivity(Styles.getConnectStatusMessage(status));
                }
            }, e -> presentError(e, "Connecting to Sense"));
        }
    }

    public @StringRes int getPairingMessage(){
        return isPairUpgradedSenseSession() ? R.string.title_pairing_with_sense : R.string.title_connecting;
    }

}
