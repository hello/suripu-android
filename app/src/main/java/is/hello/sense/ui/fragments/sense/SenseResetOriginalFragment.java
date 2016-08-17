package is.hello.sense.ui.fragments.sense;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;

public class SenseResetOriginalFragment extends InjectionFragment{

    private OnboardingSimpleStepView view;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_sense_reset_original)
                .setSubheadingText(R.string.info_sense_reset_original_intro)
                .setToolbarOnHelpClickListener(this::onHelp)
                .setDiagramImage(R.drawable.onboarding_sense_intro)
                .setSecondaryButtonText(R.string.action_do_later)
                .setSecondaryOnClickListener(this::onDone)
                .setPrimaryButtonText(R.string.action_reset_sense)
                .setPrimaryOnClickListener(this::onNext)
        ;

        return view;
    }

    private void onHelp(final View ignored) {
        UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.RESET_ORIGINAL_SENSE);
    }

    private void onDone(final View ignored) {
        finishFlow();
    }

    private void onNext(final View ignored) {
        //todo make api call
        view.setSubheadingText(R.string.info_sense_reset_original_done)
            .setDiagramImage(R.drawable.onboarding_pair_sense)
            .setWantsSecondaryButton(false)
            .setPrimaryButtonText(R.string.action_done)
            .setPrimaryOnClickListener(this::onDone);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view.destroy();
        view = null;
    }
}
