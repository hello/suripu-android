package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import org.joda.time.LocalDateTime;

import javax.inject.Inject;

import is.hello.sense.SenseApplication;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.util.DateFormatter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public final class TimestampTextView extends TextView {
    @Inject PreferencesPresenter preferencesPresenter;
    @Inject DateFormatter dateFormatter;

    private Subscription subscription;
    private boolean use24HourTime = false;
    private LocalDateTime dateTime = null;

    @SuppressWarnings("UnusedDeclaration")
    public TimestampTextView(Context context) {
        super(context);
        initialize();
    }

    @SuppressWarnings("UnusedDeclaration")
    public TimestampTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    @SuppressWarnings("UnusedDeclaration")
    public TimestampTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Observable<Boolean> use24HourTime = preferencesPresenter.observableBoolean(PreferencesPresenter.USE_24_TIME, false)
                                                                .subscribeOn(AndroidSchedulers.mainThread());
        this.subscription = use24HourTime.subscribe(this::setUse24HourTime);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (this.subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            this.subscription = null;
        }
    }


    //region Properties

    protected void update() {
        setText(dateFormatter.formatAsTime(getDateTime(), getUse24HourTime()));
    }

    public boolean getUse24HourTime() {
        return use24HourTime;
    }

    public void setUse24HourTime(boolean use24HourTime) {
        if (this.use24HourTime == use24HourTime)
            return;

        this.use24HourTime = use24HourTime;
        update();
    }

    public @Nullable LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(@Nullable LocalDateTime dateTime) {
        this.dateTime = dateTime;
        update();
    }

    //endregion
}
