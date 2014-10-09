package is.hello.sense.ui.common;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import is.hello.sense.SenseApplication;
import is.hello.sense.ui.activities.SenseActivity;
import rx.Subscription;

public class InjectionActivity extends SenseActivity implements SubscriptionTracker {
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
}
