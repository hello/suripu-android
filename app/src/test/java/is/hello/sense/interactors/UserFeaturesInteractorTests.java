package is.hello.sense.interactors;

import org.junit.Before;
import org.junit.Ignore;
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

@Ignore
public class UserFeaturesInteractorTests extends InjectionTestCase{

   // @Inject
 //   UserFeaturesInteractor userFeaturesPresenter;


/*

    @Test //todo fix one day
    public void retryOnErrorDefaultNumberTimes(){
        final AtomicInteger tryCount = new AtomicInteger(0);
        final UserFeaturesInteractor spy = spy(userFeaturesPresenter);
        when(spy.provideUpdateObservable())
        .thenReturn(Observable.defer( () -> {
            tryCount.incrementAndGet();
            return Observable.error(new Throwable("fake"));
        })
                   );

        Sync.wrap(spy.storeFeaturesInPrefs(UserFeaturesInteractor.DEFAULT_NUM_RETRIES))
        .assertThrows(Throwable.class);

        assertEquals(UserFeaturesInteractor.DEFAULT_NUM_RETRIES, tryCount.get() - 1);
    }
*/

}
