package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.util.Markdown;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class TimelinePresenter extends Presenter {
    @Inject Markdown markdown;
    @Inject ApiService service;

    private DateTime date;

    public final ReplaySubject<List<Timeline>> timeline = ReplaySubject.create(1);
    public final Observable<Timeline> mainTimeline = timeline.filter(timelines -> !timelines.isEmpty())
                                                             .map(timelines -> timelines.get(0));
    public final Observable<CharSequence> renderedTimelineMessage = mainTimeline.map(timeline -> {
        String rawMessage = timeline.getMessage();
        return markdown.toSpanned(rawMessage);
    });

    @Override
    public void update() {
        if (getDate() != null) {
            logEvent("updating timeline for " + date.toString("yyyy-MM-dd"));
            Observable<List<Timeline>> update = service.timelineForDate(date.year().getAsString(),
                                                                        date.monthOfYear().getAsString(),
                                                                        date.dayOfMonth().getAsString());
            update.subscribe(timeline::onNext, timeline::onError);
        }
    }


    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
        update();
    }
}
