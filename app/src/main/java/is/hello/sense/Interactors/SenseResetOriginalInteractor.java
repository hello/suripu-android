package is.hello.sense.interactors;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import is.hello.sense.api.ApiService;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.presenters.ValuePresenter;
import rx.Observable;

public class SenseResetOriginalInteractor extends ValuePresenter<Boolean>{

    public final PresenterSubject<Boolean> resetResult = this.subject;

    private final ApiService apiService;

    public SenseResetOriginalInteractor(final ApiService apiService) {
        super();
        this.apiService = apiService;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Boolean> provideUpdateObservable() {
        //todo replace with real api call
        return Observable.from(Collections.singletonList(true))
                .delay(2, TimeUnit.SECONDS);
    }

    public void destroy() {
        resetResult.forget();
    }
}
