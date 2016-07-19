package is.hello.sense.ui.fragments.pill;

import android.app.Fragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
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
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;

public class UpdateReadyPillFragment extends HardwareFragment
implements OnBackPressedInterceptor {
    private ProgressBar updateIndicator;
    private TextView activityStatus;
    private Button retryButton;
    private Button skipButton;
    private final ViewAnimator viewAnimator = new ViewAnimator();

    private boolean isUpdating = false;
    private SenseAlertDialog backPressedDialog;
    private OnboardingToolbar toolbar;


    public static Fragment newInstance() {
        return new UpdateReadyPillFragment();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_pill_update, container, false);
        this.updateIndicator = (ProgressBar) view.findViewById(R.id.fragment_update_pill_progress_determinate);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_update_pill_status);
        final TextView titleTextView = (TextView) view.findViewById(R.id.fragment_update_pill_title);
        final TextView infoTextView = (TextView) view.findViewById(R.id.fragment_update_pill_subhead);
        final View animatedView = view.findViewById(R.id.blue_box_view);

        this.retryButton = (Button) view.findViewById(R.id.fragment_update_pill_retry);
        this.skipButton = (Button) view.findViewById(R.id.fragment_update_pill_skip);

        activityStatus.setText(R.string.message_sleep_pill_updating);

        titleTextView.setText(R.string.title_update_sleep_pill);
        infoTextView.setText(R.string.info_update_sleep_pill);

        Views.setTimeOffsetOnClickListener(retryButton, ignored -> updatePill());
        Views.setTimeOffsetOnClickListener(skipButton, ignored -> skipUpdatingPill());

        viewAnimator.setAnimatedView(animatedView);

        this.toolbar = OnboardingToolbar.of(this, view)
                         .setWantsBackButton(false)
                         .setOnHelpClickListener(this::help);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewAnimator.onViewCreated(getActivity(), R.animator.bluetooth_sleep_pill_ota_animator);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isUpdating) {
            updatePill();
        }
        viewAnimator.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewAnimator.onPause();
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

    public void updatePill() {
        onBegin();

        hideBlockingActivity(false, () -> {
            onUpdating();
            //Todo replace with updatePill to dfu mode
            /*bindAndSubscribe(hardwarePresenter.linkPill(),
                                              ignored -> completeHardwareActivity(() -> onFinish(true)),
                                                  this::presentError);*/
        });

    }

    public void presentError(final Throwable e) {
        this.isUpdating = false;

        hideAllActivityForFailure(() -> {
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
        });
    }

    private void skipUpdatingPill() {
        onFinish(false);
    }

    private void onBegin() {
        this.isUpdating = true;

        updateIndicator.setVisibility(View.VISIBLE);
        activityStatus.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
    }

    private void onUpdating(){
        stateSafeExecutor.execute(() -> {

            final NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            final int notificationId = 1;
            final String notificationTag = UpdateReadyPillFragment.class.getName() + ".NOTIFICATION_TAG";
            final String onCompleteTitle = getString(R.string.notification_update_successful);
            final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getActivity());
            notificationBuilder.setContentTitle(getString(R.string.notification_title_update_sleep_pill));
            notificationBuilder.setContentText(getString(R.string.notification_update_progress));
            notificationBuilder.setColor(ContextCompat.getColor(getActivity(), R.color.primary));
            notificationBuilder.setSmallIcon(R.drawable.pill_icon);

            final Intent intent = new Intent(getActivity(), PillUpdateActivity.class);
            final PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(),0,intent, PendingIntent.FLAG_UPDATE_CURRENT);

            notificationBuilder.setContentIntent(pendingIntent);
            //Todo replace mock of progress with real hardware pill presenter work update
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (updateIndicator == null) {
                        this.cancel();
                    }
                    int progress = updateIndicator.getProgress();
                    if (progress < updateIndicator.getMax()) {
                        updateIndicator.setProgress(++progress);
                        notificationBuilder.setProgress(updateIndicator.getMax(), progress, false);
                        notificationManager.notify(notificationTag, notificationId, notificationBuilder.build());
                    } else {
                        this.cancel();
                        notificationBuilder.setProgress(0, 0, false);
                        notificationBuilder.setContentText(onCompleteTitle);
                        notificationBuilder.setAutoCancel(true);
                        notificationManager.notify(notificationTag, notificationId, notificationBuilder.build());

                        stateSafeExecutor.execute(() -> {
                            notificationManager.cancel(notificationTag, notificationId);
                        });

                        updateIndicator.post(() -> {
                            UpdateReadyPillFragment.this.onFinish(true);
                        });
                    }
                }
            }, 1000, 200);

        });
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
                hardwarePresenter.clearPeripheral();
                final String deviceId = "BF39B2A810B9813D"; //todo fix hardcoded
                final Intent intent = new Intent();
                intent.putExtra(PillUpdateActivity.EXTRA_DEVICE_ID, deviceId);
                ((FragmentNavigation) getActivity()).flowFinished(this, PillUpdateActivity.FLOW_FINISHED, intent);
            }, R.string.message_sleep_pill_updated);
        });
    }

    private void help(final View view) {
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATE_PILL);
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if(this.backPressedDialog == null) {
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
}
