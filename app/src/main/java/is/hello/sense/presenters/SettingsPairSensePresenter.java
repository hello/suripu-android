package is.hello.sense.presenters;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.util.Analytics;

public class SettingsPairSensePresenter extends PairSensePresenter {

    public SettingsPairSensePresenter(final HardwareInteractor hardwareInteractor,
                                      final UserFeaturesInteractor userFeaturesInteractor,
                                      final ApiService apiService) {
        super(hardwareInteractor,
              userFeaturesInteractor,
              apiService);
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
        return R.string.title_connecting_with_sense;
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
    protected boolean shouldContinueFlow() {
        return false;
    }

    @Override
    protected boolean shouldClearPeripheral() {
        return false;
    }

    @Override
    public boolean shouldShowPairDialog() {
        return BuildConfig.DEBUG;
    }

    @Override
    public boolean showSupportOptions() {
        return false;
    }
}
