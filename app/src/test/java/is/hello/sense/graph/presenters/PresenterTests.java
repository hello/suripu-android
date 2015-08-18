package is.hello.sense.graph.presenters;

import android.content.ComponentCallbacks2;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PresenterTests extends SenseTestCase {
    @Test
    public void forgetsDataWhenAppropriate() {
        Presenter presenter = mock(Presenter.class, CALLS_REAL_METHODS);
        doReturn(true)
                .when(presenter)
                .onForgetDataForLowMemory();

        presenter.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
        verify(presenter).onForgetDataForLowMemory();

        presenter.onContainerResumed();
        verify(presenter).onReloadForgottenData();
    }

    @Test
    public void ignoresUnnecessaryTrimCalls() {
        Presenter presenter = mock(Presenter.class, CALLS_REAL_METHODS);
        doReturn(false)
                .when(presenter)
                .onForgetDataForLowMemory();

        presenter.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
        verify(presenter, never()).onForgetDataForLowMemory();

        presenter.onContainerResumed();
        verify(presenter, never()).onReloadForgottenData();
    }
}
