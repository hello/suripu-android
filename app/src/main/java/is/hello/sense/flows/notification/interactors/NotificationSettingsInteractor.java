package is.hello.sense.flows.notification.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
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

    public Observable<VoidResponse> enableAll() {
        return latest().map(this::enableAll)
                       .flatMap(this::updateNotificationSettings);
    }

    public Observable<VoidResponse> disableAll() {
        return latest().map(this::disableAll)
                       .flatMap(this::updateNotificationSettings);
    }

    public boolean updateIfInvalid() {
        if (isInvalid(notificationSettings.getValue())) {
            update();
            return true;
        } else {
            return false;
        }
    }

    private List<NotificationSetting> enableAll(@Nullable final List<NotificationSetting> settings) {
        return updateAll(settings, true);
    }

    private List<NotificationSetting> disableAll(@Nullable final List<NotificationSetting> settings) {
        return updateAll(settings, false);
    }

    private List<NotificationSetting> updateAll(@Nullable final List<NotificationSetting> settings,
                                                final boolean enabled) {
        if (isInvalid(settings)) {
            throw new InvalidException();
        }
        for (final NotificationSetting setting : settings) {
            setting.setEnabled(enabled);
        }
        return settings;
    }

    private boolean isInvalid(@Nullable final List<NotificationSetting> settings) {
        return settings == null || settings.isEmpty();
    }

    static class InvalidException extends IllegalStateException
            implements Errors.Reporting {

        @Nullable
        @Override
        public String getContextInfo() {
            return "cannot push null or empty list of settings to server";
        }

        @NonNull
        @Override
        public StringRef getDisplayMessage() {
            return StringRef.from(R.string.notification_settings_generic_error_message);
        }
    }
}
