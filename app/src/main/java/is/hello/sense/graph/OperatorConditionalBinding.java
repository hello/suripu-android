package is.hello.sense.graph;

import android.support.annotation.NonNull;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

public class OperatorConditionalBinding<T, U> implements Observable.Operator<T, T> {
    private U boundValue;
    private final Func1<? super U, Boolean> predicate;

    public OperatorConditionalBinding(@NonNull U boundValue,
                                      @NonNull Func1<? super U, Boolean> predicate) {
        this.predicate = predicate;
        this.boundValue = boundValue;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        return new Subscriber<T>(child) {
            @Override
            public void onCompleted() {
                if (shouldForward()) {
                    child.onCompleted();
                } else {
                    handleLostBinding();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (shouldForward()) {
                    child.onError(e);
                } else {
                    handleLostBinding();
                }
            }

            @Override
            public void onNext(T value) {
                if (shouldForward()) {
                    child.onNext(value);
                } else {
                    handleLostBinding();
                }
            }


            private boolean shouldForward() {
                return (boundValue != null && predicate.call(boundValue));
            }

            private void handleLostBinding() {
                OperatorConditionalBinding.this.boundValue = null;
                unsubscribe();
            }
        };
    }
}
