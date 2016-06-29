package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.commonsense.bluetooth.errors.SensePeripheralError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class OnboardingConnectPillFragment extends HardwareFragment {
    private ProgressBar activityIndicator;
    private TextView activityStatus;

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
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);

        this.diagram = (DiagramVideoView) view.findViewById(R.id.fragment_onboarding_pair_pill_diagram);


        final TextView titleTextView = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_title);
        final TextView infoTextView = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_subhead);
        //skipping this is not an option currently so we don't try to keep reference to skip button
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

    private void skipConnectingPill() {
        onFinish(false);
    }

    private void onBegin() {
        this.isConnecting = true;

        activityIndicator.setVisibility(View.VISIBLE);
        activityStatus.setVisibility(View.VISIBLE);

        retryButton.setVisibility(View.GONE);
    }

    private void onFinish(final boolean success) {
        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        getFragmentManager().executePendingTransactions();
        LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
            stateSafeExecutor.execute(() -> {
                hardwarePresenter.clearPeripheral();
                if (success) {
                    getOnboardingActivity().showUpdatePillComplete();
                } else {
                    getOnboardingActivity().finish();
                }
            });
        });
    }

    private void help(final View view) {
        //Todo point to user support page
    }

    public void connectPill() {
        onBegin();

        if (!hardwarePresenter.hasPeripheral())

        {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(), ignored -> connectPill(), this::presentError);
            return;
        }
        //Todo check with Jimmy if Sense needs to be connected with app to trigger dfu
        if (!hardwarePresenter.isConnected())

        {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                if (status == ConnectProgress.CONNECTED) {
                    connectPill();
                } else {
                    showBlockingActivity(Styles.getConnectStatusMessage(status));
                }
            }, this::presentError);
            return;
        }

        showBlockingActivity(R.string.title_waiting_for_sense);

        showHardwareActivity(() -> {
                                 diagram.startPlayback();
                                 hideBlockingActivity(false, () -> {
                                     onFinish(true);
                                     //Todo replace with connectPill to dfu mode
                                     /*bindAndSubscribe(hardwarePresenter.linkPill(),
                                                      ignored -> completeHardwareActivity(() -> onFinish(true)),
                                                      this::presentError);*/
                                 });
                             }
                , this::presentError);
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
            errorDialogBuilder.withOperation("Pair Pill");
            if (e instanceof OperationTimeoutException ||
                    SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.TIME_OUT)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_message_sleep_pill_scan_timeout));
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.NETWORK_ERROR)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_network_failure_pair_pill));
                errorDialogBuilder.withSupportLink();
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_pill_already_paired));
                errorDialogBuilder.withSupportLink();
            } else {
                errorDialogBuilder.withUnstableBluetoothHelp(getActivity());
            }

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }
}
