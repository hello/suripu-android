package is.hello.sense.flows.expansions.interactors;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
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

    @Test
    public void selectedConfiguration(){
        final ArrayList<Configuration> testConfigs = new ArrayList<>();
        final Configuration selectedConfig = new Configuration("2", "selected", true);
        testConfigs.add(new Configuration("1", "not selected", false));
        testConfigs.add(selectedConfig);
        final Configuration resultConfig = Sync.wrapAfter(() -> configurationsInteractor.configSubject.onNext(testConfigs),
                                                            configurationsInteractor.selectedConfiguration())
                                                 .last();
        assertNotNull(resultConfig);
        assertTrue(resultConfig.isSelected());
        assertEquals(selectedConfig, resultConfig);
    }

    @Test
    public void selectedConfigurationReturnsEmptyIfNull(){
        final Configuration selectedConfig = Sync.wrapAfter(() -> configurationsInteractor.configSubject.onNext(null),
                                                            configurationsInteractor.selectedConfiguration())
                                                 .last();
        assertNotNull(selectedConfig);
        assertTrue(selectedConfig instanceof Configuration.Empty);
    }

}
