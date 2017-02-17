package is.hello.sense.flows.notification.interactors;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.NotificationSetting;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

public class NotificationSettingsInteractor extends ValueInteractor<ArrayList<NotificationSetting>> {

    @Inject
    ApiService apiService;

    public final InteractorSubject<ArrayList<NotificationSetting>> notificationSettings = subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<NotificationSetting>> provideUpdateObservable() {
        return apiService.getNotificationSettings();
    }

    public Observable<VoidResponse> updateNotificationSettings(@NonNull final List<NotificationSetting> settings) {
        return apiService.putNotificationSettings(settings);
    }
}
