package is.hello.sense.presenters;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import com.segment.analytics.Properties;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class OnboardingPairPillPresenter extends BasePairPillPresenter {

    @Override
    public void trackOnCreate() {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_PILL, null);
    }

    @Override
    public int getTitleRes() {
        return R.string.info_pair_pill;
    }

    @Override
    public int getSubTitleRes() {
        return R.string.info_pair_pill;
    }

    @Override
    public boolean showSkipButtonOnError() {
        return true;
    }

    @Override
    public boolean wantsBackButton() {
        return false;
    }

    @Override
    public void finishedPairingAction(@NonNull final Activity activity, final boolean success) {
        if (!(activity instanceof OnboardingActivity)) {
            throw new Error("Wrong activity using onboarding activity"); // todo change this as we update activities.
        }
        final OnboardingActivity onboardingActivity = (OnboardingActivity) activity;
        hardwareInteractor.clearPeripheral();
        if (success) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PILL_PAIRED, null);
            onboardingActivity.showPillInstructions();
        } else {
            onboardingActivity.showSenseColorsInfo();
        }

    }

    @Override
    public void trackOnSkip() {
        final Properties properties =
                Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "pill_pairing");
        Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, properties);

    }
}
