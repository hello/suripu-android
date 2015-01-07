package is.hello.sense.ui.adapter;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class TrendGraphAdapter implements GraphAdapter, GraphView.HeaderFooterProvider {
    private final Resources resources;
    private final List<ChangeObserver> observers = new ArrayList<>();
    private TrendGraph trendGraph;
    private float baseMagnitude = 0f;
    private float peakMagnitude = 0f;

    public TrendGraphAdapter(@NonNull Resources resources) {
        this.resources = resources;
    }

    public void setTrendGraph(@Nullable TrendGraph trendGraph) {
        if (trendGraph == null || Lists.isEmpty(trendGraph.getDataPoints())) {
            this.trendGraph = null;
            this.baseMagnitude = 0f;
            this.peakMagnitude = 0f;

            notifyDataChanged();
        } else {
            Observable<Pair<Float, Float>> calculateMagnitudes = Observable.create(s -> {
                List<TrendGraph.GraphSample> dataPoints = trendGraph.getDataPoints();

                float base, peak;
                if (dataPoints.size() == 1) {
                    base = 0f;
                    peak = dataPoints.get(0).getYValue();
                } else {
                    Comparator<TrendGraph.GraphSample> comparator = (l, r) -> Float.compare(r.getYValue(), l.getYValue());
                    peak = Collections.max(dataPoints, comparator).getYValue();
                    base = Collections.min(dataPoints, comparator).getYValue();
                }

                s.onNext(Pair.create(base, peak));
                s.onCompleted();
            });

            calculateMagnitudes.subscribeOn(Schedulers.computation())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(magnitudes -> {
                                   this.trendGraph = trendGraph;
                                   this.baseMagnitude = magnitudes.first;
                                   this.peakMagnitude = magnitudes.second;

                                   notifyDataChanged();
                               }, e -> {
                                   Logger.error(getClass().getSimpleName(), "Could not calculate min-max magnitudes", e);
                               });
        }
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
        if (trendGraph != null) {
            return trendGraph.getDataPoints().size();
        } else {
            return 0;
        }
    }

    @Override
    public int getSectionPointCount(int section) {
        return 1;
    }

    @Override
    public float getMagnitudeAt(int section, int position) {
        TrendGraph.GraphSample sample = trendGraph.getDataPoints().get(section);
        return sample.getYValue();
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


    @Override
    public int getSectionHeaderTextColor(int section) {
        return Color.GRAY;
    }

    @Override
    public int getSectionFooterTextColor(int section) {
        TrendGraph.GraphSample sample = trendGraph.getDataPoints().get(section);
        return resources.getColor(sample.getDataLabel().colorRes);
    }

    @NonNull
    @Override
    public String getSectionHeader(int section) {
        TrendGraph.GraphSample sample = trendGraph.getDataPoints().get(section);
        return sample.getXValue();
    }

    @NonNull
    @Override
    public String getSectionFooter(int section) {
        TrendGraph.GraphSample sample = trendGraph.getDataPoints().get(section);
        switch (trendGraph.getDataType()) {
            default:
            case NONE:
            case SLEEP_SCORE: {
                return String.format("%.0f", sample.getYValue());
            }

            case SLEEP_DURATION: {
                int durationInHours = ((int) sample.getYValue()) / 60;
                int remainingMinutes = ((int) sample.getYValue()) % 60;

                String time = Integer.toString(durationInHours);
                if (remainingMinutes >= 30) {
                    time += ".5";
                }
                time += "h";
                return time;
            }
        }
    }
}
