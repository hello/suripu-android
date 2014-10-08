package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.List;

public interface PresenterContainer {
    void addPresenter(@NonNull Presenter presenter);
    void removePresenter(@NonNull Presenter presenter);
    @NonNull List<Presenter> getPresenters();
}
