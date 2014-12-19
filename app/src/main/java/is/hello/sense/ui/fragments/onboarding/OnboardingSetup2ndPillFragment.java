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
import is.hello.sense.ui.widget.Views;

public class OnboardingSetup2ndPillFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_setup_2nd_pill, container, false);

        Button yesButton = (Button) view.findViewById(R.id.fragment_onboarding_setup_2nd_pill_yes);
        Views.setSafeOnClickListener(yesButton, ignored -> ((OnboardingActivity) getActivity()).show2ndPillPairing());

        Button noButton = (Button) view.findViewById(R.id.fragment_onboarding_setup_2nd_pill_no);
        Views.setSafeOnClickListener(noButton, ignored -> ((OnboardingActivity) getActivity()).showSenseColorsInfo());

        return view;
    }
}
