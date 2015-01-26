package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.util.Analytics;

public class OnboardingSetup2ndPillFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_ANOTHER_PILL, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepViewBuilder(this, inflater, container)
                .setHeadingText(R.string.title_onboarding_setup_2nd_pill)
                .setSubheadingText(R.string.info_onboarding_setup_2nd_pill)
                .setDiagramImage(R.drawable.onboarding_partner_clip)
                .setSecondaryButtonText(R.string.action_setup_another_pill)
                .setSecondaryOnClickListener(ignored -> ((OnboardingActivity) getActivity()).show2ndPillPairing())
                .setPrimaryButtonText(R.string.action_no_thanks)
                .setPrimaryOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showDone())
                .hideToolbar()
                .create();
    }
}
