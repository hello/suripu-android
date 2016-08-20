package is.hello.sense.interactors;

import android.os.Bundle;

import org.junit.After;
import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class InteractorContainerTests extends SenseTestCase {
    private final InteractorContainer interactorContainer = new InteractorContainer();

    @After
    public void tearDown() {
        interactorContainer.interactors.clear();
    }

    @Test
    public void onContainerDestroyed() {
        Interactor interactor1 = mock(Interactor.class);
        doNothing()
                .when(interactor1)
                .onContainerDestroyed();
        interactorContainer.addInteractor(interactor1);

        Interactor interactor2 = mock(Interactor.class);
        doNothing()
                .when(interactor2)
                .onContainerDestroyed();
        interactorContainer.addInteractor(interactor2);

        interactorContainer.onContainerDestroyed();

        verify(interactor1).onContainerDestroyed();
        verify(interactor2).onContainerDestroyed();
    }

    @Test
    public void onTrimMemory() {
        Interactor interactor1 = mock(Interactor.class);
        doNothing()
                .when(interactor1)
                .onTrimMemory(Interactor.BASE_TRIM_LEVEL);
        interactorContainer.addInteractor(interactor1);

        Interactor interactor2 = mock(Interactor.class);
        doNothing()
                .when(interactor2)
                .onTrimMemory(Interactor.BASE_TRIM_LEVEL);
        interactorContainer.addInteractor(interactor2);

        interactorContainer.onTrimMemory(Interactor.BASE_TRIM_LEVEL);

        verify(interactor1).onTrimMemory(Interactor.BASE_TRIM_LEVEL);
        verify(interactor2).onTrimMemory(Interactor.BASE_TRIM_LEVEL);
    }

    @Test
    public void onContainerResumed() {
        Interactor interactor1 = mock(Interactor.class);
        doNothing()
                .when(interactor1)
                .onContainerResumed();
        interactorContainer.addInteractor(interactor1);

        Interactor interactor2 = mock(Interactor.class);
        doNothing()
                .when(interactor2)
                .onContainerResumed();
        interactorContainer.addInteractor(interactor2);

        interactorContainer.onContainerResumed();

        verify(interactor1).onContainerResumed();
        verify(interactor2).onContainerResumed();
    }

    @Test
    public void saveState() {
        Interactor withState = mock(Interactor.class, CALLS_REAL_METHODS);

        Bundle testState = new Bundle();
        testState.putString("something", "wonderful");
        testState.putInt("meaningOfLife", 42);
        doReturn(testState)
                .when(withState)
                .onSaveState();
        doReturn("withState")
                .when(withState)
                .getSavedStateKey();
        interactorContainer.addInteractor(withState);

        Interactor withoutState = mock(Interactor.class, CALLS_REAL_METHODS);
        doReturn(null)
                .when(withoutState)
                .onSaveState();
        doReturn("withoutState")
                .when(withoutState)
                .getSavedStateKey();
        interactorContainer.addInteractor(withoutState);


        Bundle savedState = new Bundle();
        interactorContainer.onSaveState(savedState);

        verify(withState).onSaveState();
        assertThat(savedState.keySet(), hasItem("withState"));
        verify(withoutState).onSaveState();
        assertThat(savedState.keySet(), not(hasItem("withoutState")));


        interactorContainer.onRestoreState(savedState);

        verify(withState).onRestoreState(any(Bundle.class));
        verify(withoutState, never()).onRestoreState(any(Bundle.class));
    }
}
