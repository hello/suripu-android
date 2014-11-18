package is.hello.sense.graph.presenters;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class InsightsPresenter extends UpdatablePresenter<List<Insight>> {
    @Inject ApiService apiService;

    public final PresenterSubject<List<Insight>> insights = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<List<Insight>> provideUpdateObservable() {
        return apiService.currentInsights();
    }
}
