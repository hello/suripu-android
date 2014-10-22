package is.hello.sense.graph.presenters;

import android.net.wifi.ScanResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;

public class WifiNetworkPresenterTests extends InjectionTestCase {
    @Inject WifiNetworkPresenter wifiNetworkPresenter;

    public void testGetScanResultSecurity() throws Exception {
        ScanResult securedScanResult = newScanResult();
    }


    private ScanResult newScanResult() {
        try {
            Constructor<ScanResult> constructor = ScanResult.class.getConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
