package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineDate;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class TimelinePresenter implements Presenter {
    public final ReplaySubject<List<Timeline>> timeline = ReplaySubject.create(1);

    private final ApiService service;
    private final TimelineDate date;

    public TimelinePresenter(@NonNull ApiService service, @NonNull TimelineDate date) {
        this.service = service;
        this.date = date;

        update();
    }

    @Override
    public void update() {
        Observable<List<Timeline>> update = service.timelineForDate(date.month, date.day, date.year);
        update.subscribe(timeline::onNext, timeline::onError);
    }
}
