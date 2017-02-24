package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.support.annotation.NonNull;
import is.hello.sense.mvp.interactors.InteractorContainer;
import is.hello.sense.mvp.view.SenseView;
import is.hello.sense.ui.common.SenseFragment;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

public abstract class SensePresenter<SV extends SenseView, IC extends InteractorContainer> {
    @NonNull
    private final SV senseView;

    @NonNull
    private final IC interactorContainer;

    public SensePresenter(@NonNull final SenseFragment fragment) {
        this.senseView = initializeSenseView(fragment.getActivity());
        this.interactorContainer = initializeInteractorContainer(fragment);
    }

    protected abstract SV initializeSenseView(@NonNull Activity activity);

    protected abstract IC initializeInteractorContainer(@NonNull SenseFragment fragment);

    @NonNull
    public SV getSenseView() {
        return this.senseView;
    }

    @NonNull
    public IC getInteractorContainer() {
        return this.interactorContainer;
    }

    public abstract void bindAndSubscribeAll();

    @NonNull
    public <T> Subscription bindAndSubscribe(@NonNull final Observable<T> toSubscribe,
                                             @NonNull final Action1<? super T> onNext,
                                             @NonNull final Action1<Throwable> onError) {
        return getInteractorContainer().bindAndSubscribe(toSubscribe, onNext, onError);
    }

    public static class EmptySensePresenter extends SensePresenter<SenseView.EmptySenseView, InteractorContainer.EmptyInteractorContainer> {
        public EmptySensePresenter(@NonNull final SenseFragment fragment) {
            super(fragment);
        }

        @Override
        protected SenseView.EmptySenseView initializeSenseView(@NonNull final Activity activity) {
            return new SenseView.EmptySenseView(activity);
        }

        @Override
        protected InteractorContainer.EmptyInteractorContainer initializeInteractorContainer(@NonNull SenseFragment fragment) {
            return new InteractorContainer.EmptyInteractorContainer(fragment);
        }

        @Override
        public void bindAndSubscribeAll() {

        }
    }

}
