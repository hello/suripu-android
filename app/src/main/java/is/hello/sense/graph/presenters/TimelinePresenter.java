package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class TimelinePresenter implements Presenter {
    public final ReplaySubject<List<Timeline>> timeline = ReplaySubject.create(1);

    private final ApiService service;
    private final DateTime date;

    public TimelinePresenter(@NonNull ApiService service, @NonNull DateTime date) {
        this.service = service;
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
