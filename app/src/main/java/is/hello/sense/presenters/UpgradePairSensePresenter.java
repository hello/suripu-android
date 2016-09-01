package is.hello.sense.presenters;

import java.util.Collections;

import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.bluetooth.exceptions.SenseRequiredException;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.SenseResetOriginalInteractor;
import is.hello.sense.interactors.SwapSenseInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class UpgradePairSensePresenter extends PairSensePresenter {

    private final SwapSenseInteractor swapSenseInteractor;
    private final SenseResetOriginalInteractor resetOriginalInteractor;

    public UpgradePairSensePresenter(final HardwareInteractor hardwareInteractor,
                                     final UserFeaturesInteractor userFeaturesInteractor,
                                     final ApiService apiService,
                                     final SwapSenseInteractor swapSenseInteractor,
                                     final SenseResetOriginalInteractor resetOriginalInteractor) {
        super(hardwareInteractor,
              userFeaturesInteractor,
              apiService);

        this.swapSenseInteractor = swapSenseInteractor;
        this.resetOriginalInteractor = resetOriginalInteractor;
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
    protected boolean shouldContinueFlow() {
        return true;
    }

    @Override
    protected boolean shouldClearPeripheral() {
        return true;
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
    protected Observable<SensePeripheral> getObservableSensePeripheral(){
        hardwareInteractor.clearPeripheral();
        final SenseDevice currentSenseDevice = resetOriginalInteractor.getCurrentSense();
        if(currentSenseDevice != null){
            return hardwareInteractor.closestPeripheral(
                    Collections.singleton(currentSenseDevice.deviceId));
        } else {
            return Observable.error(new SenseRequiredException());
        }

    }

    @Override
    public void completePeripheralPair() {
        swapSenseInteractor.setRequest(hardwareInteractor.getDeviceId());
        //todo undue below
        super.completePeripheralPair();
        /*
        bindAndSubscribe(swapSenseInteractor.canSwap(),
                  okStatus -> super.completePeripheralPair(),
                  e -> presentError(e, "swap sense")
                 );
                 */
    }
}
