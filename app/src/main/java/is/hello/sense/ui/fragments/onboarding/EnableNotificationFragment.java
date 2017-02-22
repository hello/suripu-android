package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.flows.notification.interactors.NotificationSettingsInteractor;
import is.hello.sense.notifications.NotificationRegistrationBroadcastReceiver;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

/**
 * Introduce user to allow receiving push notifications
 */

public class EnableNotificationFragment extends InjectionFragment {

    @Inject
    NotificationSettingsInteractor notificationSettingsInteractor;

    OnboardingSimpleStepView view;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notificationSettingsInteractor.update();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.onboarding_title_enable_notification)
                .setSubheadingText(R.string.onboarding_subtitle_enable_notification)
                .setDiagramImage(R.drawable.diagram_onboarding_notification)
                .setPrimaryButtonText(R.string.action_allow_notifications)
                .setSecondaryButtonText(R.string.action_skip)
                .setSecondaryOnClickListener(this::onSkip)
                .setPrimaryOnClickListener(this::onNext);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(view != null) {
            view.destroy();
            view = null;
        }
    }

    private void onSkip(final View ignored) {
        notificationSettingsInteractor.updateIfInvalid();
        bindAndSubscribe(notificationSettingsInteractor.disableAll(),
                         voidResponse -> this.onFinish(),
                         e -> ErrorDialogFragment.presentError(getActivity(), e));
    }

    private void onNext(final View ignored) {
        notificationSettingsInteractor.updateIfInvalid();
        bindAndSubscribe(notificationSettingsInteractor.enableAll(),
                         voidResponse -> this.onFinish(),
                         e -> ErrorDialogFragment.presentError(getActivity(), e));
    }

    private void onFinish() {
        LocalBroadcastManager.getInstance(getActivity())
                             .sendBroadcast(NotificationRegistrationBroadcastReceiver.getRegisterIntent(null));
        finishFlow();
    }
}
