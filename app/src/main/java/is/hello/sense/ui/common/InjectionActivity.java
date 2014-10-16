package is.hello.sense.ui.common;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import is.hello.sense.SenseApplication;
import is.hello.sense.ui.activities.SenseActivity;
import rx.Observable;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;

public class InjectionActivity extends SenseActivity implements ObservableContainer {
    protected ArrayList<Subscription> subscriptions = new ArrayList<>();

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
        return AndroidObservable.bindActivity(this, toBind);
    }

    @Override
    public @NonNull <T> Subscription subscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return track(toSubscribe.subscribe(onNext, onError));
    }

    @Override
    public @NonNull <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return subscribe(bind(toSubscribe), onNext, onError);
    }
}
