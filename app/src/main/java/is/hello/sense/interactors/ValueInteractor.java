package is.hello.sense.interactors;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.lang.ref.WeakReference;

import is.hello.sense.functional.Functions;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.graph.SafeObserverWrapper;
import rx.Observable;
import rx.Subscription;

/**
 * A subclass of {@see Interactor} that adds generic logic for properly
 * handling overlapping update operations on a presenter subject.
 * @param <T>   The type of data being presented. If T extends Serializable, it will automatically be saved.
 */
public abstract class ValueInteractor<T extends Serializable> extends Interactor {
    public static final String SAVED_STATE_KEY = "ValueInteractor#saved_state";

    /**
     * The subject of the presenter. Subclasses should expose this
     * field under an appropriately named public final field.
     * <p/>
     * Example:
     * <pre>
     *     class BookPresenter extends ValueInteractor<Book> {
     *         public final InteractorSubject<T> book = this.subject;
     *     }
     * </pre>
     */
    protected final InteractorSubject<T> subject = InteractorSubject.create();

    /**
     * The current update operation. Will be canceled if an overlapping update is requested.
     */
    private @Nullable WeakReference<Subscription> updateSubscription;


    //region Low Memory

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        if (isDataDisposable()) {
            subject.forget();
            final Subscription existingSubscription = Functions.extract(this.updateSubscription);
            if (existingSubscription != null) {
                existingSubscription.unsubscribe();
                this.updateSubscription = null;
            }
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
        final Bundle state = new Bundle();
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
     * Cancels any pending updates within the value presenter.
     */
    @Override
    public void onContainerDestroyed() {
        super.onContainerDestroyed();

        final Subscription existingSubscription = Functions.extract(this.updateSubscription);
        if (existingSubscription != null) {
            existingSubscription.unsubscribe();
            this.updateSubscription = null;
        }
    }

    /**
     * Updates the subject of the presenter.
     */
    public final void update() {
        final Subscription existingSubscription = Functions.extract(this.updateSubscription);
        if (existingSubscription != null) {
            existingSubscription.unsubscribe();
            this.updateSubscription = null;
        }

        if (canUpdate()) {
            final Observable<T> updateObservable = provideUpdateObservable();
            final Subscription newSubscription = updateObservable.subscribe(new SafeObserverWrapper<>(subject));
            this.updateSubscription = new WeakReference<>(newSubscription);
        }
    }

    /**
     * Updates the subject of the presenter
     * if it does not currently have a value.
     */
    public final boolean updateIfEmpty() {
        if (!subject.hasValue()) {
            update();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns an terminating observable that will emit the
     * latest value contained in the value presenter.
     */
    public Observable<T> latest() {
        if (!subject.hasValue() && updateSubscription == null) {
            update();
        }

        return subject.take(1);
    }

    //endregion
}
