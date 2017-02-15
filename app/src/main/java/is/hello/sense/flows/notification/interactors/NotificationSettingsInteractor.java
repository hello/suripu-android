package is.hello.sense.flows.notification.interactors;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.NotificationSetting;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

public class NotificationSettingsInteractor extends ValueInteractor<ArrayList<NotificationSetting>> {

    @Inject
    ApiService apiService;

    public final InteractorSubject<ArrayList<NotificationSetting>> notificationSettings = subject;

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<NotificationSetting>> provideUpdateObservable() {
        return apiService.getNotificationSettings();
    }
}
