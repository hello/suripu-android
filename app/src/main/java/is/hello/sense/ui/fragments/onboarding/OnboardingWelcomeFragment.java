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

public class OnboardingWelcomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_welcome, container, false);

        Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_welcome_skip);
        skipButton.setOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showHomeActivity());

        return view;
    }
}
