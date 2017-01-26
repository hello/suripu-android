package is.hello.sense.interactors;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class InsightsInteractor extends ScopedValueInteractor<ArrayList<Insight>> {
    @Inject
    ApiService apiService;
    @Inject
    UnreadStateInteractor unreadStatePresenter;

    public final InteractorSubject<ArrayList<Insight>> insights = this.subject;
    private boolean markShownOnComplete = false;

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
        return apiService.currentInsights()
                         .doOnCompleted(() -> {

                             if (markShownOnComplete) {
                                 markShownOnComplete = false;
                                 unreadStatePresenter.updateInsightsLastViewed();
                             }
                         });
    }

    public void setMarkShownOnComplete(final boolean markShownOnComplete) {
        this.markShownOnComplete = markShownOnComplete;
    }
}
