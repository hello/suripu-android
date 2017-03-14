package is.hello.sense.ui.dialogs;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.model.v2.alerts.Category;
import is.hello.sense.graph.SenseTestCase;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.util.FragmentTestUtil.startFragment;

public class SystemAlertDialogFragmentTest extends SenseTestCase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void throwExceptionWithOutActionHandler() throws IllegalStateException {
        final SystemAlertDialogFragment fragment = SystemAlertDialogFragment.newInstance(Alert.NewEmptyInstance(),
                                                                                         getResources());
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(Matchers.containsString(Alert.ActionHandler.class.getName() + " required to handle actions"));
        startFragment(fragment);
    }

    @Test
    public void onPositiveButtonClickedForMuteSenseAlert() throws Exception {
        final Alert mutedAlert = new Alert("Sense muted",
                                           "Body test",
                                           Category.SENSE_MUTED);
        final SystemAlertDialogFragment spyFragment = spy(SystemAlertDialogFragment.newInstance(mutedAlert,
                                                                                                getResources()));
        final Alert.ActionHandler mockActionHandler = mock(Alert.ActionHandler.class);

        doReturn(mockActionHandler)
                .when(spyFragment)
                .getActionHandler();

        startFragment(spyFragment);
        spyFragment.onPositiveButtonClicked();

        verify(mockActionHandler).unMuteSense();
    }
}