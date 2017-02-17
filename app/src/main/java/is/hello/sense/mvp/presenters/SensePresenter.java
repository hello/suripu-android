package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.mvp.interactors.InteractorContainer;
import is.hello.sense.mvp.view.SenseView;

public abstract class SensePresenter<SV extends SenseView, IC extends InteractorContainer> {
    @NonNull
    private final SV senseView;

    @NonNull
    private final IC interactorContainer;

    public SensePresenter(@NonNull final Activity activity) {
        this.senseView = initializeSenseView(activity);
        this.interactorContainer = initializeInteractorContainer();
    }

    protected abstract SV initializeSenseView(@NonNull final Activity activity);

    protected abstract IC initializeInteractorContainer();

    @NonNull
    public SV getSenseView() {
        return senseView;
    }

    @NonNull
    public IC getInteractorContainer() {
        return interactorContainer;
    }
}
