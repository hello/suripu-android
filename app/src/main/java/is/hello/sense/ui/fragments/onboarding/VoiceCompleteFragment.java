package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.segment.analytics.Properties;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.util.Analytics;

public class VoiceCompleteFragment extends SenseFragment {

    private OnboardingSimpleStepView view;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final Properties properties = Analytics.createBluetoothTrackingProperties(getActivity());
            Analytics.trackEvent(Analytics.Onboarding.EVENT_END, properties);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        this.view =  new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.onboarding_voice_complete_title)
                .setSubheadingText(R.string.onboarding_voice_complete_message)
                .setDiagramImage(R.drawable.onboarding_sense_complete)
                .setPrimaryButtonText(R.string.action_done)
                .setPrimaryOnClickListener(this::complete)
                .setWantsSecondaryButton(false)
                .setToolbarWantsBackButton(false)
                .setToolbarWantsHelpButton(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.view.destroy();
        this.view = null;
    }

    public void complete(final View ignored) {
        finishFlow();
    }
}
