package is.hello.sense.interactors;

import android.annotation.SuppressLint;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.AppStats;
import is.hello.sense.api.model.AppUnreadStats;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressLint("CommitPrefEdits")
public class UnreadStateInteractorTests extends InjectionTestCase {
    @Inject
    PreferencesInteractor preferences;

    private ApiService apiService;
    private UnreadStateInteractor unreadStatePresenter;

    @Before
    public void setUp() {
        preferences.edit()
                   .clear()
                   .commit();

        this.apiService = mock(ApiService.class);
        this.unreadStatePresenter = new UnreadStateInteractor(apiService, preferences);
    }


    @Test
    public void producesCorrectValue() {
        doReturn(Observable.just(new AppUnreadStats(false, false)))
                .when(apiService)
                .unreadStats();
        assertThat(Sync.wrapAfter(unreadStatePresenter::update, unreadStatePresenter.hasUnreadItems).last(),
                   is(false));

        doReturn(Observable.just(new AppUnreadStats(true, false)))
                .when(apiService)
                .unreadStats();
        assertThat(Sync.wrapAfter(unreadStatePresenter::update, unreadStatePresenter.hasUnreadItems).last(),
                   is(true));

        doReturn(Observable.just(new AppUnreadStats(true, true)))
                .when(apiService)
                .unreadStats();
        assertThat(Sync.wrapAfter(unreadStatePresenter::update, unreadStatePresenter.hasUnreadItems).last(),
                   is(true));

        doReturn(Observable.just(new AppUnreadStats(false, true)))
                .when(apiService)
                .unreadStats();
        assertThat(Sync.wrapAfter(unreadStatePresenter::update, unreadStatePresenter.hasUnreadItems).last(),
                   is(true));

        verify(apiService, times(4)).unreadStats();
    }

    @Test
    public void updatesPreferences() {
        doReturn(Observable.just(new AppUnreadStats(false, false)))
                .when(apiService)
                .unreadStats();
        assertThat(Sync.wrapAfter(unreadStatePresenter::update, unreadStatePresenter.hasUnreadItems).last(),
                   is(false));

        assertThat(preferences.getBoolean(PreferencesInteractor.HAS_UNREAD_INSIGHT_ITEMS, true),
                   is(false));


        doReturn(Observable.just(new AppUnreadStats(true, true)))
                .when(apiService)
                .unreadStats();
        assertThat(Sync.wrapAfter(unreadStatePresenter::update, unreadStatePresenter.hasUnreadItems).last(),
                   is(true));

        assertThat(preferences.getBoolean(PreferencesInteractor.HAS_UNREAD_INSIGHT_ITEMS, false),
                   is(true));

        verify(apiService, times(2)).unreadStats();
    }

    @Test(timeout = 3000L)
    public void updateInsightsLastViewed() {
        doReturn(Observable.just(new AppUnreadStats(false, false)))
                .when(apiService)
                .unreadStats();
        doReturn(Observable.just(new VoidResponse()))
                .when(apiService)
                .updateStats(any(AppStats.class));

        unreadStatePresenter.updateInsightsLastViewed();
        assertThat(Sync.last(unreadStatePresenter.latest()), is(false));

        verify(apiService).updateStats(any(AppStats.class));
        verify(apiService).unreadStats();
    }
}
