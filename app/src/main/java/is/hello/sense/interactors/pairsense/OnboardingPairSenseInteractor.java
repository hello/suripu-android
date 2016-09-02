package is.hello.sense.interactors.pairsense;

import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.util.Analytics;

public class OnboardingPairSenseInteractor extends PairSenseInteractor {

    public OnboardingPairSenseInteractor(@NonNull final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor);
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
    public boolean shouldContinueFlow() {
        return true;
    }

    @Override
    public boolean shouldClearPeripheral() {
        return true;
    }

    @Override
    public int getLinkedAccountErrorTitleRes() {
        return R.string.error_account_not_linked;
    }

    @Override
    public String getOnFinishedAnalyticsEvent() {
        return Analytics.Onboarding.EVENT_SENSE_PAIRED;
    }
}
