package is.hello.sense.interactors;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class SenseOTAStatusInteractorTests extends InjectionTestCase {
    @Inject
    SenseOTAStatusInteractor senseOTAStatusPresenter;

    @Before
    public void setUp(){
        senseOTAStatusPresenter.reset();
    }

    @Test
    public void clearPrefsOnReset(){
        Sync.last(senseOTAStatusPresenter.storeInPrefs());
        assertTrue(senseOTAStatusPresenter.isOTARequired());
        senseOTAStatusPresenter.reset();
        Robolectric.flushBackgroundThreadScheduler();
        assertFalse(senseOTAStatusPresenter.isOTARequired());
    }
}
