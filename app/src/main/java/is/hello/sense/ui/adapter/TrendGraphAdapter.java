package is.hello.sense.ui.adapter;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class TrendGraphAdapter implements GraphAdapter, GraphView.HeaderFooterProvider {
    private final List<ChangeObserver> observers = new ArrayList<>();
    private TrendGraph trendGraph;
    private float baseMagnitude = 0f;
    private float peakMagnitude = 0f;


    public void setTrendGraph(@Nullable TrendGraph trendGraph) {
        if (trendGraph == null) {
            this.trendGraph = null;
            this.baseMagnitude = 0f;
            this.peakMagnitude = 0f;

            notifyDataChanged();
        } else {
            Observable<Pair<Float, Float>> calculateMagnitudes = Observable.create(s -> {
                Comparator<TrendGraph.GraphSample> comparator = (l, r) -> Float.compare(r.getYValue(), l.getYValue());
                float peak = Collections.max(trendGraph.getDataPoints(), comparator).getYValue();
                float base = Collections.min(trendGraph.getDataPoints(), comparator).getYValue();

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
    public int getSectionTextColor(int section) {
        return Color.GRAY;
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
        return sample.getDataLabel().toString();
    }
}
