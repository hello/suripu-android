package is.hello.sense.remote;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.Logger;
import rx.android.schedulers.AndroidSchedulers;

public class RoomConditionsWidgetUpdateService extends Service {
    @Inject CurrentConditionsPresenter presenter;

    public RoomConditionsWidgetUpdateService() {
        SenseApplication.getInstance().inject(this);
        presenter.update();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.info(RoomConditionsWidgetUpdateService.class.getSimpleName(), "Updating widget");

        presenter.currentConditions
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(this::bindConditions, e -> {
                     Logger.error(RoomConditionsWidgetUpdateService.class.getSimpleName(), "Could not update current conditions", e);
                     bindConditions(null);
                 });

        return START_STICKY;
    }

    private void bindConditions(@Nullable CurrentConditionsPresenter.Result results) {
        Resources resources = getResources();
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_current_conditions);
        if (results != null) {

            RoomConditions conditions = results.conditions;
            UnitSystem unitSystem = results.units;

            Logger.info(RoomConditionsWidgetUpdateService.class.getSimpleName(), "Updating with conditions " + conditions);

            SensorState temperature = conditions.getTemperature();
            remoteViews.setTextViewText(R.id.app_widget_temperature, temperature.getFormattedValue(unitSystem::formatTemperature));
            remoteViews.setTextColor(R.id.app_widget_temperature, resources.getColor(temperature.getCondition().colorRes));

            SensorState humidity = conditions.getHumidity();
            remoteViews.setTextViewText(R.id.app_widget_humidity, humidity.getFormattedValue(null));
            remoteViews.setTextColor(R.id.app_widget_humidity, resources.getColor(humidity.getCondition().colorRes));

            SensorState particulates = conditions.getParticulates();
            remoteViews.setTextViewText(R.id.app_widget_particulates, particulates.getFormattedValue(unitSystem::formatParticulates));
            remoteViews.setTextColor(R.id.app_widget_particulates, resources.getColor(particulates.getCondition().colorRes));
        } else {
            int unknownColor = resources.getColor(R.color.sensor_unknown);

            remoteViews.setTextViewText(R.id.app_widget_temperature, getString(R.string.missing_data_placeholder));
            remoteViews.setTextColor(R.id.app_widget_temperature, unknownColor);

            remoteViews.setTextViewText(R.id.app_widget_humidity, getString(R.string.missing_data_placeholder));
            remoteViews.setTextColor(R.id.app_widget_humidity, unknownColor);

            remoteViews.setTextViewText(R.id.app_widget_particulates, getString(R.string.missing_data_placeholder));
            remoteViews.setTextColor(R.id.app_widget_particulates, unknownColor);
        }
        publishUpdate(remoteViews);
    }

    private void publishUpdate(@NonNull RemoteViews updateViews) {
        ComponentName thisWidget = new ComponentName(this, RoomConditionsWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(thisWidget, updateViews);

        Logger.info(RoomConditionsWidgetUpdateService.class.getSimpleName(), "Widget update completed");

        stopSelf();
    }
}
