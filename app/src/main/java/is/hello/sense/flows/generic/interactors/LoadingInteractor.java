package is.hello.sense.flows.generic.interactors;

import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

public class LoadingInteractor extends ValueInteractor<VoidResponse> {
    public final InteractorSubject<VoidResponse> sub = subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<VoidResponse> provideUpdateObservable() {
        return Observable.just(new VoidResponse());
    }
}
