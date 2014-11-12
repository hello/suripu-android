package is.hello.sense.ui.common;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.SenseApplication;
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.util.Logger;
import is.hello.sense.util.ResumeScheduler;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.operators.OperatorConditionalBinding;

public class InjectionActivity extends SenseActivity implements ObservableContainer, ResumeScheduler.Resumable {
    protected ArrayList<Subscription> subscriptions = new ArrayList<>();

    protected boolean isResumed = false;
    protected @Nullable List<Runnable> onResumeRunnables; //Synchronize all access to this!
    protected final ResumeScheduler observeScheduler = new ResumeScheduler(this);
    protected static final Func1<Activity, Boolean> ACTIVITY_VALIDATOR = (activity) -> !activity.isFinishing();

    public InjectionActivity() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (Subscription subscription : subscriptions) {
            if (!subscription.isUnsubscribed())
                subscription.unsubscribe();
        }

        subscriptions.clear();
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
        synchronized(this) {
            if (onResumeRunnables != null) {
                for (Runnable runnable : onResumeRunnables) {
                    runnable.run();
                }
                onResumeRunnables.clear();
            }
        }
    }

    @Override
    public void postOnResume(@NonNull Runnable runnable) {
        if (isResumed) {
            runnable.run();
        } else {
            synchronized(this) {
                if (onResumeRunnables == null)
                    onResumeRunnables = new ArrayList<>();

                onResumeRunnables.add(runnable);
            }
        }
    }

    @Override
    public void cancelPostOnResume(@NonNull Runnable runnable) {
        synchronized(this) {
            if (onResumeRunnables != null) {
                onResumeRunnables.remove(runnable);
            }
        }
    }

    @Override
    public boolean hasSubscriptions() {
        return !subscriptions.isEmpty();
    }

    @Override
    public @NonNull Subscription track(@NonNull Subscription subscription) {
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public @NonNull <T> Observable<T> bind(@NonNull Observable<T> toBind) {
        return toBind.observeOn(observeScheduler)
                     .lift(new OperatorConditionalBinding<>(this, ACTIVITY_VALIDATOR));
    }

    @Override
    public @NonNull <T> Subscription subscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return track(toSubscribe.unsafeSubscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                try {
                    onError.call(e);
                } catch (Throwable actionError) {
                    Logger.error(InjectionActivity.class.getSimpleName(), "onError handler threw an exception, crashing", e);
                    throw actionError;
                }
            }

            @Override
            public void onNext(T t) {
                onNext.call(t);
            }
        }));
    }

    @Override
    public @NonNull <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return subscribe(bind(toSubscribe), onNext, onError);
    }
}
