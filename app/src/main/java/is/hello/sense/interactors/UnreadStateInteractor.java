package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.AppStats;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

@Singleton public class UnreadStateInteractor extends ValueInteractor<Boolean> {
    private final ApiService apiService;
    private final PreferencesInteractor preferences;

    public final InteractorSubject<Boolean> hasUnreadItems = this.subject;

    @Inject public UnreadStateInteractor(@NonNull ApiService apiService,
                                         @NonNull PreferencesInteractor preferences) {
        this.apiService = apiService;
        this.preferences = preferences;

        final boolean initialValue =
                preferences.getBoolean(PreferencesInteractor.HAS_UNREAD_INSIGHT_ITEMS, false);
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
                       .putBoolean(PreferencesInteractor.HAS_UNREAD_INSIGHT_ITEMS, hasUnreadItems)
                       .apply();

            return hasUnreadItems;
        });
    }

    public void updateInsightsLastViewed() {
        logEvent("Updating insights last viewed");

        apiService.updateStats(AppStats.withLastViewedForNow())
                  .subscribe(ignored -> {
                                 logEvent("Updated insights last viewed");
                                 update();
                             },
                             Functions.LOG_ERROR);
    }
}
