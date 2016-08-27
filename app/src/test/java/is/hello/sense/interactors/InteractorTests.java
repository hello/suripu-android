package is.hello.sense.interactors;

import android.content.ComponentCallbacks2;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class InteractorTests extends SenseTestCase {
    @Test
    public void forgetsDataWhenAppropriate() {
        Interactor interactor = mock(Interactor.class, CALLS_REAL_METHODS);
        doReturn(true)
                .when(interactor)
                .onForgetDataForLowMemory();

        interactor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
        verify(interactor).onForgetDataForLowMemory();

        interactor.onContainerResumed();
        verify(interactor).onReloadForgottenData();
    }

    @Test
    public void ignoresUnnecessaryTrimCalls() {
        Interactor interactor = mock(Interactor.class, CALLS_REAL_METHODS);
        doReturn(false)
                .when(interactor)
                .onForgetDataForLowMemory();

        interactor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
        verify(interactor, never()).onForgetDataForLowMemory();

        interactor.onContainerResumed();
        verify(interactor, never()).onReloadForgottenData();
    }
}
