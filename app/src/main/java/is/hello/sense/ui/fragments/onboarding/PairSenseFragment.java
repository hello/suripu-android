package is.hello.sense.ui.fragments.onboarding;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.dialogs.PromptForHighPowerDialogFragment;
import is.hello.sense.ui.dialogs.TroubleshootSenseDialogFragment;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.SkippableFlow;
import rx.functions.Action0;


public class PairSenseFragment extends BasePresenterFragment
        implements
        FragmentCompat.OnRequestPermissionsResultCallback,
        PairSensePresenter.Output,
        OnBackPressedInterceptor
{

    private final LocationPermission locationPermission = new LocationPermission(this);
    private OnboardingSimpleStepView view;

    @Inject
    PairSensePresenter presenter;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }


    protected void sendOnCreateAnalytics() {
        final Properties properties = Analytics.createBluetoothTrackingProperties(getActivity());
        Analytics.trackEvent(presenter.getOnCreateAnalyticsEvent(), properties);
    }

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
                .setSecondaryOnClickListener(presenter::showPairingModeHelp)
                .setPrimaryOnClickListener(ignored -> onPrimaryButtonClicked()) // todo move to presenter
                .setToolbarWantsBackButton(true)
                .setToolbarOnHelpClickListener(ignored -> presenter.showToolbarHelp())
                .setToolbarOnHelpLongClickListener(ignored -> {
                    showSupportOptions(); //todo move to presenter
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
            onPrimaryButtonClicked();
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
    public void showErrorDialog(final ErrorDialogFragment.PresenterBuilder builder,
                             final int requestCode) {
        final ErrorDialogFragment dialogFragment = builder.build();

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
    public void showMessageDialog(@StringRes final int titleRes, @StringRes final int messageRes){
        final MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(titleRes, messageRes);
        messageDialogFragment.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);
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

    @Override
    public void onPrimaryButtonClicked() {
        if (!locationPermission.isGranted()) {
            locationPermission.requestPermissionWithDialog();
            return;
        }
        presenter.onLocationPermissionGranted();
    }

    protected void showSupportOptions() {
        if (! presenter.showSupportOptions()) {
            return;
        }

        Analytics.trackEvent(Analytics.Onboarding.EVENT_SUPPORT_OPTIONS, null);

        final SenseBottomSheet options = new SenseBottomSheet(getActivity());
        options.setTitle(R.string.title_recovery_options);
        options.addOption(new SenseBottomSheet.Option(0)
                                  .setTitle(R.string.action_factory_reset)
                                  .setTitleColor(ContextCompat.getColor(getActivity(), R.color.destructive_accent))
                                  .setDescription(R.string.description_recovery_factory_reset));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            options.addOption(new SenseBottomSheet.Option(1)
                                      .setTitle("Debug")
                                      .setTitleColor(ContextCompat.getColor(getActivity(), R.color.light_accent))
                                      .setDescription("If you're adventurous, but here there be dragons."));
            if (getActivity() instanceof SkippableFlow) {
                options.addOption(new SenseBottomSheet.Option(2)
                                          .setTitle("Skip to End")
                                          .setTitleColor(ContextCompat.getColor(getActivity(), R.color.light_accent))
                                          .setDescription("If you're in a hurry."));
            }
        }
        options.setOnOptionSelectedListener(option -> {
            switch (option.getOptionId()) {
                case 0: {
                    promptForRecoveryFactoryReset();
                    break;
                }
                case 1: {
                    Distribution.startDebugActivity(getActivity());
                    break;
                }
                case 2: {
                    ((SkippableFlow) getActivity()).skipToEnd();
                    break;
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
            return true;
        });
        options.show();
    }

    protected void promptForRecoveryFactoryReset() {
        Analytics.trackEvent(Analytics.Backside.EVENT_FACTORY_RESET, null);
        final SenseAlertDialog confirmation = new SenseAlertDialog(getActivity());
        confirmation.setTitle(R.string.dialog_title_factory_reset);
        confirmation.setMessage(R.string.dialog_message_factory_reset);
        confirmation.setNegativeButton(android.R.string.cancel, null);
        confirmation.setPositiveButton(R.string.action_factory_reset,
                                       (ignored, which) -> presenter.performRecoveryFactoryReset());
        confirmation.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        confirmation.show();
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        presenter.onBackPressed(defaultBehavior);
        return true;
    }
}
