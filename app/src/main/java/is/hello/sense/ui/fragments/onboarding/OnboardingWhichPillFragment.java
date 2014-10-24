package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.util.Analytics;

public class OnboardingWhichPillFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.event(Analytics.EVENT_ONBOARDING_SETUP_TWO_PILL, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_which_pill, container, false);

        Button firstPill = (Button) view.findViewById(R.id.fragment_onboarding_which_pill_first);
        firstPill.setOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showSetupSense(false));

        Button secondPill = (Button) view.findViewById(R.id.fragment_onboarding_which_pill_second);
        secondPill.setOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showSetupSense(true));

        Button help = (Button) view.findViewById(R.id.fragment_onboarding_which_pill_help);
        help.setOnClickListener(ignored -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&noredirect=1"))));

        return view;
    }
}
