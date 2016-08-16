package is.hello.sense.ui.fragments.sense;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;

public class SenseUpdateIntroFragment extends SenseFragment{

    private OnboardingSimpleStepView view;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_upgrade_sense_voice)
                .setSubheadingText(R.string.info_upgrade_sense_voice)
                .setDiagramImage(R.drawable.onboarding_sense_intro)
                .setPrimaryButtonText(R.string.action_set_up)
                .setPrimaryOnClickListener(ignored -> finishFlow())
                .setSecondaryButtonText(R.string.action_upgrade_sense_voice_help)
                .setSecondaryOnClickListener(this::showSenseVoiceHelp)
                .setToolbarWantsBackButton(true);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view.destroy();
        view = null;
    }

    private void showSenseVoiceHelp(final View view) {
        //todo replace with proper uri
        //Analytics.trackEvent(Analytics.Onboarding.EVENT_NO_SENSE, null);
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.ORDER_URL));
    }
}
