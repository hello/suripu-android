package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.notifications.NotificationRegistrationBroadcastReceiver;
import is.hello.sense.ui.common.SenseFragment;

/**
 * Introduce user to allow receiving push notifications
 */

public class EnableNotificationFragment extends SenseFragment {

    OnboardingSimpleStepView view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.onboarding_title_enable_notification)
                .setSubheadingText(R.string.onboarding_subtitle_enable_notification)
                .setDiagramImage(R.drawable.diagram_onboarding_notification)
                .setPrimaryButtonText(R.string.action_allow_notifications)
                .setSecondaryButtonText(R.string.action_do_later)
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
        finishFlow();
    }

    private void onNext(final View ignored) {
        LocalBroadcastManager.getInstance(getActivity())
                             .sendBroadcast(NotificationRegistrationBroadcastReceiver.getIntent(null));
        finishFlow();
    }
}
