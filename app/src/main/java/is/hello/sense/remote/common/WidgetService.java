package is.hello.sense.remote.common;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.SenseApplication;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.util.Logger;

public abstract class WidgetService extends Service implements PresenterContainer {
    public static final String EXTRA_WIDGET_IDS = WidgetService.class.getName() + ".EXTRA_WIDGET_IDS";

    private final List<Presenter> presenters = new ArrayList<>();

    //region Lifecycle

    public WidgetService() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        for (Presenter presenter : presenters) {
            presenter.onTrimMemory(level);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (Presenter presenter : presenters) {
            presenter.onContainerDestroyed();
        }
    }

    //endregion


    //region Service

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.info(getClass().getSimpleName(), "Updating widget");
        int[] widgetId = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
        startUpdate(widgetId);
        return START_STICKY;
    }

    protected abstract void startUpdate(int widgetIds[]);

    protected final void publishUpdate(int widgetIds[], @NonNull RemoteViews remoteViews) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(widgetIds, remoteViews);

        Logger.info(getClass().getSimpleName(), "Widget update completed");

        stopSelf();
    }

    //endregion


    //region Presenter Container

    @Override
    public void addPresenter(@NonNull Presenter presenter) {
        presenters.remove(presenter);
    }

    @Override
    public void removePresenter(@NonNull Presenter presenter) {
        presenters.remove(presenter);
    }

    @NonNull
    @Override
    public List<Presenter> getPresenters() {
        return presenters;
    }

    //endregion
}
