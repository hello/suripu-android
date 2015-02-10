package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Feedback;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.Markdown;
import rx.Observable;

public class TimelinePresenter extends Presenter {
    @Inject Markdown markdown;
    @Inject ApiService service;

    private DateTime date;

    public final PresenterSubject<ArrayList<Timeline>> timeline = PresenterSubject.create();
    public final Observable<Timeline> mainTimeline = timeline.map(timelines -> {
        if (Lists.isEmpty(timelines)) {
            return null;
        } else {
            return timelines.get(0);
        }
    });
    public final Observable<CharSequence> renderedTimelineMessage = mainTimeline.map(timeline -> {
        if (timeline != null) {
            String rawMessage = timeline.getMessage();
            return markdown.toSpanned(rawMessage);
        } else {
            return "";
        }
    });

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        timeline.onNext(new ArrayList<>());
        return true;
    }

    public void update() {
        if (getDate() != null) {
            logEvent("updating timeline for " + date.toString("yyyy-MM-dd"));
            Observable<ArrayList<Timeline>> update = service.timelineForDate(date.year().getAsString(),
                                                                             date.monthOfYear().getAsString(),
                                                                             date.dayOfMonth().getAsString());
            update.subscribe(timeline);
        }
    }


    public DateTime getDate() {
        return date;
    }

    public void setDateWithTimeline(@NonNull DateTime date, @Nullable Timeline timeline) {
        this.date = date;
        if (timeline != null) {
            this.timeline.onNext(Lists.newArrayList(timeline));
        } else {
            update();
        }
    }

    public Observable<VoidResponse> submitCorrection(@NonNull Feedback correction) {
        return service.submitCorrect(correction)
                      .doOnCompleted(this::update);
    }
}
