package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.graphing.Extremes;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import rx.Observable;
import rx.schedulers.Schedulers;

@Deprecated
public class SensorHistoryAdapter implements GraphAdapter {
    private static final int MAX_SECTIONS_COUNT = 7; // like iOS

    private final List<ChangeObserver> observers = new ArrayList<>();

    private List<List<SensorGraphSample>> sections = Collections.emptyList();
    private float baseMagnitude = 0f;
    private float peakMagnitude = 0f;

    public void update(@NonNull Update update) {
        this.baseMagnitude = update.base;
        this.peakMagnitude = update.peak;
        this.sections = update.sections;
        notifyDataChanged();
    }

    public void clear() {
        sections.clear();
        notifyDataChanged();
    }

    public boolean isEmpty() {
        return Lists.isEmpty(sections);
    }

    public @NonNull List<SensorGraphSample> getSection(int position) {
        return sections.get(position);
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
        for (final ChangeObserver observer : observers) {
            observer.onGraphAdapterChanged();
        }
    }


    public static class Update {
        final @NonNull List<List<SensorGraphSample>> sections;
        final float peak;
        final float base;

        private Update(@NonNull List<List<SensorGraphSample>> sections, float peak, float base) {
            this.sections = sections;
            this.peak = peak;
            this.base = base;
        }

        public static Update empty() {
            return new Update(Collections.emptyList(), 0f, 0f);
        }

        /**
         * iOS chomps the end of sensor data series if it ends in
         * a zero / missing value. We do the same for consistency.
         */
        public static List<SensorGraphSample> normalizeDataSeries(@NonNull List<SensorGraphSample> history) {
            if (history.size() > 3) {
                final SensorGraphSample firstSample = history.get(0);
                int start = 0;
                if (firstSample.getValue() == 0f || firstSample.getValue() == ApiService.PLACEHOLDER_VALUE) {
                    start++;
                }

                final SensorGraphSample lastSample = history.get(history.size() - 1);
                int end = history.size();
                if (lastSample.getValue() == 0f || lastSample.getValue() == ApiService.PLACEHOLDER_VALUE) {
                    end--;
                }

                return history.subList(start, end);
            }

            return history;
        }

        public static Observable<Update> forHistorySeries(@Nullable List<SensorGraphSample> history,
                                                          boolean normalize) {
            final Observable<Update> operation = Observable.create(subscriber -> {
                if (history == null || history.isEmpty()) {
                    subscriber.onNext(Update.empty());
                    subscriber.onCompleted();
                } else {
                    final List<SensorGraphSample> normalizedHistory = normalize
                            ? normalizeDataSeries(history)
                            : history;
                    final int numberOfSections = Math.min(MAX_SECTIONS_COUNT, normalizedHistory.size());
                    final int sampleCount = normalizedHistory.size();
                    final int sectionSize = sampleCount / numberOfSections;

                    final List<List<SensorGraphSample>> sections = new ArrayList<>(numberOfSections);
                    for (int i = 0; i < numberOfSections; i++) {
                        final int start = sectionSize * i;
                        final int end;
                        if (i == (numberOfSections - 1)) {
                            end = sampleCount;
                        } else {
                            end = start + sectionSize;
                        }

                        final List<SensorGraphSample> sectionSamples =
                                normalizedHistory.subList(start, end);
                        sections.add(sectionSamples);
                    }

                    final Comparator<SensorGraphSample> comparator =
                            (l, r) -> Float.compare(l.getNormalizedValue(), r.getNormalizedValue());
                    final Extremes<SensorGraphSample> extremes =
                            Extremes.of(normalizedHistory, comparator);
                    final float peak = extremes.maxValue.getNormalizedValue();
                    final float base = extremes.minValue.getNormalizedValue();

                    subscriber.onNext(new Update(sections, peak, base));
                    subscriber.onCompleted();
                }
            });
            return operation.subscribeOn(Schedulers.computation());
        }
    }
}
