package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.AvailableTrendGraph;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class TrendsPresenter extends ValuePresenter<ArrayList<TrendGraph>> {
    @Inject ApiService apiService;

    public final PresenterSubject<ArrayList<TrendGraph>> trends = subject;


    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<TrendGraph>> provideUpdateObservable() {
        return apiService.allTrends();
    }


    public Observable<ArrayList<AvailableTrendGraph>> availableTrendGraphs() {
        return apiService.availableTrendGraphs();
    }

    public Observable<Void> updateTrend(int position, @NonNull String newTimePeriod) {
        logEvent("updateTrend(" + position + ", " + newTimePeriod + ")");

        return trends.take(1).flatMap(trends -> {
            TrendGraph trend = trends.get(position);
            return apiService.trendGraph(trend.getDataType().toQueryString(), newTimePeriod)
                             .doOnNext(newTrends -> {
                                 ArrayList<TrendGraph> updatedAllTrends = new ArrayList<>(trends);
                                 updatedAllTrends.set(position, newTrends.get(0));
                                 this.trends.onNext(updatedAllTrends);
                             })
                             .map(Functions.TO_VOID);
        });
    }
}
