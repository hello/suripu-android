package is.hello.sense.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.UpgradePairSenseInteractor;
import is.hello.sense.util.Analytics;

public class UpgradePairSensePresenter extends PairSensePresenter {

    public UpgradePairSensePresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                     @NonNull final UserFeaturesInteractor userFeaturesInteractor,
                                     @NonNull final ApiService apiService,
                                     @NonNull final UpgradePairSenseInteractor upgradePairSenseInteractor,
                                     @NonNull final PreferencesInteractor preferencesInteractor) {
        super(hardwareInteractor,
              userFeaturesInteractor,
              apiService,
              upgradePairSenseInteractor,
              preferencesInteractor);

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
        return Analytics.Upgrade.EVENT_PAIRING_MODE_HELP;
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
    protected boolean shouldUseDefaultBackPressedBehavior() {
        return false;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Upgrade.EVENT_PAIR_SENSE;
    }

}
