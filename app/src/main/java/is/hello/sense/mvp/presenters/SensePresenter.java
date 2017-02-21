package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

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

    @NonNull
    private final Context context;

    public SensePresenter(@NonNull final SenseFragment fragment) {
        this.context = fragment.getActivity();
        for (final Class clazz : requiredInterfaces()) {
            if (!fragment.getClass().isInstance(clazz)) {
                throw new IllegalStateException("fragment " + fragment.getClass().getSimpleName() + " must implement " + clazz.getSimpleName());
            }
        }
        this.interactorContainer = initializeInteractorContainer(fragment);
        this.senseView = initializeSenseView(fragment.getActivity());
    }

    protected abstract SV initializeSenseView(@NonNull Activity activity);

    protected abstract IC initializeInteractorContainer(@NonNull SenseFragment fragment);

    protected abstract void release();

    public abstract void bindAndSubscribeAll();

    @NonNull
    public SV getSenseView() {
        return this.senseView;
    }

    @NonNull
    public IC getInteractorContainer() {
        return this.interactorContainer;
    }

    @NonNull
    public Context getContext() {
        return context;
    }

    @NonNull
    public <T> Subscription bindAndSubscribe(@NonNull final Observable<T> toSubscribe,
                                             @NonNull final Action1<? super T> onNext,
                                             @NonNull final Action1<Throwable> onError) {
        return getInteractorContainer().bindAndSubscribe(toSubscribe, onNext, onError);
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

    }

    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, final int[] grantResults) {
    }

    @NonNull
    public List<Class> requiredInterfaces() {
        return new ArrayList<>();
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

        @Override
        protected void release() {

        }
    }

}
