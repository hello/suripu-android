package is.hello.sense.presenters;

import is.hello.sense.R;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.util.Analytics;

public class SettingsPairSensePresenter extends BasePairSensePresenter {

    public SettingsPairSensePresenter(final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
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
        return Analytics.Onboarding.EVENT_PAIR_SENSE_IN_APP;
    }

    @Override
    public String getOnFinishAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_SENSE_PAIRED_IN_APP;
    }

    @Override
    protected boolean shouldFinishFlow() {
        return true;
    }

    @Override
    protected boolean shouldClearPeripheral() {
        return false;
    }

    @Override
    public boolean shouldShowPairDialog() {
        return false;
    }
}
