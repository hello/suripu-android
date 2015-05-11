package is.hello.sense.ui.common;

import android.app.Dialog;
import android.app.DialogFragment;
import android.support.annotation.NonNull;

import is.hello.sense.util.StateSafeExecutor;
import is.hello.sense.util.StateSafeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public class SenseDialogFragment extends DialogFragment implements ObservableContainer, StateSafeExecutor.Resumes {
    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(this);
    protected final StateSafeScheduler observeScheduler = new StateSafeScheduler(stateSafeExecutor);

    protected static final Func1<DialogFragment, Boolean> FRAGMENT_VALIDATOR = (fragment) -> fragment.isAdded() && !fragment.getActivity().isFinishing();
    protected final DelegateObservableContainer<DialogFragment> observableContainer = new DelegateObservableContainer<>(observeScheduler, this, FRAGMENT_VALIDATOR);


    @Override
    public void onResume() {
        super.onResume();

        stateSafeExecutor.executePendingForResume();
    }

    @Override
    public void onDestroyView() {
        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }

        super.onDestroyView();
    }


    public void dismissSafely() {
        stateSafeExecutor.execute(() -> {
            if (isAdded()) {
                dismiss();
            }
        });
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
    public <T> Subscription subscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return observableContainer.subscribe(toSubscribe, onNext, onError);
    }

    @Override
    @NonNull
    public <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return observableContainer.bindAndSubscribe(toSubscribe, onNext, onError);
    }
}
