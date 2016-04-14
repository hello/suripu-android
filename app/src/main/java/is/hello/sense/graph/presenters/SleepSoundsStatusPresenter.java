package is.hello.sense.graph.presenters;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;
import rx.functions.Func1;

public class SleepSoundsStatusPresenter extends ScopedValuePresenter<SleepSoundStatus> {
    @Inject
    ApiService apiService;

    public final PresenterSubject<SleepSoundStatus> state = this.subject;

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
        return Observable.interval(0, 500, TimeUnit.MILLISECONDS)
                         .flatMap(new Func1<Long, Observable<SleepSoundStatus>>() {
                             @Override
                             public Observable<SleepSoundStatus> call(Long tick) {

                                 return apiService.getSleepSoundStatus()
                                                  .doOnError(err -> Log.e("Polling", "Error retrieving messages" + err))
                                                  .onErrorResumeNext(new Func1<Throwable, Observable<? extends SleepSoundStatus>>() {
                                                      @Override
                                                      public Observable<? extends SleepSoundStatus> call(Throwable throwable) {
                                                          return Observable.empty();
                                                      }
                                                  });
                             }
                         });
    }
}