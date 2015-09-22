package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.AppStats;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

@Singleton public class UnreadStatePresenter extends ValuePresenter<Boolean> {
    private final ApiService apiService;
    private final PreferencesPresenter preferences;

    public final PresenterSubject<Boolean> hasUnreadItems = this.subject;

    @Inject public UnreadStatePresenter(@NonNull ApiService apiService,
                                        @NonNull PreferencesPresenter preferences) {
        this.apiService = apiService;
        this.preferences = preferences;

        final boolean initialValue = preferences.getBoolean(PreferencesPresenter.HAS_UNREAD_INSIGHT_ITEMS, false);
        hasUnreadItems.onNext(initialValue);
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
        return apiService.unreadStats().map(appUnreadStats -> {
            final boolean hasUnreadItems = (appUnreadStats.hasUnreadInsights() ||
                    appUnreadStats.hasUnansweredQuestions());

            preferences.edit()
                       .putBoolean(PreferencesPresenter.HAS_UNREAD_INSIGHT_ITEMS, hasUnreadItems)
                       .apply();

            return hasUnreadItems;
        });
    }

    public void updateInsightsLastViewed() {
        logEvent("Updating insights last viewed");

        final AppStats appStats = new AppStats();
        appStats.setInsightsLastViewed(DateTime.now());
        apiService.updateStats(appStats)
                  .subscribe(ignored -> {
                                 logEvent("Updated insights last viewed");
                                 update();
                             },
                             Functions.LOG_ERROR);
    }
}
