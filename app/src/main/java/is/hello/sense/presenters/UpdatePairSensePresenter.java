package is.hello.sense.presenters;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.util.Analytics;

public class UpdatePairSensePresenter extends BasePairSensePresenter {

    public UpdatePairSensePresenter(final HardwarePresenter hardwarePresenter) {
        super(hardwarePresenter);
    }

    @Override
    public int getTitleRes() {
        return R.string.title_pair_new_sense;
    }

    @Override
    public int getSubtitleRes() {
        return R.string.info_pair_new_sense;
    }

    @Override
    public int getPairingRes() {
        return R.string.title_pairing_with_sense;
    }

    @Override
    public int getFinishedRes() {
        return R.string.title_paired;
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
    public boolean shouldShowPairDialog() {
        return true;
    }
}
