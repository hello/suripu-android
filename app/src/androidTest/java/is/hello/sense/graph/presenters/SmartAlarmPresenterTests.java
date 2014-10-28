package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.graph.annotations.CacheDirectoryFile;
import is.hello.sense.util.SyncObserver;
import rx.Observable;

@SuppressWarnings("ConstantConditions")
public class SmartAlarmPresenterTests extends InjectionTestCase {
    @Inject SmartAlarmPresenter smartAlarmPresenter;

    public void testUpdate() throws Exception {
        SyncObserver<List<SmartAlarm>> smartAlarms = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, smartAlarmPresenter.alarms);
        smartAlarms.ignore(1);

        smartAlarmPresenter.update();

        smartAlarms.await();

        assertNull(smartAlarms.getError());
        assertEquals(2, smartAlarms.getResults().size());
        assertEquals(1, smartAlarms.getLast().size());
    }

    public void testSave() throws Exception {
        List<SmartAlarm> alarms = Collections.emptyList();
        SyncObserver<ApiResponse> saveAlarms = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, smartAlarmPresenter.save(alarms));
        saveAlarms.await();

        assertNull(saveAlarms.getError());
        assertNotNull(saveAlarms.getSingle());
    }


    public static class StubedSmartAlarmPresenter extends SmartAlarmPresenter {
        public StubedSmartAlarmPresenter(@NonNull ApiService apiService,
                                         @CacheDirectoryFile @Nullable File cacheDirectory,
                                         @NonNull ObjectMapper objectMapper) {
            super(apiService, cacheDirectory, objectMapper);
        }


        @Nullable
        @Override
        public Observable<List<SmartAlarm>> retrieveCache() {
            return Observable.just(Collections.emptyList());
        }

        @Nullable
        @Override
        public Observable<List<SmartAlarm>> saveCache(@Nullable List<SmartAlarm> alarms) {
            return Observable.just(Collections.emptyList());
        }
    }
}
