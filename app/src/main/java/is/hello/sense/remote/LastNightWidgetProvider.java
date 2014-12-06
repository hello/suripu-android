package is.hello.sense.remote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.widget.RemoteViews;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.remote.common.WidgetService;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.common.Styles;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class LastNightWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, LastNightService.class).putExtra(WidgetService.EXTRA_WIDGET_IDS, appWidgetIds));
    }


    public static class LastNightService extends WidgetService {
        @Inject TimelinePresenter presenter;

        public LastNightService() {
            presenter.setDate(DateFormatter.lastNight());
            addPresenter(presenter);
        }

        @Override
        protected void startUpdate(int widgetIds[]) {
            Observable<Pair<Timeline, CharSequence>> update = Observable.combineLatest(presenter.mainTimeline.take(1), presenter.renderedTimelineMessage.take(1), Pair::new);
            bindAndSubscribe(update,
                             r -> bindConditions(widgetIds, r),
                             e -> {
                                 Logger.error(LastNightWidgetProvider.class.getSimpleName(), "Could not fetch last night's timeline", e);
                                 bindConditions(widgetIds, null);
                             });
        }

        private void bindConditions(int widgetIds[], @Nullable Pair<Timeline, CharSequence> result) {
            Resources resources = getResources();
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_last_night);
            if (result != null) {
                Timeline timeline = result.first;
                int sleepScore = timeline.getScore();
                remoteViews.setTextViewText(R.id.widget_last_night_score, Integer.toString(sleepScore));
                remoteViews.setTextColor(R.id.widget_last_night_score, resources.getColor(Styles.getSleepScoreColorRes(sleepScore)));

                CharSequence message = result.second;
                remoteViews.setTextViewText(R.id.widget_last_night_message, message);
            } else {
                remoteViews.setTextViewText(R.id.widget_last_night_score, getString(R.string.missing_data_placeholder));
                remoteViews.setTextViewText(R.id.widget_last_night_message, getString(R.string.missing_data_placeholder));
            }

            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent onClick = PendingIntent.getActivity(this, 12, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_last_night, onClick);

            publishUpdate(widgetIds, remoteViews);
        }
    }
}
