package is.hello.sense.ui.fragments.sense;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;

public class SenseUpdateReadyFragment extends SenseFragment{

    private OnboardingSimpleStepView view;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view =  new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_upgrade_ready_sense_voice)
                .setSubheadingText(R.string.info_upgrade_ready_sense_voice)
                .setDiagramImage(R.drawable.sense_upgrade_ready)
                .setPrimaryButtonText(R.string.action_continue)
                .setPrimaryOnClickListener(ignored -> finishFlow())
                .setWantsSecondaryButton(false)
                .setToolbarWantsBackButton(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view.destroy();
        view = null;
    }
}