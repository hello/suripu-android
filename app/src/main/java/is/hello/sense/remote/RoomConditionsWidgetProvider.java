package is.hello.sense.remote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.remote.common.WidgetService;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.Logger;
import rx.android.schedulers.AndroidSchedulers;

public class RoomConditionsWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, UpdateService.class).putExtra(WidgetService.EXTRA_WIDGET_IDS, appWidgetIds));
    }

    public static class UpdateService extends WidgetService {
        @Inject CurrentConditionsPresenter presenter;

        public UpdateService() {
            presenter.update();
            addPresenter(presenter);
        }
        
        @Override
        protected void startUpdate(int widgetIds[]) {
            presenter.currentConditions
                     .take(1)
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(r -> bindConditions(widgetIds, r), e -> {
                         Logger.error(UpdateService.class.getSimpleName(), "Could not update current conditions", e);
                         bindConditions(widgetIds, null);
                     });
        }

        private void bindConditions(int widgetIds[], @Nullable CurrentConditionsPresenter.Result results) {
            Resources resources = getResources();
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_current_conditions);
            if (results != null) {
                RoomConditions conditions = results.conditions;
                UnitSystem unitSystem = results.units;

                Logger.info(UpdateService.class.getSimpleName(), "Updating with conditions " + conditions);

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

            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent onClick = PendingIntent.getActivity(this, 9, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_current_conditions, onClick);

            publishUpdate(widgetIds, remoteViews);
        }
    }
}
