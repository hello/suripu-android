package is.hello.sense.ui.fragments.pill;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;
import is.hello.sense.util.Analytics;

public class UpdatePairPillConfirmationFragment extends SenseFragment implements OnBackPressedInterceptor {
    private OnboardingSimpleStepView view;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.update_pair_pill_confirmation_title)
                .setSubheadingText(R.string.update_pair_pill_confirmation_message)
                .setPrimaryButtonText(R.string.action_continue)
                .setWantsSecondaryButton(false)
                .setToolbarWantsHelpButton(true)
                .setPrimaryOnClickListener(this::onPrimaryClick)
                .setToolbarOnHelpClickListener(this::onHelpClick)
                .setDiagramVideo(Uri.parse(getString(R.string.diagram_onboarding_clip_pill)))
                .setDiagramImage(R.drawable.vid_sleep_pill_attach);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (view != null) {
            view.destroy();
            view = null;
        }
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        return true;
    }

    private void onPrimaryClick(@NonNull final View view) {
        finishFlow();
    }

    private void onHelpClick(@NonNull final View sender) {
        Analytics.trackEvent(Analytics.General.EVENT_HELP, null);
        UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.PILL_PLACEMENT);
    }
}
