package is.hello.sense.interactors.pairsense;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.sense.interactors.Interactor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import rx.Observable;

/**
 * Any behavior that is shared by subclasses of {@link is.hello.sense.presenters.BasePairSensePresenter}
 * can be made abstract or implemented here.
 */
public abstract class PairSenseInteractor extends Interactor {

    protected final HardwareInteractor hardwareInteractor;

    public PairSenseInteractor(@NonNull final HardwareInteractor hardwareInteractor){
        this.hardwareInteractor = hardwareInteractor;
    }

    @StringRes
    public abstract int getPairingRes();

    @StringRes
    public abstract int getFinishedRes();

    public abstract boolean shouldContinueFlow();

    public abstract boolean shouldClearPeripheral();

    public abstract int getLinkedAccountErrorTitleRes();

    public abstract String getOnFinishedAnalyticsEvent();

    public Observable<SensePeripheral> closestPeripheral(){
        return hardwareInteractor.closestPeripheral();
    }

    public Observable<Void> linkAccount(){
        return hardwareInteractor.linkAccount();
    }

}
