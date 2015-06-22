package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.SafeObserverWrapper;
import rx.Observable;
import rx.Subscription;

/**
 * A subclass of {@see Presenter} that adds generic logic for properly
 * handling overlapping update operations on a presenter subject.
 * @param <T>   The type of data being presented. If T extends Serializable, it will automatically be saved.
 */
public abstract class ValuePresenter<T extends Serializable> extends Presenter {
    public static final String SAVED_STATE_KEY = "ValuePresenter#saved_state";

    /**
     * The subject of the presenter. Subclasses should expose this
     * field under an appropriately named public final field.
     * <p/>
     * Example:
     * <pre>
     *     class BookPresenter extends ValuePresenter<Book> {
     *         public final PresenterSubject<T> book = this.subject;
     *     }
     * </pre>
     */
    protected final PresenterSubject<T> subject = PresenterSubject.create();

    /**
     * The current update operation. Will be canceled if an overlapping update is requested.
     */
    private @Nullable Subscription updateSubscription;


    //region Low Memory

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        if (isDataDisposable()) {
            subject.forget();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns whether or not the data contained in the presenter can be easily recreated.
     * <p/>
     * If this method returns yes, the presenter will automatically
     * forget its contents on a low memory warning.
     */
    protected abstract boolean isDataDisposable();

    //endregion


    //region State Saving

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        super.onRestoreState(savedState);

        if (savedState.containsKey(SAVED_STATE_KEY)) {
            //noinspection unchecked
            subject.onNext((T) savedState.getSerializable(SAVED_STATE_KEY));
        }
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle state = new Bundle();
        subject.saveState(SAVED_STATE_KEY, state);
        return state;
    }


    //endregion


    //region Updating

    /**
     * Returns whether or not the presenter can be updated.
     * <p/>
     * {@see #update()} does nothing if this method returns false.
     */
    protected abstract boolean canUpdate();

    /**
     * Returns an update observable to be subscribed to.
     */
    protected abstract Observable<T> provideUpdateObservable();

    /**
     * Updates the subject of the presenter.
     */
    public final void update() {
        if (updateSubscription != null) {
            updateSubscription.unsubscribe();
            this.updateSubscription = null;
        }

        if (canUpdate()) {
            Observable<T> updateObservable = provideUpdateObservable();
            this.updateSubscription = updateObservable.subscribe(new SafeObserverWrapper<>(subject));
        }
    }

    /**
     * Returns an terminating observable that will emit the
     * latest value contained in the value presenter.
     */
    public Observable<T> latest() {
        return subject.take(1);
    }

    //endregion
}
