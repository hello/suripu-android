package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;

public class OnboardingRegisterAudioFragment extends InjectionFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepViewBuilder(this, inflater, container)
                .setHeadingText(R.string.onboarding_title_enhanced_audio)
                .setSubheadingText(R.string.onboarding_info_enhanced_audio)
                .setDiagramImage(R.drawable.onboarding_enhanced_audio)
                .setPrimaryButtonText(R.string.action_enable_enhanced_audio)
                .setPrimaryOnClickListener(this::optIn)
                .setSecondaryOnClickListener(this::optOut)
                .setToolbarWantsBackButton(false)
                .setToolbarOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.ENHANCED_AUDIO))
                .create();
    }


    public void optIn(@NonNull View sender) {
        // TODO: Opt in logic
        ((OnboardingActivity) getActivity()).showSetupSense();
    }

    public void optOut(@NonNull View sender) {
        ((OnboardingActivity) getActivity()).showSetupSense();
    }
}
