package is.hello.sense.ui.fragments.onboarding.sense;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.DeviceOTAState;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

public class SenseOTAFragment extends HardwareFragment {

    @Inject
    ApiService apiService;

    private static final int REQUEST_STATUS_CHECK_INTERVAL_SECONDS = 5;
    private static final long REQUEST_STATUS_TIMEOUT_SECONDS = 150;
    private static final long REQUEST_STATUS_TIMEOUT_INTERVAL = 1;

    private ProgressBar progressBar;
    private Button retryButton;
    private Button skipButton;
    private TextView progressStatus;
    private OnboardingToolbar toolbar;

    public static SenseOTAFragment newInstance() {
        return new SenseOTAFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_onboarding_sense_update, container, false);

        this.progressBar = (ProgressBar) view.findViewById(R.id.fragment_onboarding_sense_update_progressbar);
        this.progressStatus = (TextView) view.findViewById(R.id.fragment_onboarding_sense_update_status);
        this.skipButton = (Button) view.findViewById(R.id.fragment_onboarding_sense_update_skip);

        Views.setSafeOnClickListener(skipButton, ignored -> skipUpdate());

        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_sense_update_retry);
        Views.setSafeOnClickListener(retryButton, ignored -> requestUpdate());

        this.toolbar = OnboardingToolbar.of(this, view)
                         .setWantsBackButton(false)
                         .setOnHelpClickListener(this::showHelp);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requestUpdate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressBar = null;
        progressStatus = null;
        retryButton.setOnClickListener(null);
        retryButton = null;
        skipButton.setOnClickListener(null);
        skipButton = null;
        toolbar.onDestroyView();
        toolbar = null;
    }

    private void requestUpdate() {
        updateUI(false);
        //todo maybe implement NullBodyAwareOkHttpClient https://github.com/wikimedia/apps-android-wikipedia/commit/f1a50adf0bcb550114cf0df42283d206ed7e45d7
        Analytics.trackEvent(Analytics.SenseUpdate.EVENT_START, null);
        bindAndSubscribe(apiService.requestSenseUpdate(""),
                         ignored -> Logger.info(SenseOTAFragment.class.getSimpleName(), "Sense update request sent."),
                         e -> presentError(e, "Requesting Sense Update"));

        bindAndSubscribe(apiService.getSenseUpdateStatus().repeatWhen(
                onComplete -> onComplete.delay(REQUEST_STATUS_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS,
                                               observeScheduler)
        ), response -> {
             sendAnalyticsStatusUpdate(response.state);
             setProgressStatus(response.state);
             if(response.state.equals(DeviceOTAState.OtaState.COMPLETE)) {
                 Logger.info(SenseOTAFragment.class.getSimpleName(), "Sense updated.");
                 done();
             }
        },
         e -> presentError(e, "Updating Sense"));

        bindAndSubscribe(Observable.interval(REQUEST_STATUS_TIMEOUT_INTERVAL, TimeUnit.SECONDS),
                         tickSeconds -> {
                             if(REQUEST_STATUS_TIMEOUT_SECONDS == tickSeconds) {
                                 stateSafeExecutor.execute(() -> presentError(new TimeoutException(), "Updating Sense Timeout"));
                             }
                         },
                         e -> presentError(e, "Updating Sense Timeout"));

    }

    private void sendAnalyticsStatusUpdate(final DeviceOTAState.OtaState state) {
        if(!this.progressStatus.getText().equals(state.name())){
            Analytics.trackEvent(Analytics.SenseUpdate.EVENT_STATUS,
                                 Analytics.createProperties(Analytics.SenseUpdate.PROPERY_NAME, state.name()));
        }
    }

    private void setProgressStatus(final DeviceOTAState.OtaState state) {
        if(state.equals(DeviceOTAState.OtaState.IN_PROGRESS)){
            this.progressStatus.post(
                    stateSafeExecutor.bind(() -> this.progressStatus.setText(R.string.sense_downloading_update)));
        }
    }

    private void skipUpdate(){
        final SenseAlertDialog alertDialog = new SenseAlertDialog(getActivity());
        alertDialog.setTitle(R.string.title_update_later);
        alertDialog.setMessage(R.string.sense_try_later_dialog_message);
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setPositiveButton(R.string.action_ok, (dialog, which) -> {
            cancelFlow();
            dialog.dismiss();
        });

        alertDialog.show();
    }

    private void done() {
        Analytics.trackEvent(Analytics.SenseUpdate.EVENT_END, null);
        stateSafeExecutor.execute( () -> {
            if(userFeaturesPresenter.hasVoice()){
                LoadingDialogFragment.show(getFragmentManager(),
                                           null, LoadingDialogFragment.OPAQUE_BACKGROUND);
                getFragmentManager().executePendingTransactions();
                LoadingDialogFragment.closeWithMessageTransition(
                        getFragmentManager(),
                        stateSafeExecutor.bind(this::finishFlow),
                        R.string.sense_updated);
            } else {
                finishFlow();
            }
        });
    }

    public void showHelp(@NonNull final View sender) {
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATING_SENSE);
    }

    public void presentError(final Throwable e, @NonNull final String operation) {
        observableContainer.clearSubscriptions();
        hideBlockingActivity(false, () -> {
            updateUI(true);

            final int titleRes = R.string.error_update_failed;
            int messageRes = R.string.error_sense_update_failed_message;

            if(ApiException.isNetworkError(e)){
                messageRes = R.string.error_internet_connection_generic_message;
            }

            final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                    .withTitle(titleRes)
                    .withMessage(StringRef.from(messageRes))
                    .withOperation(operation)
                    .withSupportLink()
                    .build();

            dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    private void updateUI(final boolean showRetry) {
        final int hideOnRetry = showRetry ? View.GONE : View.VISIBLE;
        final int showOnRetry = showRetry ? View.VISIBLE : View.GONE;
        progressBar.setVisibility(hideOnRetry);
        progressStatus.setVisibility(hideOnRetry);
        retryButton.setVisibility(showOnRetry);
        skipButton.setVisibility(showOnRetry);
    }
}
