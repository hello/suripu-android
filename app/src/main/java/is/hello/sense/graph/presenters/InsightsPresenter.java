package is.hello.sense.graph.presenters;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class InsightsPresenter extends ScopedValuePresenter<ArrayList<Insight>> {
    @Inject ApiService apiService;

    public final PresenterSubject<ArrayList<Insight>> insights = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<Insight>> provideUpdateObservable() {
        return apiService.currentInsights();
    }
}
