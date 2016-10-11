package is.hello.sense.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.util.Analytics;

public class SettingsPairSensePresenter extends PairSensePresenter {

    public SettingsPairSensePresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                      @NonNull final UserFeaturesInteractor userFeaturesInteractor,
                                      @NonNull final ApiService apiService,
                                      final PairSenseInteractor pairSenseInteractor,
                                      @NonNull final PreferencesInteractor preferencesInteractor) {
        super(hardwareInteractor,
              userFeaturesInteractor,
              apiService,
              pairSenseInteractor,
              preferencesInteractor);
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
    public String getAnalyticsHelpEvent() {
        return Analytics.Settings.EVENT_PAIRING_MODE_HELP;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Settings.EVENT_PAIR_SENSE;
    }

    @Override
    public boolean shouldShowPairDialog() {
        return BuildConfig.DEBUG;
    }

    @Override
    public boolean showSupportOptions() {
        return false;
    }

    @Override
    protected boolean shouldUseDefaultBackPressedBehavior() {
        return true;
    }
}
