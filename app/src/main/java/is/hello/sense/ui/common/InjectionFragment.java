package is.hello.sense.ui.common;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import is.hello.sense.SenseApplication;
import rx.Subscription;

public class InjectionFragment extends Fragment {
    protected ArrayList<Subscription> subscriptions = new ArrayList<>();

    public InjectionFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        for (Subscription subscription : subscriptions) {
            if (!subscription.isUnsubscribed())
                subscription.unsubscribe();
        }

        subscriptions.clear();
    }

    protected boolean hasSubscriptions() {
        return !subscriptions.isEmpty();
    }

    protected @NonNull Subscription track(@NonNull Subscription subscription) {
        subscriptions.add(subscription);
        return subscription;
    }
}
