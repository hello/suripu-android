package is.hello.sense.presenters;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.UpgradePairSenseInteractor;
import is.hello.sense.util.Analytics;

public class UpgradePairSensePresenter extends PairSensePresenter {

    public UpgradePairSensePresenter(final HardwareInteractor hardwareInteractor,
                                     final UserFeaturesInteractor userFeaturesInteractor,
                                     final ApiService apiService,
                                     final UpgradePairSenseInteractor upgradePairSenseInteractor) {
        super(hardwareInteractor,
              userFeaturesInteractor,
              apiService,
              upgradePairSenseInteractor);

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
    public String getAnalyticsHelpEvent() {
        return Analytics.SenseUpgrade.EVENT_PAIRING_MODE_HELP;
    }

    @Override
    public boolean shouldShowPairDialog() {
        return true;
    }

    @Override
    public boolean showSupportOptions() {
        return false;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.SenseUpgrade.EVENT_SENSE_PAIR;
    }

}
