package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import com.hello.ble.protobuf.MorpheusBle;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.Fixtures;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

public class HardwarePresenterTests extends InjectionTestCase {
    private static final int SCAN_COUNT = 2;

    @Inject HardwarePresenter hardwarePresenter;

    public void testMultiScan() throws Exception {
        Observable<List<MorpheusBle.wifi_endpoint>> scan = hardwarePresenter.scanForWifiNetworks(SCAN_COUNT);
        SyncObserver<List<MorpheusBle.wifi_endpoint>> observer = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, scan);
        observer.await();

        assertNull(observer.getError());
        assertFalse(observer.getResults().isEmpty());
        assertEquals(1, observer.getResults().size());
    }

    public static class StubbedHardwarePresenter extends HardwarePresenter {
        public boolean scanForWifiNetworksCalled = false;

        public StubbedHardwarePresenter(@NonNull PreferencesPresenter preferencesPresenter,
                                        @NonNull ApiSessionManager apiSessionManager) {
            super(preferencesPresenter, apiSessionManager);
        }

        @Override
        public Observable<List<MorpheusBle.wifi_endpoint>> scanForWifiNetworks() {
            Observable<List<MorpheusBle.wifi_endpoint>> result;
            if (scanForWifiNetworksCalled) {
                result = Observable.just(Arrays.asList(Fixtures.SCAN_RESULTS_2));
            } else {
                result = Observable.just(Arrays.asList(Fixtures.SCAN_RESULTS_1));
            }
            this.scanForWifiNetworksCalled = true;
            return result;
        }
    }
}
