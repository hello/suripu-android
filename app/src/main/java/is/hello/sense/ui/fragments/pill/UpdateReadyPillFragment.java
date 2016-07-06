package is.hello.sense.ui.fragments.pill;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.commonsense.bluetooth.errors.SensePeripheralError;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.bluetooth.PillDfuPresenter;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.SenseCache;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class UpdateReadyPillFragment extends PillHardwareFragment
        implements OnBackPressedInterceptor, DfuProgressListener {
    private static final String TAG = UpdateReadyPillFragment.class.getSimpleName();

    private ProgressBar updateIndicator;
    private TextView activityStatus;
    private Button retryButton;
    private Button skipButton;
    private final ViewAnimator viewAnimator = new ViewAnimator();

    private SenseAlertDialog backPressedDialog;
    private OnboardingToolbar toolbar;

    @Inject
    SenseCache.FirmwareCache firmwareCache;

    @Inject
    PillDfuPresenter pillDfuPresenter;

    public static Fragment newInstance() {
        return new UpdateReadyPillFragment();
    }

    public UpdateReadyPillFragment() {
        //required empty public constructor //todo why?
        super();
    }

    @Override
    protected String operationString() {
        return null;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPresenter(firmwareCache);
        addPresenter(pillDfuPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pill_update, container, false);
        this.updateIndicator = (ProgressBar) view.findViewById(R.id.fragment_update_pill_progress_determinate);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_update_pill_status);
        final TextView titleTextView = (TextView) view.findViewById(R.id.fragment_update_pill_title);
        final TextView infoTextView = (TextView) view.findViewById(R.id.fragment_update_pill_subhead);
        //skipping this is not an option currently so we don't keep reference to skip button
        this.retryButton = (Button) view.findViewById(R.id.fragment_update_pill_retry);
        final View animatedView = view.findViewById(R.id.blue_box_view);

        updateIndicator.setVisibility(View.VISIBLE);
        activityStatus.setText(R.string.message_sleep_pill_updating);

        titleTextView.setText(R.string.title_update_sleep_pill);
        infoTextView.setText(R.string.info_update_sleep_pill);

        Views.setTimeOffsetOnClickListener(retryButton, ignored -> updatePill());
        Views.setTimeOffsetOnClickListener(skipButton, ignored -> skipUpdatingPill());

        viewAnimator.setAnimatedView(animatedView);
        Views.setSafeOnClickListener(retryButton, ignored -> {
            retryButton.setVisibility(View.GONE);
            activityStatus.setVisibility(View.VISIBLE);
            updateIndicator.setVisibility(View.VISIBLE);
            firmwareCache.update();
        });

        this.toolbar = OnboardingToolbar.of(this, view)
                         .setWantsBackButton(false)
                         .setOnHelpClickListener(this::help);

   

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewAnimator.onViewCreated(getActivity(), R.animator.bluetooth_sleep_pill_ota_animator);
        bindAndSubscribe(firmwareCache.file,
                         file -> pillDfuPresenter.startDfuService(file)
                                                 .subscribe(),
                         this::presentError);
        firmwareCache.update();
    }

    @Override
    public void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(getActivity(), this);
        viewAnimator.onResume();
    }


    @Override
    public void onPause() {
        DfuServiceListenerHelper.unregisterProgressListener(getActivity(), this);
        viewAnimator.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.activityStatus = null;
        this.retryButton.setOnClickListener(null);
        this.retryButton = null;
        this.backPressedDialog = null;

        viewAnimator.onDestroyView();

        toolbar.onDestroyView();
        toolbar = null;
    }


    public void presentError(final Throwable e) {
        Log.e(TAG, "Error: " + e);

            updateIndicator.setVisibility(View.GONE);
            activityStatus.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.VISIBLE);

        //Todo update error checks
        final ErrorDialogFragment.Builder errorDialogBuilder =
                new ErrorDialogFragment.Builder(e, getActivity());
        errorDialogBuilder.withOperation("Update Pill");
        if (e instanceof OperationTimeoutException ||
                SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.TIME_OUT)) {
            errorDialogBuilder
                    .withTitle(R.string.error_sleep_pill_title_update_fail)
                    .withMessage(StringRef.from(R.string.error_sleep_pill_message_update_fail));
        } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.NETWORK_ERROR)) {
            errorDialogBuilder
                    .withTitle(R.string.error_sleep_pill_title_update_fail)
                    .withMessage(StringRef.from(R.string.error_sleep_pill_message_update_fail));
            errorDialogBuilder.withSupportLink();
        } else {
            errorDialogBuilder.withTitle(R.string.action_turn_on_ble)
                              .withMessage(StringRef.from(R.string.info_turn_on_bluetooth));
        }

        final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    private void skipUpdatingPill() {
        onFinish(false);
    }

    private void onBegin() {
        updateIndicator.setVisibility(View.VISIBLE);
        activityStatus.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
    }


    private void onFinish(final boolean success) {
        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.OPAQUE_BACKGROUND);
            getFragmentManager().executePendingTransactions();

            if(!success){
                ((FragmentNavigation) getActivity()).flowFinished(this, PillUpdateActivity.FLOW_CANCELED, null);
                return;
            }

            LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(), () -> {
                stateSafeExecutor.execute(() -> {
                    if (success) {
                        final String deviceId = "BF39B2A810B9813D"; //todo fix hardcoded
                        final Intent intent = new Intent();
                        intent.putExtra(PillUpdateActivity.EXTRA_DEVICE_ID, deviceId);
                        ((FragmentNavigation) getActivity()).flowFinished(this, PillUpdateActivity.FLOW_FINISHED, intent);
                    } else {
                        getActivity().finish();
                    }
                });
              
            }, R.string.message_sleep_pill_updated);
        });
    }

    private void help(final View view) {
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATE_PILL);
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if (this.backPressedDialog == null) {
            this.backPressedDialog = new SenseAlertDialog(getActivity());
            backPressedDialog.setCanceledOnTouchOutside(true);
            backPressedDialog.setTitle(R.string.dialog_title_confirm_leave_app);
            backPressedDialog.setMessage(R.string.dialog_message_confirm_leave_update_pill);
            backPressedDialog.setPositiveButton(R.string.action_ok, (which, ignored) -> {
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
            });
            backPressedDialog.setNegativeButton(android.R.string.cancel, null);
        }
        backPressedDialog.show();
        return true;
    }


    @Override
    public void onDeviceConnecting(final String deviceAddress) {
        Log.e("DFU Listener", "onDeviceConnecting");
    }

    @Override
    public void onDeviceConnected(final String deviceAddress) {
        Log.e("DFU Listener", "onDeviceConnected");
        
    }

    @Override
    public void onDfuProcessStarting(final String deviceAddress) {
        Log.e("DFU Listener", "onDfuProcessStarting");

    }

    @Override
    public void onDfuProcessStarted(final String deviceAddress) {
        Log.e("DFU Listener", "onDfuProcessStarted");

    }

    @Override
    public void onEnablingDfuMode(final String deviceAddress) {
        Log.e("DFU Listener", "onEnablingDfuMode");

    }

    @Override
    public void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
        Log.e("DFU Listener", "onProgressChanged " + percent + "%");
        updateIndicator.post(() -> {
            updateIndicator.setProgress(percent);
        });


    }

    @Override
    public void onFirmwareValidating(final String deviceAddress) {
        Log.e("DFU Listener", "onFirmwareValidating");

    }

    @Override
    public void onDeviceDisconnecting(final String deviceAddress) {
        Log.e("DFU Listener", "onDeviceDisconnecting");

    }

    @Override
    public void onDeviceDisconnected(final String deviceAddress) {
        Log.e("DFU Listener", "onDeviceDisconnected");

    }

    @Override
    public void onDfuCompleted(final String deviceAddress) {
        Log.e("DFU Listener", "onDfuCompleted");
        onFinish(true);
    }

    @Override
    public void onDfuAborted(final String deviceAddress) {
        Log.e("DFU Listener", "onDfuAborted");

    }

    @Override
    public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
        Log.e("DFU Listener", "onError: " + message + ". errorType: " + errorType + ". error: " + error);
        presentError(new Throwable(message));
    }

}
