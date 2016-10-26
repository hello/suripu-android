package is.hello.sense.flows.expansions.interactors;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ConfigurationsInteractorTests extends InjectionTestCase {
    @Inject
    ConfigurationsInteractor configurationsInteractor;

    @Before
    public void setUp(){
        configurationsInteractor.configSubject.forget();
        configurationsInteractor.setExpansionId(Expansion.NO_ID);
    }

    @Test
    public void defaultExpansionIdReturnsEmptyList(){
        final List<Configuration> configurations = Sync.wrapAfter(configurationsInteractor::update,
                                                                  configurationsInteractor.configSubject)
                                                       .last();
        assertTrue(configurations.isEmpty());
    }

    @Test
    public void shouldResubscribeTests() {
        assertTrue(configurationsInteractor.shouldResubscribe(null,System.currentTimeMillis()));
        assertTrue(configurationsInteractor.shouldResubscribe(Collections.emptyList(),System.currentTimeMillis()));
    }

    @Test
    public void shouldNotResubscribeTests(){
        assertFalse(configurationsInteractor.shouldResubscribe(
                Collections.singletonList(new Configuration.Empty("", "", R.drawable.error_white)),
                System.currentTimeMillis()));

        assertFalse(configurationsInteractor.shouldResubscribe(
                Collections.singletonList(new Configuration.Empty("", "", R.drawable.error_white)),
                System.currentTimeMillis() - ConfigurationsInteractor.FILTER_NULL_EMPTY_CONFIG_LIST_DURATION_MILLIS));

        assertFalse(configurationsInteractor.shouldResubscribe(
                Collections.emptyList(),
                System.currentTimeMillis() - ConfigurationsInteractor.FILTER_NULL_EMPTY_CONFIG_LIST_DURATION_MILLIS));

        assertFalse(configurationsInteractor.shouldResubscribe(
                null,
                System.currentTimeMillis() - ConfigurationsInteractor.FILTER_NULL_EMPTY_CONFIG_LIST_DURATION_MILLIS));
    }

}
