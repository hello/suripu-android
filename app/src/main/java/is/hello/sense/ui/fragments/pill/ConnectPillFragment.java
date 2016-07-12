package is.hello.sense.ui.fragments.pill;

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
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class ConnectPillFragment extends HardwareFragment {
    private ProgressBar activityIndicator;
    private TextView activityStatus;
    private DiagramVideoView diagram;
    private Button retryButton;

    private boolean isConnecting = false;

    public ConnectPillFragment(){
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

        retryButton.setOnClickListener(null);
        this.retryButton = null;
    }

    public void connectPill() {
        onBegin();

        if (!hardwarePresenter.hasPeripheral())

        {
            //Todo replace with phone battery level checks
            activityStatus.setText(R.string.title_checking_connectivity);
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(), ignored -> connectPill(), this::presentError);
            return;
        }
        //Todo replace hardwarePresenter with pillHardwarePresenter
        if (!hardwarePresenter.isConnected())

        {
            //Todo replace with pill battery level and bluetooth connectivity strength checks
            activityStatus.setText(R.string.label_searching_for_pill);
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

        //Todo the loading done drawable is not displayed quick enough before fragment is closed
        activityIndicator.setActivated(true);
        activityStatus.setText(R.string.message_sleep_pill_connected);
        activityStatus.postDelayed( stateSafeExecutor.bind(() -> onFinish(true)), 1200);

    }

    public void presentError(final Throwable e) {
        this.isConnecting = false;

        diagram.suspendPlayback(true);
        hideAllActivityForFailure(() -> {
            activityIndicator.setVisibility(View.GONE);
            activityStatus.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);

            //Todo update error checks
            final ErrorDialogFragment.Builder errorDialogBuilder =
                    new ErrorDialogFragment.Builder(e, getActivity());

            if (e instanceof OperationTimeoutException ||
                    SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.TIME_OUT)) {
                errorDialogBuilder
                        .withTitle(R.string.error_sleep_pill_title_update_missing)
                        .withMessage(StringRef.from(R.string.error_sleep_pill_message_update_missing));
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.NETWORK_ERROR)) {
                errorDialogBuilder
                        .withTitle(R.string.error_sleep_pill_title_update_missing)
                        .withMessage(StringRef.from(R.string.error_sleep_pill_message_update_missing));
                errorDialogBuilder.withSupportLink();
            } else {
                errorDialogBuilder.withTitle(R.string.action_turn_on_ble)
                                  .withMessage(StringRef.from(R.string.info_turn_on_bluetooth));
            }

            final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_WEAK_RSSI.getUri().toString();
            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder
                    .withOperation("Connect Pill")
                    .withAction(helpUriString, R.string.label_having_trouble)
                    .build();
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

    private void onFinish(final boolean success) {
        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        getFragmentManager().executePendingTransactions();
        LoadingDialogFragment.closeWithOnComplete(
                getFragmentManager(),
                stateSafeExecutor.bind(() -> {
                    hardwarePresenter.clearPeripheral();
                    if (success) {
                        ((FragmentNavigation) getActivity()).flowFinished(this, PillUpdateActivity.FLOW_UPDATE_PILL_SCREEN, null);
                    } else {
                        getActivity().finish();
                    }
                }));
    }

    private void help(final View view) {
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATE_PILL);
    }
}
