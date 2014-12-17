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
import is.hello.sense.ui.common.ObservableContainer;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public abstract class WidgetService extends Service implements ObservableContainer {
    public static final String EXTRA_WIDGET_IDS = WidgetService.class.getName() + ".EXTRA_WIDGET_IDS";

    private final List<Subscription> subscriptions = new ArrayList<>();
    private final PresenterContainer presenterContainer = new PresenterContainer();

    //region Lifecycle

    public WidgetService() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        presenterContainer.onTrimMemory(level);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        presenterContainer.onContainerDestroyed();

        for (Subscription subscription : subscriptions) {
            if (!subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
        }

        subscriptions.clear();
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

    public void addPresenter(@NonNull Presenter presenter) {
        presenterContainer.addPresenter(presenter);
    }


    //endregion


    //region Observable Container

    @Override
    public boolean hasSubscriptions() {
        return !subscriptions.isEmpty();
    }

    @Override
    public @NonNull Subscription track(@NonNull Subscription subscription) {
        subscriptions.add(subscription);
        return subscription;
    }

    @NonNull
    @Override
    public <T> Observable<T> bind(@NonNull Observable<T> toBind) {
        return toBind.observeOn(AndroidSchedulers.mainThread())
                     .take(1);
    }

    @NonNull
    @Override
    public <T> Subscription subscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return track(toSubscribe.subscribe(onNext, onError));
    }

    @NonNull
    @Override
    public <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return subscribe(bind(toSubscribe), onNext, onError);
    }


    //endregion
}
