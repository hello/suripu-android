package is.hello.sense.presenters.selectwifinetwork;

import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter.Output;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SettingsSelectWifiNetworkPresenterTest extends InjectionTestCase {
    @Inject
    SettingsSelectWifiNetworkPresenter presenter;

    @Test
    public void shouldUseToolBar() throws Exception {
        assertThat(presenter.shouldUseToolBar(), equalTo(false));
    }

    @Test
    public void onBackPressed() throws Exception {
        final Runnable mockRunnable = mock(Runnable.class);
        final Output mockOutput = getMockOutput(false);
        presenter.setView(mockOutput);
        final boolean intercepted = presenter.onBackPressed(mockRunnable);
        assertThat(intercepted, equalTo(true));
        verify(mockOutput, times(1)).cancelFlow();
        verify(mockRunnable, times(0)).run();
    }

    @Test
    public void onBackPressedWithViewAndScanning() throws Exception {
        final Runnable mockRunnable = mock(Runnable.class);
        final Output mockOutput = getMockOutput(true);
        presenter.setView(mockOutput);
        final boolean intercepted = presenter.onBackPressed(mockRunnable);
        assertThat(intercepted, equalTo(true));
        verify(mockOutput, times(0)).cancelFlow();
        verify(mockRunnable, times(0)).run();
    }

    private Output getMockOutput(final boolean isScanning) {
        final Output mockOutput = mock(Output.class);
        doReturn(true).when(mockOutput).isResumed();
        doReturn(true).when(mockOutput).canObservableEmit();
        doReturn(isScanning).when(mockOutput).isScanning();
        return mockOutput;
    }
}