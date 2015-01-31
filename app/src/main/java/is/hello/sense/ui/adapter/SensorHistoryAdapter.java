package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.functional.Function;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.widget.graphing.Extremes;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.sense.functional.Lists.map;
import static is.hello.sense.functional.Lists.segment;

public class SensorHistoryAdapter implements GraphAdapter {
    private final List<ChangeObserver> observers = new ArrayList<>();

    private List<Section> sections = Collections.emptyList();
    private float baseMagnitude = 0f;
    private float peakMagnitude = 0f;

    public void update(@NonNull Update update) {
        setBaseMagnitude(update.base);
        setPeakMagnitude(update.peak);
        setSections(update.sections);
    }

    public void setSections(@NonNull List<Section> sections) {
        this.sections = sections;
        notifyDataChanged();
    }

    public @NonNull Section getSection(int position) {
        return sections.get(position);
    }

    public void setBaseMagnitude(float baseMagnitude) {
        this.baseMagnitude = baseMagnitude;
    }

    public void setPeakMagnitude(float peakMagnitude) {
        this.peakMagnitude = peakMagnitude;
    }

    public void clear() {
        sections.clear();
        notifyDataChanged();
    }


    @Override
    public float getBaseMagnitude() {
        return baseMagnitude;
    }

    @Override
    public float getPeakMagnitude() {
        return peakMagnitude;
    }

    @Override
    public int getSectionCount() {
        return sections.size();
    }

    @Override
    public int getSectionPointCount(int section) {
        return sections.get(section).size();
    }

    @Override
    public float getMagnitudeAt(int section, int position) {
        return sections.get(section).get(position).getNormalizedValue();
    }

    @Override
    public void registerObserver(@NonNull ChangeObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterObserver(@NonNull ChangeObserver observer) {
        observers.remove(observer);
    }

    public void notifyDataChanged() {
        for (ChangeObserver observer : observers) {
            observer.onGraphAdapterChanged();
        }
    }


    public static class Update {
        final @NonNull List<Section> sections;
        final float peak;
        final float base;

        private Update(@NonNull List<Section> sections, float peak, float base) {
            this.sections = sections;
            this.peak = peak;
            this.base = base;
        }

        public static Update empty() {
            return new Update(Collections.emptyList(), 0f, 0f);
        }

        public static Observable<Update> forHistorySeries(@NonNull List<SensorGraphSample> history,
                                                          @NonNull SensorHistoryPresenter.Mode mode) {
            Observable<Update> operation = Observable.create(s -> {
                if (history.isEmpty()) {
                    s.onNext(Update.empty());
                    s.onCompleted();
                } else {
                    Function<SensorGraphSample, Integer> segmentKeyProducer;
                    if (mode == SensorHistoryPresenter.Mode.WEEK) {
                        segmentKeyProducer = sensorHistory -> sensorHistory.getShiftedTime().getDayOfMonth();
                    } else {
                        segmentKeyProducer = sensorHistory -> {
                            DateTime shiftedTime = sensorHistory.getShiftedTime();
                            return (shiftedTime.getDayOfMonth() * 100) + (shiftedTime.getHourOfDay() / 6);
                        };
                    }
                    List<List<SensorGraphSample>> segments = segment(segmentKeyProducer, history);
                    List<Section> sections = map(segments, Section::new);

                    Comparator<SensorGraphSample> comparator = (l, r) -> Float.compare(l.getNormalizedValue(), r.getNormalizedValue());
                    Extremes<SensorGraphSample> extremes = Extremes.of(history, comparator);
                    float peak = extremes.maxValue.getNormalizedValue();
                    float base = extremes.minValue.getNormalizedValue();

                    s.onNext(new Update(sections, peak, base));
                    s.onCompleted();
                }
            });
            operation.subscribeOn(Schedulers.computation());
            return operation;
        }
    }

    public static class Section {
        private final List<SensorGraphSample> instants;
        private final long average;

        public Section(@NonNull List<SensorGraphSample> instants) {
            this.instants = instants;
            double average = 0f;
            int placeholderCount = 0;
            for (SensorGraphSample instant : instants) {
                if (instant.isValuePlaceholder()) {
                    placeholderCount++;
                }

                average += instant.getNormalizedValue();
            }
            if (placeholderCount == instants.size() / 2) {
                average = ApiService.PLACEHOLDER_VALUE;
            } else {
                average /= instants.size();
            }

            this.average = Math.round(average);
        }

        public SensorGraphSample get(int i) {
            return instants.get(i);
        }

        public int size() {
            return instants.size();
        }

        public long getAverage() {
            return average;
        }

        public SensorGraphSample getRepresentativeValue() {
            return instants.get(0);
        }
    }
}
