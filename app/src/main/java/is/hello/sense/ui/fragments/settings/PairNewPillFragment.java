package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;

public class PairNewPillFragment extends SenseFragment {
    private OnboardingSimpleStepView view = null;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.sense_voice_new_pill_title)
                .setSubheadingText(R.string.sense_voice_new_pill_message)
                .setPrimaryButtonText(R.string.action_continue)
                .setPrimaryOnClickListener(view1 -> {
                    // todo in next branch
                })
                .setWantsSecondaryButton(false)
                .setDiagramImage(R.drawable.sense_voice_table);
        return this.view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (view != null) {
            view.destroy();
            view = null;
        }
    }
}
