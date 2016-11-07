package is.hello.sense.flows.expansions.ui.fragments;

import org.junit.Before;
import org.junit.Test;

import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.graph.SenseTestCase;

import static junit.framework.Assert.assertEquals;

public class ConfigSelectionFragmentTest extends SenseTestCase {

    private ConfigSelectionFragment configSelectionFragment;

    @Before
    public void setUp(){
        configSelectionFragment = new ConfigSelectionFragment();
        startNestedVisibleFragment(configSelectionFragment);
    }

    @Test
    public void bindExpansionDisplaysTitleTextViewsProperly() throws Exception {

        final Expansion testExpansion = Expansion.generateTemperatureTestCase(State.NOT_CONFIGURED);
        configSelectionFragment.bindExpansion(testExpansion);

        assertEquals("Connected to " + testExpansion.getCompanyName(), configSelectionFragment.presenterView.getTitleText());
        assertEquals("Select the " + testExpansion.getConfigurationType() + " that Sense will control.", configSelectionFragment.presenterView.getSubtitleText());
    }

}