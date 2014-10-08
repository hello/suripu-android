package is.hello.sense.graph.presenters;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.SenseApplication;

public abstract class Presenter {
    public Presenter() {
        SenseApplication.getInstance().inject(this);
    }

    public void onRestoreState(@NonNull Parcelable savedState) {

    }

    public @Nullable Parcelable onSaveState() {
        return null;
    }

    public @NonNull String getSavedStateKey() {
        return getClass().getSimpleName() + "#instanceState";
    }

    public abstract void update();
}
