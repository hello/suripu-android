package is.hello.sense.interactors.pairsense;

import android.support.annotation.NonNull;

import java.util.Collections;

import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.sense.R;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.bluetooth.exceptions.SenseRequiredException;
import is.hello.sense.interactors.CurrentSenseInteractor;
import is.hello.sense.interactors.SwapSenseInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class UpgradePairSenseInteractor extends PairSenseInteractor{

    protected final SwapSenseInteractor swapSenseInteractor;
    protected final CurrentSenseInteractor resetOriginalInteractor;

    public UpgradePairSenseInteractor(@NonNull final HardwareInteractor hardwareInteractor,
                                      @NonNull final SwapSenseInteractor swapSenseInteractor,
                                      @NonNull final CurrentSenseInteractor resetOriginalInteractor) {

        super(hardwareInteractor);
        this.swapSenseInteractor = swapSenseInteractor;
        this.resetOriginalInteractor = resetOriginalInteractor;
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
    public boolean shouldContinueFlow() {
        return true;
    }

    @Override
    public boolean shouldClearPeripheral() {
        return true;
    }

    @Override
    public int getLinkedAccountErrorTitleRes() {
        return R.string.error_sense_pairing_failed;
    }

    @Override
    public String getOnFinishedAnalyticsEvent() {
        return Analytics.SenseUpgrade.EVENT_SENSE_PAIRED;
    }

    @Override
    public Observable<SensePeripheral> closestPeripheral() {
        hardwareInteractor.clearPeripheral();
        final SenseDevice currentSenseDevice = resetOriginalInteractor.getCurrentSense();
        if (currentSenseDevice != null) {
            return hardwareInteractor.closestPeripheral(
                    Collections.singleton(currentSenseDevice.deviceId));
        } else {
            return Observable.error(new SenseRequiredException());
        }
    }

    @Override
    public Observable<Void> linkAccount() {
        sendSwapAccountRequestEvent();
        swapSenseInteractor.setRequest(hardwareInteractor.getDeviceId());
        return swapSenseInteractor.canSwap()
                                  .doOnNext( status -> {
                                      logEvent("Swap Status: " + status);
                                      sendSwappedAccountsEvent();
                                  })
                                  .flatMap(okStatus -> hardwareInteractor.linkAccount());
    }

    protected void sendSwapAccountRequestEvent(){
        Analytics.trackEvent(Analytics.SenseUpgrade.EVENT_SWAP_ACCOUNTS_REQUEST, null);
    }

    protected void sendSwappedAccountsEvent(){
        Analytics.trackEvent(Analytics.SenseUpgrade.EVENT_SWAPPED_ACCOUNTS, null);
    }
}
