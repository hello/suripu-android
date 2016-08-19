package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import java.io.InvalidObjectException;
import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.api.model.v2.InsightInfo;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class InsightsInteractor extends ScopedValueInteractor<ArrayList<Insight>> {
    @Inject ApiService apiService;
    @Inject
    UnreadStateInteractor unreadStatePresenter;

    public final InteractorSubject<ArrayList<Insight>> insights = this.subject;

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
                         .doOnCompleted(unreadStatePresenter::updateInsightsLastViewed);
    }


    @Deprecated
    public Observable<InsightInfo> infoForInsight(@NonNull Insight insight) {
        return apiService.insightInfo(insight.getCategory()).flatMap(insights -> {
            if (insights.isEmpty()) {
                return Observable.error(new InvalidObjectException("No insight info found."));
            } else {
                return Observable.just(insights.get(0));
            }
        });
    }
}
