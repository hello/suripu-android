package is.hello.sense.graph.presenters;

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

public class PresenterContainerTests extends SenseTestCase {
    private final PresenterContainer presenterContainer = new PresenterContainer();

    @After
    public void tearDown() {
        presenterContainer.presenters.clear();
    }

    @Test
    public void onContainerDestroyed() {
        Presenter presenter1 = mock(Presenter.class);
        doNothing()
                .when(presenter1)
                .onContainerDestroyed();
        presenterContainer.addPresenter(presenter1);

        Presenter presenter2 = mock(Presenter.class);
        doNothing()
                .when(presenter2)
                .onContainerDestroyed();
        presenterContainer.addPresenter(presenter2);

        presenterContainer.onContainerDestroyed();

        verify(presenter1).onContainerDestroyed();
        verify(presenter2).onContainerDestroyed();
    }

    @Test
    public void onTrimMemory() {
        Presenter presenter1 = mock(Presenter.class);
        doNothing()
                .when(presenter1)
                .onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        presenterContainer.addPresenter(presenter1);

        Presenter presenter2 = mock(Presenter.class);
        doNothing()
                .when(presenter2)
                .onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        presenterContainer.addPresenter(presenter2);

        presenterContainer.onTrimMemory(Presenter.BASE_TRIM_LEVEL);

        verify(presenter1).onTrimMemory(Presenter.BASE_TRIM_LEVEL);
        verify(presenter2).onTrimMemory(Presenter.BASE_TRIM_LEVEL);
    }

    @Test
    public void onContainerResumed() {
        Presenter presenter1 = mock(Presenter.class);
        doNothing()
                .when(presenter1)
                .onContainerResumed();
        presenterContainer.addPresenter(presenter1);

        Presenter presenter2 = mock(Presenter.class);
        doNothing()
                .when(presenter2)
                .onContainerResumed();
        presenterContainer.addPresenter(presenter2);

        presenterContainer.onContainerResumed();

        verify(presenter1).onContainerResumed();
        verify(presenter2).onContainerResumed();
    }

    @Test
    public void saveState() {
        Presenter withState = mock(Presenter.class, CALLS_REAL_METHODS);

        Bundle testState = new Bundle();
        testState.putString("something", "wonderful");
        testState.putInt("meaningOfLife", 42);
        doReturn(testState)
                .when(withState)
                .onSaveState();
        doReturn("withState")
                .when(withState)
                .getSavedStateKey();
        presenterContainer.addPresenter(withState);

        Presenter withoutState = mock(Presenter.class, CALLS_REAL_METHODS);
        doReturn(null)
                .when(withoutState)
                .onSaveState();
        doReturn("withoutState")
                .when(withoutState)
                .getSavedStateKey();
        presenterContainer.addPresenter(withoutState);


        Bundle savedState = new Bundle();
        presenterContainer.onSaveState(savedState);

        verify(withState).onSaveState();
        assertThat(savedState.keySet(), hasItem("withState"));
        verify(withoutState).onSaveState();
        assertThat(savedState.keySet(), not(hasItem("withoutState")));


        presenterContainer.onRestoreState(savedState);

        verify(withState).onRestoreState(any(Bundle.class));
        verify(withoutState, never()).onRestoreState(any(Bundle.class));
    }
}
