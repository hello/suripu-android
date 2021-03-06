package is.hello.sense.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.pairsense.PairSenseInteractor;
import is.hello.sense.util.Analytics;

public class OnboardingPairSensePresenter extends PairSensePresenter {

    public OnboardingPairSensePresenter(@NonNull final HardwareInteractor hardwareInteractor,
                                        @NonNull final DevicesInteractor devicesInteractor,
                                        @NonNull final ApiService apiService,
                                        @NonNull final PairSenseInteractor pairSenseInteractor,
                                        @NonNull final PreferencesInteractor preferencesInteractor) {
        super(hardwareInteractor,
              devicesInteractor,
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
        return Analytics.Onboarding.EVENT_PAIRING_MODE_HELP;
    }

    @Override
    public String getOnCreateAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_PAIR_SENSE;
    }

    @Override
    public boolean shouldShowPairDialog() {
        return BuildConfig.DEBUG;
    }

    @Override
    public boolean showSupportOptions() {
        return true;
    }

    @Override
    protected boolean shouldUseDefaultBackPressedBehavior() {
        return true;
    }
}
