package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import is.hello.sense.api.model.SenseTimeZone;
import rx.Observable;

public class PairSenseInteractor extends Interactor {

    private HardwareInteractor hardwareInteractor;
    private AccountInteractor accountInteractor;
    private UserFeaturesInteractor userFeaturesInteractor;

    public PairSenseInteractor(@NonNull final HardwareInteractor hardwareInteractor,
                               @NonNull final AccountInteractor accountInteractor,
                               @NonNull final UserFeaturesInteractor userFeaturesInteractor) {
        this.hardwareInteractor = hardwareInteractor;
        this.accountInteractor = accountInteractor;
        this.userFeaturesInteractor = userFeaturesInteractor;
    }


    public Observable<Void> linkAccount() {
        return hardwareInteractor.linkAccount();
    }

    public Observable<SenseTimeZone> updateTimeZone() {
        return accountInteractor.updateTimeZone(SenseTimeZone.fromDefault());
    }

    public Observable<Void> pushData() {
        return hardwareInteractor.pushData();
    }

    public Observable<Void> storeFeaturesInPrefs() {
        return userFeaturesInteractor.storeFeaturesInPrefs();
    }
}
