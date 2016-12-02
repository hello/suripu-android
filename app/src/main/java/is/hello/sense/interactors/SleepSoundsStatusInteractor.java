package is.hello.sense.interactors;



import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;
import rx.schedulers.Schedulers;

public class SleepSoundsStatusInteractor extends ScopedValueInteractor<SleepSoundStatus> {
    public final static int OFFLINE_MINUTES = 30;

    private static final int MAX_TIME_OUT_MS = 30000;

    private final static int POLLING_INTERVAL_MS = 1000;
    private final static int INITIAL_BACK_OFF_MS = 0;
    private final static int BACK_OFF_INCREMENTS_MS = 1000;
    private final static int MAX_BACK_OFF_MS = 6000;

    private int backOff = INITIAL_BACK_OFF_MS;

    private boolean resume = false;
    private long timeSent = 0;

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
        return Observable.interval(getBackOffInterval(), TimeUnit.MILLISECONDS, Schedulers.io())
                         .flatMap(longTimeInterval -> {
                             if (resume) {
                                 return apiService.getSleepSoundStatus();
                             }
                             return Observable.error(new ObservableEndThrowable());
                         });
    }

    public void startAndUpdate() {
        this.resume = true;
        update();
    }

    public void stop() {
        this.resume = false;
    }

    /**
     * its important to update the observable so it uses the new interval.
     */
    public void resetBackOff() {
        this.backOff = INITIAL_BACK_OFF_MS;
        update();
    }


    /**
     * its important to update the observable so it uses the new interval.
     */
    public void incrementBackoff() {
        this.backOff += BACK_OFF_INCREMENTS_MS;
        update();
    }

    public int getBackOffInterval() {
        return POLLING_INTERVAL_MS + this.backOff;
    }

    public void resetTimeSent() {
        this.timeSent = System.currentTimeMillis();
    }

    public boolean isBackOffMaxed() {
        return backOff > MAX_BACK_OFF_MS;
    }

    public boolean isTimedOut() {
        return System.currentTimeMillis() - timeSent > MAX_TIME_OUT_MS;
    }

    public class ObservableEndThrowable extends Throwable {

    }


}