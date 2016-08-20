package is.hello.sense.presenters;


import android.app.Activity;
import android.support.annotation.NonNull;

import com.segment.analytics.Properties;

import is.hello.sense.R;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;

public class ChangePairPillPresenter extends BasePairPillPresenter {

    @Override
    public void trackOnCreate() {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_PILL_IN_APP, null);
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
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PILL_PAIRED_IN_APP, null);
        activity.finish();

    }

    @Override
    public void trackOnSkip() {
        final Properties properties =
                Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "pill_pairing");
        Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP_IN_APP, properties);

    }

}
