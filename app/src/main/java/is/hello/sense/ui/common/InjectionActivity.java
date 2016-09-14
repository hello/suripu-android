package is.hello.sense.ui.common;

import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.SenseApplication;
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.util.StateSafeExecutor;
import is.hello.sense.util.StateSafeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public abstract class InjectionActivity extends SenseActivity implements ObservableContainer {
    protected boolean isResumed = false;
    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(() -> isResumed);
    protected final StateSafeScheduler observeScheduler = new StateSafeScheduler(stateSafeExecutor);

    protected static final Func1<Activity, Boolean> ACTIVITY_VALIDATOR = a -> !a.isFinishing();
    protected final DelegateObservableContainer<Activity> observableContainer = new DelegateObservableContainer<>(observeScheduler, this, ACTIVITY_VALIDATOR);

    public InjectionActivity() {
        if(shouldInjectToMainGraphObject()) {
            SenseApplication.getInstance().inject(this);
        }
    }

    protected boolean shouldInjectToMainGraphObject(){
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        observableContainer.clearSubscriptions();
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.isResumed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.isResumed = true;
        stateSafeExecutor.executePendingForResume();
    }


    @Override
    public boolean hasSubscriptions() {
        return observableContainer.hasSubscriptions();
    }

    @Override
    @NonNull
    public Subscription track(@NonNull Subscription subscription) {
        return observableContainer.track(subscription);
    }

    @Override
    @NonNull
    public <T> Observable<T> bind(@NonNull Observable<T> toBind) {
        return observableContainer.bind(toBind);
    }

    @Override
    @NonNull
    public <T> Subscription subscribe(@NonNull Observable<T> toSubscribe,
                                      @NonNull Action1<? super T> onNext,
                                      @NonNull Action1<Throwable> onError) {
        return observableContainer.subscribe(toSubscribe, onNext, onError);
    }

    @Override
    @NonNull
    public <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe,
                                             @NonNull Action1<? super T> onNext,
                                             @NonNull Action1<Throwable> onError) {
        return observableContainer.bindAndSubscribe(toSubscribe, onNext, onError);
    }
}
