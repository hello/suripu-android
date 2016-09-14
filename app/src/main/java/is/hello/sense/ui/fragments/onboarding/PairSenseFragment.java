package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.functional.Functions;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.PromptForHighPowerDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

public class PairSenseFragment extends HardwareFragment
        implements FragmentCompat.OnRequestPermissionsResultCallback {
    public static final int REQUEST_CODE_EDIT_WIFI = 0xf1;
    private static final int REQUEST_CODE_HIGH_POWER_RETRY = 0x88;
    private static final int REQUEST_CODE_SHOW_RATIONALE_DIALOG = 0xb2;

    private static final int RESULT_EDIT_WIFI = 0x99;

    private static final int LINK_ACCOUNT_FAILURES_BEFORE_EDIT_WIFI = 3;
    private static final String OPERATION_LINK_ACCOUNT = "Linking account";
    private static final String ARG_PAIR_NEW_SENSE = "pair_new_sense" + PairSenseFragment.class.getName();

    @Inject
    ApiService apiService;

    private int linkAccountFailures = 0;
    private boolean hasLinkedAccount = false;
    private boolean pairNewSenseSession = false;

    private final LocationPermission locationPermission = new LocationPermission(this);
    private OnboardingSimpleStepView view;

    public static PairSenseFragment newUpdateInstance() {
        final PairSenseFragment fragment = new PairSenseFragment();
        final Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_PAIR_NEW_SENSE, true);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.hasLinkedAccount = savedInstanceState.getBoolean("hasLinkedAccount", false);
        }

        setPairNewSenseSession();

        final Properties properties = Analytics.createBluetoothTrackingProperties(getActivity());
        if (isPairOnlySession()) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_SENSE_IN_APP, properties);
        } else {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_SENSE, properties);
        }

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {

        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText( pairNewSenseSession ?  R.string.title_pair_new_sense : R.string.title_pair_sense)
                .setSubheadingText( pairNewSenseSession ? R.string.info_pair_new_sense : R.string.info_pair_sense)
                .setDiagramImage(R.drawable.onboarding_pair_sense)
                .setSecondaryButtonText(R.string.action_sense_pairing_mode_help)
                .setSecondaryOnClickListener(this::showPairingModeHelp)
                .setPrimaryOnClickListener(ignored -> next())
                .setToolbarWantsBackButton(true)
                .setToolbarOnHelpClickListener(ignored -> {
                    UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.PAIRING_SENSE_BLE);
                })
                .setToolbarOnHelpLongClickListener(ignored -> {
                    showSupportOptions();
                    return true;
                })
                .configure(b -> subscribe(hardwarePresenter.bluetoothEnabled, b.primaryButton::setEnabled, Functions.LOG_ERROR));

        return view;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasLinkedAccount", hasLinkedAccount);
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
                    linkAccount();
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
        hideAllActivityForSuccess(this::showSelectWifiNetwork,
                                  e -> presentError(e, "Turning off LEDs"));
    }

    private void showSelectWifiNetwork() {
        getFragmentNavigation().flowFinished(this, REQUEST_CODE_EDIT_WIFI, null);
    }

    private void linkAccount() {
        if (hasLinkedAccount) {
            setDeviceTimeZone();
        } else {
            showBlockingActivity(R.string.title_linking_account);

            bindAndSubscribe(hardwarePresenter.linkAccount(),
                             ignored -> {
                                 this.hasLinkedAccount = true;
                                 setDeviceTimeZone();
                             },
                             error -> {
                                 Logger.error(PairSenseFragment.class.getSimpleName(), "Could not link Sense to account", error);
                                 presentError(error, OPERATION_LINK_ACCOUNT);
                             });
        }
    }

    private void setDeviceTimeZone() {
        showBlockingActivity(R.string.title_setting_time_zone);

        final SenseTimeZone timeZone = SenseTimeZone.fromDefault();
        bindAndSubscribe(apiService.updateTimeZone(timeZone),
                         ignored -> {
                             Logger.info(ConnectToWiFiFragment.class.getSimpleName(), "Time zone updated.");

                             pushDeviceData();
                         },
                         e -> presentError(e, "Updating time zone"));
    }

    private void pushDeviceData() {
        showBlockingActivity(R.string.title_pushing_data);
        bindAndSubscribe(hardwarePresenter.pushData(),
                         ignored -> getDeviceFeatures(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not push data from Sense, ignoring.", error);
                             getDeviceFeatures();
                         });
    }

    private void getDeviceFeatures() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(userFeaturesPresenter.storeFeaturesInPrefs(),
                         ignored -> finishedLinking(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not get features from Sense, ignoring.", error);
                             finishedLinking();
                         });
    }

    private void finishedLinking() {
        hideAllActivityForSuccess(() -> {
                                      if (isPairOnlySession()) {
                                          if (shouldReleasePeripheralOnPair()) {
                                              hardwarePresenter.clearPeripheral();
                                          }
                                          Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_PAIRED_IN_APP, null);
                                          getActivity().finish();
                                      } else {
                                          Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_PAIRED, null);
                                          getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null);
                                      }
                                  },
                                  e -> {

                                      Log.e("Error", "E: " + e.getLocalizedMessage());
                                      presentError(e, "Turning off LEDs");
                                  });
    }

    private void setPairNewSenseSession() {
        final Bundle arguments = getArguments();
        if(arguments != null){
            this.pairNewSenseSession = arguments.getBoolean(ARG_PAIR_NEW_SENSE);
        }
    }

    public void showPairingModeHelp(@NonNull final View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIRING_MODE_HELP, null);
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.PAIRING_MODE);
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
        if (BuildConfig.DEBUG) {
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
            showBlockingActivity(R.string.title_connecting);
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                if (status == ConnectProgress.CONNECTED) {
                    checkConnectivityAndContinue();
                } else {
                    showBlockingActivity(Styles.getConnectStatusMessage(status));
                }
            }, e -> presentError(e, "Connecting to Sense"));
        }
    }

    public void presentError(Throwable e, @NonNull String operation) {
        hideAllActivityForFailure(() -> {
            if (OPERATION_LINK_ACCOUNT.equals(operation)) {
                this.linkAccountFailures++;
                if (linkAccountFailures >= LINK_ACCOUNT_FAILURES_BEFORE_EDIT_WIFI) {
                    ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder()
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
                    PromptForHighPowerDialogFragment dialogFragment = new PromptForHighPowerDialogFragment();
                    dialogFragment.setTargetFragment(this, REQUEST_CODE_HIGH_POWER_RETRY);
                    dialogFragment.show(getFragmentManager(), PromptForHighPowerDialogFragment.TAG);
                } else {
                    TroubleshootSenseDialogFragment dialogFragment = new TroubleshootSenseDialogFragment();
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

    public static class TroubleshootSenseDialogFragment extends DialogFragment {
        public static final String TAG = TroubleshootSenseDialogFragment.class.getSimpleName();

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

            dialog.setTitle(R.string.dialog_title_troubleshoot_sense);
            dialog.setMessage(R.string.dialog_message_troubleshoot_sense);

            dialog.setPositiveButton(android.R.string.ok, null);
            dialog.setNegativeButton(R.string.action_help, (sender, which) -> {
                UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.PAIRING_SENSE_BLE);
            });

            return dialog;
        }
    }
}
