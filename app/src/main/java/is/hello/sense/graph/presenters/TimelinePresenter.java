package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

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

    private final DateTime date;

    public final ReplaySubject<List<Timeline>> timeline = ReplaySubject.create(1);
    public final Observable<Timeline> mainTimeline = timeline.filter(timelines -> !timelines.isEmpty())
                                                             .map(timelines -> timelines.get(0));
    public final Observable<CharSequence> renderedTimelineMessage = mainTimeline.map(timeline -> {
        String rawMessage = timeline.getMessage();
        return markdown.toSpanned(rawMessage);
    });

    public TimelinePresenter(@NonNull DateTime date) {
        super();

        this.date = date;

        update();
    }

    @Override
    public void update() {
        Observable<List<Timeline>> update = service.timelineForDate(date.year().getAsString(),
                                                                    date.monthOfYear().getAsString(),
                                                                    date.dayOfMonth().getAsString());
        update.subscribe(timeline::onNext, timeline::onError);
    }
}
