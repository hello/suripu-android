package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.DevicePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class OnboardingPairPillFragment extends InjectionFragment {
    private static final String ARG_COLOR_INDEX = OnboardingPairPillFragment.class.getName() + ".ARG_COLOR_INDEX";

    @Inject DevicePresenter devicePresenter;

    public static OnboardingPairPillFragment newInstance(int colorIndex) {
        OnboardingPairPillFragment fragment = new OnboardingPairPillFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_COLOR_INDEX, colorIndex);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        nextButton.setOnClickListener(this::next);

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(ignored -> finishedPairing());

        return view;
    }


    private void beginPairing() {
        ((OnboardingActivity) getActivity()).beginBlockingWork(R.string.title_pairing);
    }

    private void finishedPairing() {
        devicePresenter.clearDevice();

        OnboardingActivity activity = (OnboardingActivity) getActivity();
        activity.finishBlockingWork();
        activity.showWelcome();
    }


    public void next(@NonNull View sender) {
        beginPairing();

        bindAndSubscribe(devicePresenter.linkPill(), ignored -> finishedPairing(), e -> {
            OnboardingActivity onboardingActivity = (OnboardingActivity) getActivity();
            if (onboardingActivity != null) {
                onboardingActivity.finishBlockingWork();
                ErrorDialogFragment.presentError(getFragmentManager(), e);
            }
        });
    }
}
