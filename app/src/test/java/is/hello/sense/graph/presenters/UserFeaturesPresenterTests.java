package is.hello.sense.graph.presenters;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class UserFeaturesPresenterTests extends InjectionTestCase{

    @Inject
    UserFeaturesPresenter userFeaturesPresenter;

    @Before
    public void setUp(){
        userFeaturesPresenter.reset();
    }

    @Test //todo figure out how to mock local broadcast logout intent
    public void clearPreferencesOnReset(){
        Sync.last(userFeaturesPresenter.storeFeaturesInPrefs());

        assertTrue(userFeaturesPresenter.hasVoice());

        userFeaturesPresenter.reset();

        Robolectric.flushBackgroundThreadScheduler();

        assertFalse(userFeaturesPresenter.hasVoice());
    }

    @Test
    public void retryOnErrorDefaultNumberTimes(){
        final AtomicInteger tryCount = new AtomicInteger(0);
        final UserFeaturesPresenter spy = spy(userFeaturesPresenter);
        when(spy.provideUpdateObservable())
        .thenReturn(Observable.defer( () -> {
            tryCount.incrementAndGet();
            return Observable.error(new Throwable("fake"));
        })
                   );

        Sync.wrap(spy.storeFeaturesInPrefs(UserFeaturesPresenter.DEFAULT_NUM_RETRIES))
        .assertThrows(Throwable.class);

        assertEquals(UserFeaturesPresenter.DEFAULT_NUM_RETRIES, tryCount.get() - 1);
    }

}
