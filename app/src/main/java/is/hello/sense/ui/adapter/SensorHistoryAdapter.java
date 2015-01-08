package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;

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
        return sections.get(section).get(position).getValue();
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

        public Update(@NonNull List<Section> sections, float peak, float base) {
            this.sections = sections;
            this.peak = peak;
            this.base = base;
        }
    }

    public static class Section {
        private final List<SensorHistory> instants;
        private final float average;

        public Section(@NonNull List<SensorHistory> instants) {
            this.instants = instants;
            float average = 0f;
            for (SensorHistory instant : instants) {
                average += instant.getValue();
            }
            average /= instants.size();
            this.average = average;
        }

        public SensorHistory get(int i) {
            return instants.get(i);
        }

        public int size() {
            return instants.size();
        }

        public float getAverage() {
            return average;
        }

        public SensorHistory getRepresentativeValue() {
            return instants.get(0);
        }
    }
}
