package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Feedback;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.Markdown;
import rx.Observable;

public class TimelinePresenter extends ValuePresenter<Timeline> {
    @Inject Markdown markdown;
    @Inject ApiService service;

    private DateTime date;

    public final PresenterSubject<Timeline> timeline = subject;
    public final Observable<CharSequence> message = timeline.map(timeline -> {
        if (timeline != null) {
            String rawMessage = timeline.getMessage();
            return markdown.toSpanned(rawMessage);
        } else {
            return "";
        }
    });

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return (getDate() != null);
    }

    @Override
    protected Observable<Timeline> provideUpdateObservable() {
        Observable<ArrayList<Timeline>> update = service.timelineForDate(date.year().getAsString(),
                                                                         date.monthOfYear().getAsString(),
                                                                         date.dayOfMonth().getAsString());
        return update.map(timelines -> {
            if (Lists.isEmpty(timelines)) {
                return null;
            } else {
                return timelines.get(0);
            }
        });
    }


    public DateTime getDate() {
        return date;
    }

    public void setDateWithTimeline(@NonNull DateTime date, @Nullable Timeline timeline) {
        this.date = date;
        if (timeline != null) {
            this.timeline.onNext(timeline);
        } else {
            update();
        }
    }

    public Observable<VoidResponse> submitCorrection(@NonNull Feedback correction) {
        return service.submitCorrect(correction)
                      .doOnCompleted(this::update);
    }
}
