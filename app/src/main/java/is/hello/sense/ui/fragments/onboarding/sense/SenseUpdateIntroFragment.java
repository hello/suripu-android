package is.hello.sense.ui.fragments.onboarding.sense;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;
import is.hello.sense.util.Analytics;

public class SenseUpdateIntroFragment extends SenseFragment {

    private OnboardingSimpleStepView view;

    public static SenseUpdateIntroFragment newInstance() {
        return new SenseUpdateIntroFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.SenseUpdate.EVENT_ENTER, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view =  new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_sense_update)
                .setSubheadingText(R.string.subtitle_sense_update)
                .setDiagramImage(R.drawable.onboarding_sense_ota)
                .setPrimaryButtonText(R.string.action_continue)
                .setPrimaryOnClickListener(this::onContinue)
                .setWantsSecondaryButton(false)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(
                        ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATING_SENSE));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view.destroy();
        view = null;
    }

    public void onContinue(final View ignored) {
        finishFlow();
    }
}
