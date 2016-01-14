package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;

public class OnboardingSmartAlarmFragment extends SenseFragment {
    private static final int EDIT_REQUEST_CODE = 0x31;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_FIRST_ALARM, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.onboarding_title_smart_alarm)
                .setSubheadingText(R.string.onboarding_info_smart_alarm)
                .setDiagramVideo(Uri.parse(getString(R.string.diagram_onboarding_smart_alarm)))
                .setDiagramImage(R.drawable.onboarding_smart_alarm)
                .setPrimaryButtonText(R.string.action_set_smart_alarm_now)
                .setPrimaryOnClickListener(this::createNewAlarm)
                .setSecondaryButtonText(R.string.action_do_later)
                .setSecondaryOnClickListener(ignored -> complete())
                .hideToolbar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            complete();
        }
    }


    public void createNewAlarm(@NonNull View sender) {
        Intent newAlarm = new Intent(getActivity(), SmartAlarmDetailActivity.class);
        startActivityForResult(newAlarm, EDIT_REQUEST_CODE);
    }

    public void complete() {
        LoadingDialogFragment.close(getFragmentManager());
        ((OnboardingActivity) getActivity()).showDone();
    }
}
