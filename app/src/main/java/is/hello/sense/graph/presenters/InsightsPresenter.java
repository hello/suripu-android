package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.io.InvalidObjectException;
import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.api.model.v2.InsightInfo;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class InsightsPresenter extends ScopedValuePresenter<ArrayList<Insight>> {
    @Inject ApiService apiService;
    @Inject UnreadStatePresenter unreadStatePresenter;

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
        return apiService.currentInsights()
                         .doOnCompleted(unreadStatePresenter::updateInsightsLastViewed);
    }


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
