package is.hello.sense.presenters;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.util.Analytics;

public class OnboardingPairSensePresenter extends BasePairSensePresenter {

    public OnboardingPairSensePresenter(final HardwarePresenter hardwarePresenter) {
        super(hardwarePresenter);
    }

    @Override
    public int getTitleRes() {
        return R.string.title_pair_sense;
    }

    @Override
    public int getSubtitleRes() {
        return R.string.info_pair_sense;
    }

    @Override
    public int getPairingRes() {
        return R.string.title_connecting;
    }

    @Override
    public int getFinishedRes() {
        return R.string.action_done;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_PAIR_SENSE;
    }

    @Override
    public String getOnFinishAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_SENSE_PAIRED;
    }

    @Override
    public boolean shouldShowPairDialog() {
        return BuildConfig.DEBUG;
    }
}
