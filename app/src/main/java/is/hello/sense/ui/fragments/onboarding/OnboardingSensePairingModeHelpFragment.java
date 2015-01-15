package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;

public class OnboardingSensePairingModeHelpFragment extends SenseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepViewBuilder(this, inflater, container)
                .setHeadingText(R.string.title_sense_pairing_mode_help)
                .setSubheadingText(R.string.info_sense_pairing_mode_help)
                .setPrimaryOnClickListener(ignored -> getFragmentManager().popBackStack())
                .setWantsSecondaryButton(false)
                .hideToolbar()
                .create();
    }
}
