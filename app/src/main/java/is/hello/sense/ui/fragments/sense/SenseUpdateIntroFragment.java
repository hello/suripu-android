package is.hello.sense.ui.fragments.sense;

import android.app.Activity;
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

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_upgrade_sense_voice)
                .setSubheadingText(R.string.info_upgrade_sense_voice)
                .setDiagramImage(R.drawable.sense_upgrade_intro)
                .setDiagramEdgeToEdge(false)
                .setPrimaryButtonText(R.string.action_set_up)
                .setPrimaryOnClickListener(ignored -> next())
                .setSecondaryButtonText(R.string.action_upgrade_sense_voice_help)
                .setSecondaryOnClickListener(this::showSenseVoiceHelp)
                .setToolbarWantsBackButton(true);
    }

    private void showSenseVoiceHelp(final View view) {
        //todo replace with proper uri
        //Analytics.trackEvent(Analytics.Onboarding.EVENT_NO_SENSE, null);
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.ORDER_URL));
    }

    private void next() {
        getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null);
    }
}
