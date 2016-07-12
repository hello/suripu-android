package is.hello.sense.ui.fragments.onboarding;

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
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;

public class UpdateReadyPillFragment extends HardwareFragment
implements OnBackPressedInterceptor {
    private ProgressBar updateIndicator;
    private TextView activityStatus;
    private DiagramVideoView diagram;
    private Button retryButton;

    private boolean isUpdating = false;
    private SenseAlertDialog backPressedDialog;


    public static Fragment newInstance() {
        return new UpdateReadyPillFragment();
    }

    public UpdateReadyPillFragment(){
        //required empty public constructor
        super();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);
        final ProgressBar unusedProgressBar = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_activity);
        unusedProgressBar.setVisibility(View.GONE);
        this.updateIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_progress_determinate);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);
        this.diagram = (DiagramVideoView) view.findViewById(R.id.fragment_onboarding_pair_pill_diagram);
        final TextView titleTextView = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_title);
        final TextView infoTextView = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_subhead);
        //skipping this is not an option currently so we don't keep reference to skip button
        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_retry);

        updateIndicator.setVisibility(View.VISIBLE);
        activityStatus.setText(R.string.message_sleep_pill_updating);

        titleTextView.setText(R.string.title_update_sleep_pill);
        infoTextView.setText(R.string.info_update_sleep_pill);

        diagram.destroy();
        diagram.setPlaceholder(R.drawable.sleep_pill_ota);
        diagram.invalidate();

        Views.setSafeOnClickListener(retryButton, ignored -> updatePill());

        OnboardingToolbar.of(this, view)
                         .setWantsBackButton(false)
                         .setOnHelpClickListener(this::help);

        if (BuildConfig.DEBUG) {
            diagram.setOnLongClickListener(ignored -> {
                skipUpdatingPill();
                return true;
            });
            diagram.setBackgroundResource(R.drawable.selectable_dark);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isUpdating) {
            updatePill();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diagram != null) {
            diagram.destroy();
        }

        this.activityStatus = null;
        this.diagram = null;
        this.retryButton.setOnClickListener(null);
        this.retryButton = null;
        this.backPressedDialog = null;
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
                errorDialogBuilder.withUnstableBluetoothHelp(getActivity());
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
            LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(), () -> {
                stateSafeExecutor.execute(() -> {
                    hardwarePresenter.clearPeripheral();
                    if (success) {
                        ((FragmentNavigation) getActivity()).flowFinished(this, PillUpdateActivity.FLOW_FINISHED, null);
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
