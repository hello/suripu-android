package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterAudioFragment extends InjectionFragment {
    @Inject ApiService apiService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_AUDIO, null);
        }
    }

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
        updateEnhancedAudioEnabled(true);
    }

    public void optOut(@NonNull View sender) {
        updateEnhancedAudioEnabled(false);
    }

    private void updateEnhancedAudioEnabled(boolean enabled) {
        LoadingDialogFragment.show(getFragmentManager());

        Map<Account.Preference, Boolean> update = Account.Preference.ENHANCED_AUDIO.toUpdate(enabled);
        bindAndSubscribe(apiService.updateAccountPreferences(update),
                         ignored -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ((OnboardingActivity) getActivity()).showSetupSense();
                         },
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ErrorDialogFragment.presentError(getFragmentManager(), e);
                         });
    }
}
