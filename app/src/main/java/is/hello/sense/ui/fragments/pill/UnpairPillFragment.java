package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.BaseHardwareFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class UnpairPillFragment extends BaseHardwareFragment {
    private final static int ONE_SECOND_DELAY = 1000;

    private OnboardingSimpleStepView view = null;

    @Inject
    DevicesPresenter devicesPresenter;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.unpair_pill_title)
                .setSubheadingText(R.string.unpair_pill_message)
                .setPrimaryButtonText(R.string.action_pair_new_sleep_pill)
                .setSecondaryButtonText(R.string.action_do_later)
                .setToolbarWantsHelpButton(true)
                .setPrimaryOnClickListener(this::onPrimaryClick)
                .setSecondaryOnClickListener(this::onSecondaryClick)
                .setToolbarOnHelpClickListener(this::onHelpClick)
                .setDiagramImage(R.drawable.onboarding_sleep_pill);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(devicesPresenter.devices,
                         this::bindDevices,
                         this::presentError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (view != null) {
            view.destroy();
            view = null;
        }
    }

    private void onPrimaryClick(@NonNull final View view) {
        showBlockingActivity(R.string.unpairing_sleep_pill);
        devicesPresenter.update();
    }

    private void onSecondaryClick(@NonNull final View view) {
        final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setTitle(R.string.unpair_pill_dialog_title);
        dialog.setMessage(R.string.unpair_pill_dialog_message);
        dialog.setPositiveButton(R.string.action_pair_new_pill, (dialogInterface, i) -> {
            onPrimaryClick(view);
        });
        dialog.setNegativeButton(R.string.action_dont_pair, (dialogInterface, i) -> {
            finishFlow();
        });
        dialog.show();
    }

    private void onHelpClick(@NonNull final View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIRING_MODE_HELP, null);
        UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.PAIRING_MODE);
    }

    private void bindDevices(@NonNull final Devices devices) {
        if (view == null) {
            return;
        }
        view.postDelayed(() -> {
            final SleepPillDevice sleepPillDevice = devices.getSleepPill();
            if (sleepPillDevice == null) { // account doesn't have a pill.
                finishWithSuccess();
                return;
            }
            showBlockingActivity(R.string.unpairing_sleep_pill);
            bindAndSubscribe(devicesPresenter.unregisterDevice(sleepPillDevice),
                             this::bindUnregisterDevice,
                             this::presentError);
        }, ONE_SECOND_DELAY);

    }

    private void bindUnregisterDevice(@NonNull final VoidResponse vr) {
        finishWithSuccess();
    }

    private void presentError(@NonNull final Throwable e) {
        hideBlockingActivityWithDelay(
                () -> ErrorDialogFragment.presentError(getActivity(), e));
    }

    private void finishWithSuccess() {
        devicesPresenter.devices.forget();
        hideBlockingActivity(R.string.unpaired, () -> finishFlowWithResult(Activity.RESULT_OK));
    }

    private void hideBlockingActivityWithDelay(@NonNull final Runnable runnable) {
        if (view == null) {
            return;
        }
        view.postDelayed(() -> hideBlockingActivity(false, runnable), ONE_SECOND_DELAY);
    }
}
