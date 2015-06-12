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
import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.remote.common.WidgetService;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class LastNightWidgetProvider extends AppWidgetProvider {
    private static final String WIDGET_NAME = "Last Night";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, LastNightService.class).putExtra(WidgetService.EXTRA_WIDGET_IDS, appWidgetIds));
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        Logger.info(getClass().getSimpleName(), "onEnabled()");
        Analytics.trackEvent(Analytics.Widgets.EVENT_CREATED, Analytics.createProperties(Analytics.Widgets.PROP_NAME, WIDGET_NAME));
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        Logger.info(getClass().getSimpleName(), "onDeleted()");
        Analytics.trackEvent(Analytics.Widgets.EVENT_DELETED, Analytics.createProperties(Analytics.Widgets.PROP_NAME, WIDGET_NAME));
    }

    public static class LastNightService extends WidgetService {
        @Inject TimelinePresenter presenter;

        public LastNightService() {
            presenter.setDateWithTimeline(DateFormatter.lastNight(), null);
            addPresenter(presenter);
        }

        @Override
        protected void startUpdate(int widgetIds[]) {
            bindAndSubscribe(presenter.timeline.take(1),
                             timeline -> bindConditions(widgetIds, timeline),
                             e -> {
                                 Logger.error(LastNightWidgetProvider.class.getSimpleName(), "Could not fetch last night's timeline", e);
                                 bindConditions(widgetIds, null);
                             });
        }

        private void bindConditions(int widgetIds[], @Nullable Timeline timeline) {
            Resources resources = getResources();
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_last_night);
            if (timeline != null) {
                int sleepScore = timeline.getScore();
                remoteViews.setTextViewText(R.id.widget_last_night_score, Integer.toString(sleepScore));
                remoteViews.setTextColor(R.id.widget_last_night_score, resources.getColor(Styles.getSleepScoreColorRes(sleepScore)));

                remoteViews.setTextViewText(R.id.widget_last_night_message, timeline.getMessage());
            } else {
                remoteViews.setTextViewText(R.id.widget_last_night_score, getString(R.string.missing_data_placeholder));
                remoteViews.setTextViewText(R.id.widget_last_night_message, getString(R.string.missing_data_placeholder));
            }

            Intent activityIntent = new Intent(this, HomeActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent onClick = PendingIntent.getActivity(this, 0, activityIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget_last_night, onClick);

            publishUpdate(widgetIds, remoteViews);
        }
    }
}
