package is.hello.sense.ui.fragments.sense;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.presenters.BasePairSensePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.SkippableFlow;

public abstract class BasePairSenseFragment extends BasePresenterFragment
implements BasePairSensePresenter.Output{

    @Inject
    protected BasePairSensePresenter presenter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addScopedPresenter(presenter);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected void sendOnCreateAnalytics() {
        final Properties properties = Analytics.createBluetoothTrackingProperties(getActivity());
        Analytics.trackEvent(presenter.getOnCreateAnalyticsEvent(), properties);
    }

    /**region {@link BasePairSensePresenter.Output}**/

    @Override
    public void finishPairFlow(final int resultCode){
        finishFlowWithResult(resultCode);
    }

    @Override
    public void finishActivity(){
        getActivity().finish();
    }

    @Override
    public abstract void presentError(StringRef message,
                                      int resultCode,
                                      @StringRes int actionStringRes,
                                      String operation,
                                      int requestCode);


    @Override
    public void presentFactoryResetDialog(final Throwable e, final String operation) {
        hideBlockingActivity(false, () -> {
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                    .withOperation(operation)
                    .withSupportLink()
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    @Override
    public void showMessageDialog(@StringRes final int titleRes, @StringRes final int messageRes){
        final MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(titleRes, messageRes);
        messageDialogFragment.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);
    }

    //end region

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

    protected OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }
}

