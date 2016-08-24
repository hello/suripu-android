package is.hello.sense.ui.fragments.onboarding;

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

import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.PromptForHighPowerDialogFragment;
import is.hello.sense.ui.dialogs.TroubleshootSenseDialogFragment;
import is.hello.sense.ui.fragments.sense.BasePairSenseFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class PairSenseFragment extends BasePairSenseFragment
        implements FragmentCompat.OnRequestPermissionsResultCallback {

    private final LocationPermission locationPermission = new LocationPermission(this);
    private OnboardingSimpleStepView view;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sendOnCreateAnalytics();

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {


        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(presenter.getTitleRes())
                .setSubheadingText(presenter.getSubtitleRes())
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
                .configure(b -> presenter.provideBluetoothEnabledSubscription(b.primaryButton::setEnabled));

        return view;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
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

    @Override
    public void requestPermissionWithDialog() {
        locationPermission.requestPermissionWithDialog();
    }

    private void checkConnectivityAndContinue() {
        showHardwareActivity(() -> {
            bindAndSubscribe(hardwareInteractor.currentWifiNetwork(), network -> {
                if (network.connectionState == SenseCommandProtos.wifi_connection_state.IP_RETRIEVED) {
                    presenter.checkLinkedAccount();
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
        hideAllActivityForSuccess(presenter.getFinishedRes(),
                                  this::showSelectWifiNetwork,
                                  e -> presentError(e, "Turning off LEDs"));
    }

    @Override
    public void onFinished() {
        hideAllActivityForSuccess(presenter.getFinishedRes(),
                                   () -> {
                                       sendOnFinishedAnalytics();
                                       presenter.onPairSuccess();
                                  },
                                  e -> {
                                      Log.e("Error", "E: " + e.getLocalizedMessage());
                                      presentError(e, "Turning off LEDs");
                                  });
    }

    @Override
    public void presentError(final StringRef message,
                             final int actionResultCode,
                             @StringRes final int actionStringRes,
                             final String operation,
                             final int requestCode) {
        final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder()
                .withMessage(message)
                .withAction(actionResultCode, actionStringRes)
                .withOperation(operation)
                .withSupportLink()
                .build();

        dialogFragment.setTargetFragment(this, requestCode);
        dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    @Override
    public void presentHighPowerErrorDialog(int requestCode) {
        final PromptForHighPowerDialogFragment dialogFragment = new PromptForHighPowerDialogFragment();
        dialogFragment.setTargetFragment(this, requestCode);
        dialogFragment.show(getFragmentManager(), PromptForHighPowerDialogFragment.TAG);
    }

    @Override
    public void presentTroubleShootingDialog() {
        final TroubleshootSenseDialogFragment dialogFragment = new TroubleshootSenseDialogFragment();
        dialogFragment.show(getFragmentManager(), TroubleshootSenseDialogFragment.TAG);
    }

    @Override
    public void presentUnstableBluetoothDialog(final Throwable e, @NonNull final String operation) {
        final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                .withUnstableBluetoothHelp(getActivity())
                .withOperation(operation)
                .build();
        dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
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
        presenter.onLocationPermissionGranted();
    }

    public void tryToPairWith(@NonNull final SensePeripheral device) {
        if (presenter.shouldShowPairDialog()) {
            final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
            dialog.setTitle(R.string.debug_title_confirm_sense_pair);
            dialog.setMessage(getString(R.string.debug_message_confirm_sense_pair_fmt, device.getName()));
            dialog.setPositiveButton(android.R.string.ok, (sender, which) -> completePeripheralPair());
            dialog.setNegativeButton(android.R.string.cancel, (sender, which) -> hideBlockingActivity(false, hardwareInteractor::clearPeripheral));
            dialog.setCancelable(false);
            dialog.show();
        } else {
            completePeripheralPair();
        }
    }

    public void completePeripheralPair() {
        if (presenter.hasPeripheralPair()) {
            presenter.bindAndSubscribe(hardwareInteractor.clearBond(),
                             ignored -> presenter.hasPeripheralPair()
                             ,
                             e -> presentError(e, "Clearing Bond"));
        } else {
            presenter.bindAndSubscribe(hardwareInteractor.connectToPeripheral(),
                             status -> {
                                 if(presenter.hasConnectivity(status)){
                                     checkConnectivityAndContinue();
                                 }},
                             e -> presentError(e, "Connecting to Sense"));
        }
    }
}
