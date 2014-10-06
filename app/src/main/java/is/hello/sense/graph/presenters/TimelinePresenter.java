package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import org.joda.time.DateTime;
import org.markdownj.MarkdownProcessor;

import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Timeline;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class TimelinePresenter implements Presenter {
    public final ReplaySubject<List<Timeline>> timeline = ReplaySubject.create(1);
    public final Observable<Timeline> mainTimeline = timeline.filter(timelines -> !timelines.isEmpty())
                                                             .map(timelines -> timelines.get(0));
    public final Observable<CharSequence> renderedTimelineMessage = mainTimeline.map(timeline -> {
        String rawMessage = timeline.getMessage();
        String markdown = new MarkdownProcessor().markdown(rawMessage);
        Spanned html = Html.fromHtml(markdown);
        return html.subSequence(0, TextUtils.getTrimmedLength(html));
    });

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
