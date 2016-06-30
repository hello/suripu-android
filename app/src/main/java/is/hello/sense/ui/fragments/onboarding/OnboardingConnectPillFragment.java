package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.commonsense.bluetooth.errors.SensePeripheralError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class OnboardingConnectPillFragment extends HardwareFragment {
    private ProgressBar activityIndicator;
    private ProgressBar updateIndicator;
    private TextView activityStatus;
    private TextView titleTextView;
    private TextView infoTextView;
    private DiagramVideoView diagram;

    private Button retryButton;

    private boolean isConnecting = false;

    public OnboardingConnectPillFragment(){
        //required empty public constructor
        super();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_activity);
        this.updateIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_progress_determinate);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);

        this.diagram = (DiagramVideoView) view.findViewById(R.id.fragment_onboarding_pair_pill_diagram);


        this.titleTextView = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_title);
        this.infoTextView = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_subhead);
        //skipping this is not an option currently so we don't keep reference to skip button
        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_retry);


        titleTextView.setText(R.string.title_connect_sleep_pill);
        infoTextView.setText(R.string.info_connect_sleep_pill);

        Views.setSafeOnClickListener(retryButton, ignored -> connectPill());

        OnboardingToolbar.of(this, view)
                         .setWantsBackButton(false)
                         .setOnHelpClickListener(this::help);

        if (BuildConfig.DEBUG) {
            diagram.setOnLongClickListener(ignored -> {
                skipConnectingPill();
                return true;
            });
            diagram.setBackgroundResource(R.drawable.selectable_dark);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isConnecting) {
            connectPill();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diagram != null) {
            diagram.destroy();
        }

        this.activityIndicator = null;
        this.activityStatus = null;

        this.diagram = null;

        this.retryButton = null;
    }

    public void connectPill() {
        onBegin();

        if (!hardwarePresenter.hasPeripheral())

        {
            //Todo replace with phone battery level checks
            showBlockingActivity(R.string.title_checking_connectivity);
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(), ignored -> connectPill(), this::presentError);
            return;
        }
        //Todo replace hardwarePresenter with pillHardwarePresenter
        if (!hardwarePresenter.isConnected())

        {
            //Todo replace with pill battery level and bluetooth connectivity strength checks
            showBlockingActivity(R.string.title_checking_connectivity);
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                if (status == ConnectProgress.CONNECTED) {
                    connectPill();
                } else {
                    showBlockingActivity(Styles.getConnectStatusMessage(status));
                }
            }, this::presentError);
            return;
        }

        diagram.startPlayback();
        hideBlockingActivity(false, () -> {
            onConnected();
            //Todo replace with connectPill to dfu mode
                                 /*bindAndSubscribe(hardwarePresenter.linkPill(),
                                                  ignored -> completeHardwareActivity(() -> onFinish(true)),
                                                  this::presentError);*/
        });

    }

    public void presentError(final Throwable e) {
        this.isConnecting = false;

        diagram.suspendPlayback(true);
        hideAllActivityForFailure(() -> {
            activityIndicator.setVisibility(View.GONE);
            activityStatus.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);

            //Todo update error strings
            final ErrorDialogFragment.Builder errorDialogBuilder =
                    new ErrorDialogFragment.Builder(e, getActivity());
            errorDialogBuilder.withOperation("Connect Pill");
            if (e instanceof OperationTimeoutException ||
                    SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.TIME_OUT)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_message_sleep_pill_scan_timeout));
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.NETWORK_ERROR)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_network_failure_pair_pill));
                errorDialogBuilder.withSupportLink();
            } else {
                errorDialogBuilder.withUnstableBluetoothHelp(getActivity());
            }

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    private void skipConnectingPill() {
        onFinish(false);
    }

    private void onBegin() {
        this.isConnecting = true;

        activityIndicator.setVisibility(View.VISIBLE);
        activityStatus.setVisibility(View.VISIBLE);

        retryButton.setVisibility(View.GONE);
    }

    private void onConnected(){
        stateSafeExecutor.execute(() -> {
            diagram.destroy();
            //Todo distorts the drawable because different size layout
            diagram.setPlaceholder(R.drawable.sleep_pill_ota);
            diagram.invalidate();
            activityIndicator.setVisibility(View.GONE);
            updateIndicator.setVisibility(View.VISIBLE);
            activityStatus.setText(R.string.message_sleep_pill_updating);
            titleTextView.setText(R.string.title_update_sleep_pill);
            infoTextView.setText(R.string.info_update_sleep_pill);

            //Todo replace mock of progress with real hardware pill presenter work update
            stateSafeExecutor.execute( () -> {
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if(updateIndicator == null){
                            this.cancel();
                        }
                        int progress = updateIndicator.getProgress();
                        if (progress < updateIndicator.getMax()) {
                            updateIndicator.setProgress(++progress);
                        } else{
                            this.cancel();
                            updateIndicator.post(() -> {
                                OnboardingConnectPillFragment.this.onFinish(true);
                            });
                        }
                    }
                }, 1000, 200);

            });
        });
    }

    private void onFinish(final boolean success) {
        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        getFragmentManager().executePendingTransactions();
        LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(), () -> {
            stateSafeExecutor.execute(() -> {
                hardwarePresenter.clearPeripheral();
                if (success) {
                    //Todo see if able to fade out
                    getOnboardingActivity().finish();
                } else {
                    getOnboardingActivity().finish();
                }
            });
        }, R.string.message_sleep_pill_updated);
    }

    private void help(final View view) {
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATE_PILL);
    }
}
