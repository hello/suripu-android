package is.hello.sense.mvp.interactors;


import android.support.annotation.NonNull;

import java.io.Serializable;

import is.hello.sense.api.fb.model.FacebookProfile;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;

public abstract class SenseInteractor<T extends Serializable> extends ValueInteractor<T> {
    public final InteractorSubject<T> subscriptionSubject = this.subject;

    public interface Binder<G extends Serializable> {
        void bind(G object);
    }

}
