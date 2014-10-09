package is.hello.sense.ui.common;

import android.support.annotation.NonNull;

import rx.Subscription;

public interface SubscriptionTracker {
    boolean hasSubscriptions();
    @NonNull Subscription track(@NonNull Subscription subscription);
}
