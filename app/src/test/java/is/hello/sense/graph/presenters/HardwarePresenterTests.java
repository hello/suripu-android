package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.errors.GattException;
import is.hello.buruberi.util.Operation;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import rx.Observable;

import static is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint;
import static is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint.sec_type;
import static is.hello.sense.AssertExtensions.assertNoThrow;
import static is.hello.sense.AssertExtensions.assertThrows;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HardwarePresenterTests extends InjectionTestCase {
    @Inject HardwarePresenter presenter;

    @Test
    public void errorsResetPeripheral() throws Exception {
        final SensePeripheral peripheral = mock(SensePeripheral.class);
        //noinspection ResourceType
        doReturn(Observable.error(new GattException(GattException.GATT_STACK_ERROR,
                                                    Operation.ENABLE_NOTIFICATION)))
                .when(peripheral)
                .getWifiNetwork();
        presenter.peripheral = peripheral;

        assertThrows(() -> Sync.last(presenter.currentWifiNetwork()));
        assertThat(presenter.peripheral, is(nullValue()));
    }

    @Test
    public void clearsPeripheralOnBluetoothDisable() throws Exception {
        presenter.peripheral = mock(SensePeripheral.class);
        presenter.onBluetoothEnabledChanged(true);
        assertThat(presenter.peripheral, is(notNullValue()));

        presenter.onBluetoothEnabledChanged(false);
        assertThat(presenter.peripheral, is(nullValue()));
    }

    @Test
    public void connectivityGetters() throws Exception {
        final SensePeripheral peripheral = mock(SensePeripheral.class);
        doReturn(true).when(peripheral).isConnected();
        presenter.peripheral = peripheral;

        assertThat(presenter.hasPeripheral(), is(true));
        assertThat(presenter.isConnected(), is(true));

        doReturn(false).when(peripheral).isConnected();

        assertThat(presenter.isConnected(), is(false));

        presenter.peripheral = null;

        assertThat(presenter.hasPeripheral(), is(false));
        assertThat(presenter.isConnected(), is(false));
    }

    @Test
    public void noDeviceErrors() throws Exception {
        presenter.peripheral = null;

        assertThrowsNoDeviceError(presenter.connectToPeripheral());
        assertThrowsNoDeviceError(presenter.runLedAnimation(SenseLedAnimation.STOP));
        assertThrowsNoDeviceError(presenter.scanForWifiNetworks(false));
        assertThrowsNoDeviceError(presenter.currentWifiNetwork());
        assertThrowsNoDeviceError(presenter.sendWifiCredentials("1234", sec_type.SL_SCAN_SEC_TYPE_OPEN, ""));
        assertThrowsNoDeviceError(presenter.linkAccount());
        assertThrowsNoDeviceError(presenter.linkPill());
        assertThrowsNoDeviceError(presenter.pushData());
        assertThrowsNoDeviceError(presenter.putIntoPairingMode());
        assertThrowsNoDeviceError(presenter.factoryReset(new SenseDevice(BaseDevice.State.UNKNOWN,
                                                                         SenseDevice.Color.UNKNOWN,
                                                                         "", "", null, null)));
    }

    @Test
    public void wifiSignalStrengthSort() throws Exception {
        final List<wifi_endpoint> endpoints = new ArrayList<>();
        endpoints.add(wifi_endpoint.newBuilder()
                                   .setSecurityType(sec_type.SL_SCAN_SEC_TYPE_OPEN)
                                   .setSsid("Test 1")
                                   .setRssi(-1000).build());
        endpoints.add(wifi_endpoint.newBuilder()
                                   .setSecurityType(sec_type.SL_SCAN_SEC_TYPE_OPEN)
                                   .setSsid("Test 2")
                                   .setRssi(-50)
                                   .build());
        endpoints.add(wifi_endpoint.newBuilder()
                                   .setSecurityType(sec_type.SL_SCAN_SEC_TYPE_OPEN)
                                   .setSsid("Test 3")
                                   .setRssi(-4000)
                                   .build());

        presenter.sortWifiNetworks(endpoints);
        final List<String> endpointNames = Lists.map(endpoints, wifi_endpoint::getSsid);
        assertThat(endpointNames, hasItems("Test 2", "Test 1", "Test 3"));

        assertNoThrow(() -> presenter.sortWifiNetworks(Lists.newArrayList()));
    }

    @Test
    public void clearPeripheral() throws Exception {
        final SensePeripheral peripheral = mock(SensePeripheral.class);
        doReturn(true)
                .when(peripheral)
                .isConnected();
        //noinspection ResourceType
        doReturn(Observable.just(peripheral))
                .when(peripheral)
                .disconnect();

        presenter.peripheral = peripheral;

        assertThat(presenter.hasPeripheral(), is(true));
        assertThat(presenter.isConnected(), is(true));

        presenter.clearPeripheral();

        assertThat(presenter.hasPeripheral(), is(false));
        assertThat(presenter.isConnected(), is(false));

        //noinspection ResourceType
        verify(peripheral, atLeastOnce()).disconnect();
    }


    private static <T> void assertThrowsNoDeviceError(@NonNull Observable<T> observable) {
        try {
            Sync.last(observable);
            fail("Expected " + HardwarePresenter.NoConnectedPeripheralException.class.getCanonicalName());
        } catch (Exception e) {
            assertThat(e, is(instanceOf(HardwarePresenter.NoConnectedPeripheralException.class)));
        }
    }
}
