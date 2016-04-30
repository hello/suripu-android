package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.api.model.v2.SleepSoundStatus;
import rx.Observable;
import rx.subjects.PublishSubject;

public class SleepSoundsStatusBus {

    private static SleepSoundsStatusBus instance;
    private PublishSubject<SleepSoundStatus> subject = PublishSubject.create();

    public static SleepSoundsStatusBus instanceOf() {
        if (instance == null) {
            instance = new SleepSoundsStatusBus();
        }
        return instance;
    }

    public void setValue(final @NonNull SleepSoundStatus status) {
        subject.onNext(status);
    }

    public Observable<SleepSoundStatus> getObservable() {
        return subject;
    }
}
