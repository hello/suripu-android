package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.util.Analytics;

public class OnboardingGettingStartedFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.event(Analytics.EVENT_ONBOARDING_SETUP_START, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_getting_started, container, false);

        Button onePill = (Button) view.findViewById(R.id.fragment_onboarding_getting_started_one_pill);
        onePill.setOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showSetupSense(false));

        Button twoPills = (Button) view.findViewById(R.id.fragment_onboarding_getting_started_two_pills);
        twoPills.setOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showWhichPill());

        return view;
    }
}
