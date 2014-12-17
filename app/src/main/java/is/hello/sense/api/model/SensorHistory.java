package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import is.hello.sense.functional.Function;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.sense.functional.Lists.map;
import static is.hello.sense.functional.Lists.segment;

public class SensorHistory extends ApiResponse {
    public static final String SENSOR_NAME_TEMPERATURE = "temperature";
    public static final String SENSOR_NAME_HUMIDITY = "humidity";
    public static final String SENSOR_NAME_PARTICULATES = "particulates";

    @JsonProperty("value")
    private float value;

    @JsonProperty("datetime")
    private DateTime time;

    @JsonProperty("offset_millis")
    private long offset;


    public float getValue() {
        return value;
    }

    public DateTime getTime() {
        return time;
    }

    public long getOffset() {
        return offset;
    }


    @Override
    public String toString() {
        return "SensorHistory{" +
                "value=" + value +
                ", time=" + time +
                ", offset=" + offset +
                '}';
    }


    //region Time Zone Fun

    /**
     * Returns the user's current time, in the UTC timezone. For use with sensor history.
     */
    public static long timeForLatest() {
        DateTime now = DateTime.now();
        DateTime nowUTC = new DateTime(
                now.getYear(),
                now.getMonthOfYear(),
                now.getDayOfMonth(),
                now.getHourOfDay(),
                now.getMinuteOfHour(),
                now.getSecondOfMinute(),
                DateTimeZone.UTC
        );
        return nowUTC.getMillis();
    }

    //endregion


    //region Generating Graphs

    public static Observable<SensorHistoryAdapter.Update> createAdapterUpdate(@NonNull List<SensorHistory> history, int mode, @NonNull DateTimeZone timeZone) {
        return Observable.create((Observable.OnSubscribe<SensorHistoryAdapter.Update>) s -> {
            Function<SensorHistory, Integer> segmentKeyProducer;
            if (mode == SensorHistoryPresenter.MODE_WEEK) {
                segmentKeyProducer = sensorHistory -> sensorHistory.getTime().withZone(timeZone).getDayOfMonth();
            } else {
                segmentKeyProducer = sensorHistory -> {
                    DateTime shiftedTime = sensorHistory.getTime().withZone(timeZone);
                    return (shiftedTime.getDayOfMonth() * 100) + (shiftedTime.getHourOfDay() / 6);
                };
            }
            List<List<SensorHistory>> segments = segment(segmentKeyProducer, history);
            List<SensorHistoryAdapter.Section> sections = map(segments, SensorHistoryAdapter.Section::new);

            Comparator<SensorHistory> comparator = (l, r) -> Float.compare(r.getValue(), l.getValue());
            float peak = Collections.max(history, comparator).getValue();
            float base = Collections.min(history, comparator).getValue();

            s.onNext(new SensorHistoryAdapter.Update(sections, peak, base));
            s.onCompleted();
        }).subscribeOn(Schedulers.computation());
    }

    //endregion
}
