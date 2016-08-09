package is.hello.sense.graph.presenters;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

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


}
