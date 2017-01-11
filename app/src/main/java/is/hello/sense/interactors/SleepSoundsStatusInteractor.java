package is.hello.sense.interactors;



import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class SleepSoundsStatusInteractor extends ScopedValueInteractor<SleepSoundStatus> {
    public final static int OFFLINE_MINUTES = 30;

    private static final int MAX_TIME_OUT_MS = 30000;

    private final static int POLLING_INTERVAL_MS = 1000;
    private final static int INITIAL_BACK_OFF_MS = 0;
    private final static int BACK_OFF_INCREMENTS_MS = 1000;
    private final static int MAX_BACK_OFF_MS = 6000;

    private int backOff = INITIAL_BACK_OFF_MS;

    private long timeSent = 0;

    private Subscription updateSubscription = Subscriptions.empty();

    @Inject
    ApiService apiService;

    public final InteractorSubject<SleepSoundStatus> state = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<SleepSoundStatus> provideUpdateObservable() {
        return apiService.getSleepSoundStatus();
    }

    /**
     * @return true if back off interval was reset else it wasn't required.
     */
    public synchronized boolean resetBackOffIfNeeded() {
        if (this.backOff != INITIAL_BACK_OFF_MS) {
            this.backOff = INITIAL_BACK_OFF_MS;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Remember to start polling to use the new value.
     */
    public synchronized void incrementBackoff() {
        this.backOff += BACK_OFF_INCREMENTS_MS;
    }

    public synchronized int getBackOffInterval() {
        return POLLING_INTERVAL_MS + this.backOff;
    }

    public synchronized void resetTimeSent() {
        this.timeSent = System.currentTimeMillis();
    }

    public synchronized boolean isBackOffMaxed() {
        return backOff > MAX_BACK_OFF_MS;
    }

    public synchronized boolean isTimedOut() {
        return System.currentTimeMillis() - timeSent > MAX_TIME_OUT_MS;
    }

    public synchronized void stopPolling() {
        updateSubscription.unsubscribe();
    }

    public synchronized void startPolling() {
        updateSubscription.unsubscribe();
        updateSubscription = Observable.interval(getBackOffInterval(), TimeUnit.MILLISECONDS)
                                       .subscribeOn(Schedulers.newThread())
                                       .observeOn(Schedulers.newThread())
                                       .subscribe(o -> {
                                           update();
                                       });
    }

    @Override
    public void onContainerDestroyed() {
        super.onContainerDestroyed();
        updateSubscription.unsubscribe();
    }

    public class ObservableEndThrowable extends Throwable {

    }


}