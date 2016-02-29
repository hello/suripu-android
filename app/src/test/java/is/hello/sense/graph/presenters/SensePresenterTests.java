package is.hello.sense.graph.presenters;

import android.annotation.SuppressLint;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.sense.graph.InjectionTestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SuppressLint("CommitPrefEdits")

public class SensePresenterTests extends InjectionTestCase {
    private static final String FAKE_ADDRESS = "00:DE:AD:BE:EF:00";

    @Inject PreferencesPresenter preferences;
    @Inject SensePresenter sensePresenter;

    @Before
    public void setUp() throws Exception {
        preferences.edit()
                   .clear()
                   .commit();
    }

    @Test
    public void shouldPromptForHighPowerScan() {
        assertThat(sensePresenter.shouldPromptForHighPowerScan(), is(false));

        for (int i = 0; i < SensePresenter.FAILS_BEFORE_HIGH_POWER; i++) {
            sensePresenter.trackPeripheralNotFound();
        }

        assertThat(sensePresenter.shouldPromptForHighPowerScan(), is(true));
    }

    @Test
    public void lastAddress() {
        assertThat(sensePresenter.getLastAddress(), is(nullValue()));

        sensePresenter.setLastAddress("00:00:00:00:00");
        assertThat(sensePresenter.getLastAddress(), is(equalTo("00:00:00:00:00")));
    }

    @Test
    public void hasPeripheral() {
        assertThat(sensePresenter.hasPeripheral(), is(false));

        sensePresenter.peripheral.onNext(mock(GattPeripheral.class));

        assertThat(sensePresenter.hasPeripheral(), is(true));
    }

    @Test
    public void isDisconnectIntentForSense() {
        final Intent matchingIntent = new Intent()
                .putExtra(GattPeripheral.EXTRA_ADDRESS, FAKE_ADDRESS);

        assertThat(sensePresenter.isDisconnectIntentForSense(matchingIntent), is(false));

        final GattPeripheral peripheral = mock(GattPeripheral.class);
        doReturn(FAKE_ADDRESS).when(peripheral).getAddress();
        sensePresenter.peripheral.onNext(peripheral);

        assertThat(sensePresenter.isDisconnectIntentForSense(matchingIntent), is(true));

        final Intent notMatchingIntent = new Intent()
                .putExtra(GattPeripheral.EXTRA_ADDRESS, "00:CA:FE:BA:BE:00");
        assertThat(sensePresenter.isDisconnectIntentForSense(notMatchingIntent), is(false));
    }
}
