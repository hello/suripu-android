package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import rx.functions.Action0;

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
    public void presentHighPowerErrorDialog(final int requestCode) {
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

    @Override
    public void showPairDialog(final String deviceName,
                               final Action0 positiveAction,
                               final Action0 negativeAction){
        final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setTitle(R.string.debug_title_confirm_sense_pair);
        dialog.setMessage(getString(R.string.debug_message_confirm_sense_pair_fmt, deviceName));
        dialog.setPositiveButton(android.R.string.ok, (sender, which) -> positiveAction.call());
        dialog.setNegativeButton(android.R.string.cancel, (sender, which) -> negativeAction.call());
        dialog.setCancelable(false);
        dialog.show();
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
}
