package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.errors.OperationTimeoutException;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.bluetooth.PillDfuPresenter;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import is.hello.sense.bluetooth.exceptions.RssiException;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.SenseCache;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class UpdateReadyPillFragment extends PillHardwareFragment
        implements OnBackPressedInterceptor, DfuProgressListener {

    private final ViewAnimator viewAnimator = new ViewAnimator();
    private SenseAlertDialog backPressedDialog;
    private SenseAlertDialog skipPressedDialog;
    private ProgressBar updateIndicator;
    private TextView activityStatus;
    private Button retryButton;
    private Button skipButton;
    private boolean isUploading = false;

    @Inject
    SenseCache.FirmwareCache firmwareCache;

    public static Fragment newInstance() {
        return new UpdateReadyPillFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getFragmentNavigation() == null) {
            finishWithResult(Activity.RESULT_CANCELED, null);
            return;
        }
        addPresenter(firmwareCache);
        skipPressedDialog = new SenseAlertDialog(getActivity());
        skipPressedDialog.setCanceledOnTouchOutside(true);
        skipPressedDialog.setTitle(R.string.dialog_title_skip_update);
        skipPressedDialog.setMessage(R.string.dialog_message_skip_update);
        skipPressedDialog.setPositiveButton(R.string.action_ok, (which, ignored) -> {
            onFinish(false);
        });
        skipPressedDialog.setNegativeButton(android.R.string.cancel, null);

        backPressedDialog = new SenseAlertDialog(getActivity());
        backPressedDialog.setCanceledOnTouchOutside(true);
        backPressedDialog.setTitle(R.string.dialog_title_confirm_leave_app);
        backPressedDialog.setMessage(R.string.dialog_message_confirm_leave_update_pill);
        backPressedDialog.setPositiveButton(R.string.action_ok, (which, ignored) -> {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        });
        backPressedDialog.setNegativeButton(android.R.string.cancel, null);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pill_update, container, false);
        this.updateIndicator = (ProgressBar) view.findViewById(R.id.fragment_update_pill_progress_determinate);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_update_pill_status);
        this.retryButton = (Button) view.findViewById(R.id.fragment_update_pill_retry);
        this.skipButton = (Button) view.findViewById(R.id.fragment_update_pill_skip);
        final TextView titleTextView = (TextView) view.findViewById(R.id.fragment_update_pill_title);
        final TextView infoTextView = (TextView) view.findViewById(R.id.fragment_update_pill_subhead);
        viewAnimator.setAnimatedView(view.findViewById(R.id.blue_box_view));
        activityStatus.setText(R.string.message_sleep_pill_updating);
        titleTextView.setText(R.string.title_update_sleep_pill);
        infoTextView.setText(R.string.info_update_sleep_pill);
        Views.setTimeOffsetOnClickListener(skipButton, ignored -> skipPressedDialog.show());
        Views.setSafeOnClickListener(retryButton, ignored -> updatePill());
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
                         file -> {
                             isUploading = true;
                             pillDfuPresenter.startDfuService(file)
                                             .subscribe();
                         },
                         this::presentError);
        updatePill();
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
        this.pillDfuPresenter.reset();
        this.activityStatus = null;
        this.retryButton.setOnClickListener(null);
        this.retryButton = null;
        this.skipPressedDialog = null;
        this.backPressedDialog = null;

        viewAnimator.onDestroyView();
        super.onDestroyView();
    }


    @Override
    void onLocationPermissionGranted(final boolean isGranted) {
        if (isGranted) {
            updatePill();
        }
    }

    private void updatePill() {
        if (isLocationPermissionGranted()) {
            toolbar.setVisible(false);
            retryButton.setVisibility(View.GONE);
            activityStatus.setVisibility(View.VISIBLE);
            updateIndicator.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.GONE);
            firmwareCache.update();
        } else {
            requestLocationPermission();
        }
    }

    public void presentError(final Throwable e) {
        toolbar.setVisible(true);
        isUploading = false;
        updateIndicator.setVisibility(View.GONE);
        activityStatus.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
        skipButton.setVisibility(View.VISIBLE);

        @StringRes int title = R.string.error_sleep_pill_title_update_fail;
        @StringRes int message = R.string.error_sleep_pill_message_update_fail;
        final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_WEAK_RSSI.getUri().toString();
        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getActivity());
        errorDialogBuilder.withOperation(StringRef.from(R.string.update_ready_pill_fragment_operation).toString());
        if (e instanceof RssiException) {
            Analytics.trackEvent(Analytics.PillUpdate.Error.PILL_TOO_FAR, null);
            title = R.string.error_pill_too_far;
        } else if (e instanceof PillNotFoundException) {
            Analytics.trackEvent(Analytics.PillUpdate.Error.PILL_NOT_DETECTED, null);
            title = R.string.error_pill_not_found;
        } else if (e instanceof ApiException) {
            title = R.string.network_activity_no_connectivity;
            message = R.string.error_network_failure_pair_pill;
        }
        errorDialogBuilder
                .withTitle(title)
                .withMessage(StringRef.from(message))
                .withAction(helpUriString, R.string.label_having_trouble)
                .build()
                .showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    private void onFinish(final boolean success) {
        if (!success) {
            getFragmentNavigation().flowFinished(this, Activity.RESULT_CANCELED, null);
            return;
        }
        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        getFragmentManager().executePendingTransactions();
        LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(), () ->
                stateSafeExecutor.execute(() -> {
                    final String deviceId = pillDfuPresenter.sleepPill.getValue().getName();
                    pillDfuPresenter.reset();
                    final Intent intent = new Intent();
                    intent.putExtra(PillUpdateActivity.EXTRA_DEVICE_ID, deviceId);
                    getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, intent);
                }), R.string.message_sleep_pill_updated);
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if (isUploading) {
            backPressedDialog.show();
        } else {
            skipPressedDialog.show();
        }
        return true;
    }


    @Override
    public void onDeviceConnecting(final String deviceAddress) {
        Log.d("DFU Listener", "onDeviceConnecting");
    }

    @Override
    public void onDeviceConnected(final String deviceAddress) {
        Log.d("DFU Listener", "onDeviceConnected");

    }

    @Override
    public void onDfuProcessStarting(final String deviceAddress) {
        Log.d("DFU Listener", "onDfuProcessStarting");

    }

    @Override
    public void onDfuProcessStarted(final String deviceAddress) {
        Log.d("DFU Listener", "onDfuProcessStarted");
        isUploading = true;

    }

    @Override
    public void onEnablingDfuMode(final String deviceAddress) {
        Log.d("DFU Listener", "onEnablingDfuMode");

    }

    @Override
    public void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
        Log.d("DFU Listener", "onProgressChanged " + percent + "%");
        updateIndicator.post(() -> updateIndicator.setProgress(percent));


    }

    @Override
    public void onFirmwareValidating(final String deviceAddress) {
        Log.d("DFU Listener", "onFirmwareValidating");

    }

    @Override
    public void onDeviceDisconnecting(final String deviceAddress) {
        Log.d("DFU Listener", "onDeviceDisconnecting");

    }

    @Override
    public void onDeviceDisconnected(final String deviceAddress) {
        Log.d("DFU Listener", "onDeviceDisconnected");

    }

    @Override
    public void onDfuCompleted(final String deviceAddress) {
        Log.d("DFU Listener", "onDfuCompleted");
        onFinish(true);
    }

    @Override
    public void onDfuAborted(final String deviceAddress) {
        Log.d("DFU Listener", "onDfuAborted");

    }

    @Override
    public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
        Log.d("DFU Listener", "onError: " + message + ". errorType: " + errorType + ". error: " + error);
        Analytics.trackEvent(Analytics.PillUpdate.Error.PILL_OTA_FAIL, null);
        presentError(new Throwable(message));
    }

}
