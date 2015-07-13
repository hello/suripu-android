package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.ui.widget.graphing.Extremes;
import rx.Observable;

public class TrendsPresenter extends ScopedValuePresenter<ArrayList<TrendsPresenter.Rendered>> {
    public static final int DAY_CUT_OFF = 9;
    public static final int DAY_STEP = 5;

    public static final int MONTH_CUT_OFF = 31;
    public static final int MONTH_STEP = 28;

    @Inject ApiService apiService;

    public final PresenterSubject<ArrayList<Rendered>> trends = subject;


    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<Rendered>> provideUpdateObservable() {
        return apiService.allTrends().map(trendGraphs -> {
            ArrayList<Rendered> renderedGraphs = new ArrayList<>();
            for (TrendGraph trendGraph : trendGraphs) {
                renderedGraphs.add(renderTrendGraph(trendGraph));
            }
            return renderedGraphs;
        });
    }


    public Observable<Void> updateTrend(int position, @NonNull String newTimePeriod) {
        logEvent("updateTrend(" + position + ", " + newTimePeriod + ")");

        return latest().flatMap(trends -> {
            Rendered rendered = trends.get(position);
            return apiService.trendGraph(rendered.graph.getDataType().toQueryString(), newTimePeriod)
                    .doOnNext(newTrends -> {
                        ArrayList<Rendered> updatedAllTrends = new ArrayList<>(trends);
                        updatedAllTrends.set(position, renderTrendGraph(newTrends.get(0)));
                        this.trends.onNext(updatedAllTrends);
                    })
                    .map(Functions.TO_VOID);
        });
    }


    public static Rendered renderTrendGraph(@NonNull TrendGraph trendGraph) {
        List<TrendGraph.GraphSample> sectionSamples;
        Extremes<Float> extremes;

        if (Lists.isEmpty(trendGraph.getDataPoints())) {
            sectionSamples = Collections.emptyList();
            extremes = null;
        } else {
            List<TrendGraph.GraphSample> dataPoints = trendGraph.getDataPoints();

            if (dataPoints.size() == 1) {
                extremes = new Extremes<>(0f, ApiService.PLACEHOLDER_VALUE,
                        dataPoints.get(0).getYValue(), ApiService.PLACEHOLDER_VALUE);
            } else {
                Comparator<TrendGraph.GraphSample> comparator = (l, r) -> Float.compare(l.getYValue(), r.getYValue());
                extremes = Extremes.of(dataPoints, comparator)
                                   .map(TrendGraph.GraphSample::getYValue);
            }

            if (TrendGraph.TIME_PERIOD_OVER_TIME_ALL.equals(trendGraph.getTimePeriod())) {
                sectionSamples = Collections.emptyList();
            } else if (trendGraph.getDataPoints().size() > MONTH_CUT_OFF) {
                sectionSamples = Lists.takeEvery(trendGraph.getDataPoints(), MONTH_STEP);
            } else if (trendGraph.getDataPoints().size() > DAY_CUT_OFF) {
                sectionSamples = Lists.takeEvery(trendGraph.getDataPoints(), DAY_STEP);
            } else {
                sectionSamples = trendGraph.getDataPoints();
            }
        }

        return new Rendered(trendGraph, sectionSamples, extremes);
    }

    public static class Rendered implements Serializable {
        public final @NonNull TrendGraph graph;
        public final @NonNull List<TrendGraph.GraphSample> sectionSamples;
        public final @Nullable Extremes<Float> extremes;

        public Rendered(@NonNull TrendGraph graph,
                        @NonNull List<TrendGraph.GraphSample> sectionSamples,
                        @Nullable Extremes<Float> extremes) {
            this.graph = graph;
            this.sectionSamples = sectionSamples;
            this.extremes = extremes;
        }


        @Override
        public String toString() {
            return "Rendered{" +
                    "graph=" + graph +
                    ", sectionSamples=" + sectionSamples +
                    ", extremes=" + extremes +
                    '}';
        }
    }
}
