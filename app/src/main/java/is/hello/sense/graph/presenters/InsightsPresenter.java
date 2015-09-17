package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.AppStats;
import is.hello.sense.api.model.Insight;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class InsightsPresenter extends ScopedValuePresenter<ArrayList<Insight>> {
    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferences;

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


    public void updateLastViewed() {
        logEvent("Updating insights last viewed");

        final AppStats appStats = new AppStats();
        appStats.setInsightsLastViewed(DateTime.now());
        apiService.updateStats(appStats)
                  .subscribe(ignored -> logEvent("Updated insights last viewed"),
                             Functions.LOG_ERROR);

        preferences.edit()
                   .putBoolean(PreferencesPresenter.HAS_UNREAD_INSIGHT_ITEMS, false)
                   .apply();
    }
}
