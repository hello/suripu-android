package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;

public class OnboardingTaskFragment extends Fragment {
    private static final String ARG_TITLE_RES_ID = OnboardingTaskFragment.class.getName() + ".ARG_TITLE_RES_ID";

    public static OnboardingTaskFragment newInstance(@StringRes int titleResId) {
        OnboardingTaskFragment fragment = new OnboardingTaskFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(ARG_TITLE_RES_ID, titleResId);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public @Nullable View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_task, container, false);

        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.fragment_onboarding_task_progress_bar);

        TextView title = (TextView) view.findViewById(R.id.fragment_onboarding_task_title);
        title.setText(getArguments().getInt(ARG_TITLE_RES_ID));

        return view;
    }
}
