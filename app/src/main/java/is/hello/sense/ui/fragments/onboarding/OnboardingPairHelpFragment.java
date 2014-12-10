package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.SenseFragment;

public class OnboardingPairHelpFragment extends SenseFragment {
    public static final int RESULT_NEW_SENSE = 0x333;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_help, container, false);

        Button secondPill = (Button) view.findViewById(R.id.fragment_onboarding_pair_help_2nd_pill);
        secondPill.setOnClickListener(this::setupSecondPill);

        Button newSense = (Button) view.findViewById(R.id.fragment_onboarding_pair_help_new_sense);
        newSense.setOnClickListener(this::setupNewSense);

        return view;
    }


    public void setupSecondPill(@NonNull View sender) {
        FragmentNavigation navigation = (FragmentNavigation) getActivity();
        finishWithResult(Activity.RESULT_OK, null);
        navigation.showFragment(new OnboardingPillPartnerInfoFragment(), null, true);
    }

    public void setupNewSense(@NonNull View sender) {
        finishWithResult(RESULT_NEW_SENSE, null);
    }
}
