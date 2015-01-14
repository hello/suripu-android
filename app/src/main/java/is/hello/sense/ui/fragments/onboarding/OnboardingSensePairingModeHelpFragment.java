package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.util.Views;

public class OnboardingSensePairingModeHelpFragment extends SenseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sense_pairing_mode_help, container, false);

        Button continueButton = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        Views.setSafeOnClickListener(continueButton, ignored -> getFragmentManager().popBackStack());

        return view;
    }
}
