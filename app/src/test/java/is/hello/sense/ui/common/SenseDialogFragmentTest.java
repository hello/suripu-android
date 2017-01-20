package is.hello.sense.ui.common;

import android.app.FragmentManager;

import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.Constants;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SenseDialogFragmentTest extends SenseTestCase{
    @Test
    public void showAllowingStateLossDoesNotPerformTransaction() throws Exception {
        final SenseDialogFragment dialogFragment = new SenseDialogFragment();
        final FragmentManager mockedFragmentManager = mock(FragmentManager.class);
        doReturn(true).when(mockedFragmentManager)
                      .isDestroyed();
        final int expected = Constants.NONE;
        final int actual = dialogFragment.showAllowingStateLoss(mockedFragmentManager, "TAG");
        assertEquals(expected, actual);
        verify(mockedFragmentManager, Mockito.never()).beginTransaction();
    }

}