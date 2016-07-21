package is.hello.sense.ui.fragments.onboarding.sense;

import android.app.Activity;
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
import is.hello.sense.api.model.DeviceOTAState;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.schedulers.Schedulers;

public class SenseUpdateFragment extends HardwareFragment {

    @Inject
    ApiService apiService;

    private static final int REQUEST_STATUS_CHECK_INTERVAL_SECONDS = 5;
    private static final long REQUEST_STATUS_TIMEOUT_SECONDS = 150;

    private ProgressBar progressBar;
    private Button retryButton;
    private Button skipButton;
    private TextView progressStatus;



    public static SenseUpdateFragment newInstance() {
        return new SenseUpdateFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_onboarding_sense_update, container, false);

        this.progressBar = (ProgressBar) view.findViewById(R.id.fragment_onboarding_sense_update_progressbar);
        this.progressStatus = (TextView) view.findViewById(R.id.fragment_onboarding_sense_update_status);
        this.skipButton = (Button) view.findViewById(R.id.fragment_onboarding_sense_update_skip);

        Views.setSafeOnClickListener(skipButton, ignored -> skipUpdate());

        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_sense_update_retry);
        Views.setSafeOnClickListener(retryButton, ignored -> requestUpdate());

        OnboardingToolbar.of(this, view)
                         .setWantsBackButton(isPairOnlySession())
                         .setOnHelpClickListener(this::showHelp);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requestUpdate();
    }

    private void requestUpdate() {
        updateUI(false);

        bindAndSubscribe(apiService.requestSenseUpdate(""), //todo maybe implement NullBodyAwareOkHttpClient https://github.com/wikimedia/apps-android-wikipedia/commit/f1a50adf0bcb550114cf0df42283d206ed7e45d7
                         ignored -> {
                             Logger.info(SenseUpdateFragment.class.getSimpleName(), "Sense update request sent.");
                         },
                         e -> presentError(e, "Updating Sense"));

        bindAndSubscribe(Observable.interval(REQUEST_STATUS_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS, Schedulers.io())
                .flatMap( func -> apiService.getSenseUpdateStatus()),
                         state -> {
                             setProgressStatus(state.state);
                             if(state.state.equals(DeviceOTAState.OtaState.COMPLETE)) {
                                 Logger.info(SenseUpdateFragment.class.getSimpleName(), "Sense updated.");
                                 done();
                             }
                         },
                         Functions.LOG_ERROR);

        bindAndSubscribe(Observable.timer(REQUEST_STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS, Schedulers.io()),
                         ignored -> {
                             Logger.info(SenseUpdateFragment.class.getSimpleName(), "Sense update timeout. Retry to send new request.");
                             observableContainer.clearSubscriptions();
                             stateSafeExecutor.execute( () -> {
                                 presentError(new TimeoutException(), "Sense Update");
                             });
                         },
                         Functions.LOG_ERROR);

    }

    private void setProgressStatus(DeviceOTAState.OtaState state) {
        this.progressStatus.post( () -> {
            this.progressStatus.setText(state.state);
        });
    }

    private void skipUpdate(){
        getFragmentNavigation().flowFinished(this, Activity.RESULT_CANCELED, null);
    }

    private void done() {
        // todo update message to Sense updated
        hideBlockingActivity(true, () -> {
            getOnboardingActivity().checkForSenseUpdate();
        });
    }

    public void showHelp(@NonNull View sender) {
        //todo replace with sense ota analytics
        //Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIRING_MODE_HELP, null);
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.PAIRING_MODE);
    }

    public void presentError(final Throwable e, @NonNull final String operation) {
        hideBlockingActivity(false, () -> {
            updateUI(true);

            final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder()
                    .withMessage(StringRef.from(R.string.error_sense_update_failed_message))
                    .withOperation(operation)
                    .withSupportLink()
                    .build();

            dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    private void updateUI(boolean showRetry) {
        progressBar.setVisibility(showRetry ? View.GONE : View.VISIBLE);
        progressStatus.setVisibility(showRetry ? View.GONE : View.VISIBLE);
        retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        skipButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }
}
