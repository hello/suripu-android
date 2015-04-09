package is.hello.sense.ui.adapter;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.GraphType;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.graphing.Extremes;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static is.hello.sense.api.model.TrendGraph.GraphSample;

public class TrendGraphAdapter implements GraphAdapter, GraphView.HeaderFooterProvider {
    private static final int DAY_CUT_OFF = 9;
    private static final int DAY_STEP = 5;

    private final Resources resources;
    private final List<ChangeObserver> observers = new ArrayList<>();
    private TrendGraph trendGraph;
    private List<GraphSample> sectionSamples = Collections.emptyList();
    private @Nullable Extremes<Float> extremes = null;

    public TrendGraphAdapter(@NonNull Resources resources) {
        this.resources = resources;
    }

    public static int getNumberOfLines(@Nullable TrendGraph trendGraph) {
        if (trendGraph != null) {
            int numberOfDataPoints = trendGraph.getDataPoints().size();
            if (numberOfDataPoints > DAY_CUT_OFF) {
                return (numberOfDataPoints / DAY_STEP) + 1;
            } else {
                return numberOfDataPoints;
            }
        } else {
            return 0;
        }
    }

    public void bindTrendGraph(@Nullable TrendGraph trendGraph, @NonNull Runnable onDataChanged) {
        if (trendGraph == null || Lists.isEmpty(trendGraph.getDataPoints())) {
            this.trendGraph = null;
            this.sectionSamples = Collections.emptyList();
            this.extremes = null;

            onDataChanged.run();
            notifyDataChanged();
        } else {
            Observable<Result> calculateMagnitudes = Observable.create(s -> {
                List<GraphSample> dataPoints = trendGraph.getDataPoints();

                Extremes<Float> extremes;
                if (dataPoints.size() == 1) {
                    extremes = new Extremes<>(0f, ApiService.PLACEHOLDER_VALUE,
                                              dataPoints.get(0).getYValue(), ApiService.PLACEHOLDER_VALUE);
                } else {
                    Comparator<GraphSample> comparator = (l, r) -> Float.compare(l.getYValue(), r.getYValue());
                    extremes = Extremes.of(dataPoints, comparator)
                                       .map(GraphSample::getYValue);
                }

                List<GraphSample> sectionSamples;
                if (TrendGraph.TIME_PERIOD_OVER_TIME_ALL.equals(trendGraph.getTimePeriod())) {
                    sectionSamples = Collections.emptyList();
                } else if (trendGraph.getDataPoints().size() > DAY_CUT_OFF) {
                    sectionSamples = Lists.takeEvery(trendGraph.getDataPoints(), DAY_STEP);
                } else {
                    sectionSamples = trendGraph.getDataPoints();
                }

                s.onNext(new Result(sectionSamples, extremes));
                s.onCompleted();
            });

            calculateMagnitudes.subscribeOn(Schedulers.computation())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(result -> {
                                   this.trendGraph = trendGraph;
                                   this.sectionSamples = result.sectionSamples;
                                   this.extremes = result.extremes;

                                   onDataChanged.run();
                                   notifyDataChanged();
                               }, e -> {
                                   Logger.error(getClass().getSimpleName(), "Could not calculate min-max magnitudes", e);
                               });
        }
    }

    public LineGraphDrawable.Marker[] getMarkers() {
        int baseIndex = getBaseIndex();
        int peakIndex = getPeakIndex();
        if (baseIndex == ApiService.PLACEHOLDER_VALUE || peakIndex == ApiService.PLACEHOLDER_VALUE) {
            return null;
        } else {
            int base = (int) getBaseMagnitude();
            int baseColor = resources.getColor(Styles.getSleepScoreColorRes(base));
            String baseString = Integer.toString(base);

            int peak = (int) getPeakMagnitude();
            String peakString = Integer.toString(peak);
            int peakColor = resources.getColor(Styles.getSleepScoreColorRes(peak));

            return new LineGraphDrawable.Marker[] {
                new LineGraphDrawable.Marker(baseIndex, 0, baseColor, baseString),
                new LineGraphDrawable.Marker(peakIndex, 0, peakColor, peakString),
            };
        }
    }

    @Override
    public float getBaseMagnitude() {
        return extremes != null ? extremes.minValue : 0f;
    }

    public int getBaseIndex() {
        return extremes != null ? extremes.minPosition : ApiService.PLACEHOLDER_VALUE;
    }

    @Override
    public float getPeakMagnitude() {
        return extremes != null ? extremes.maxValue : 0f;
    }

    public int getPeakIndex() {
        return extremes != null ? extremes.maxPosition : ApiService.PLACEHOLDER_VALUE;
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
    public int getSectionHeaderFooterCount() {
        return sectionSamples.size();
    }

    @Override
    public int getSectionPointCount(int section) {
        return 1;
    }

    @Override
    public float getMagnitudeAt(int section, int position) {
        GraphSample sample = trendGraph.getDataPoints().get(section);
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
        if (trendGraph.getGraphType() == GraphType.HISTOGRAM &&
                trendGraph.getDataType() == TrendGraph.DataType.SLEEP_DURATION) {
            return resources.getColor(R.color.light_accent);
        } else {
            return resources.getColor(R.color.text_medium);
        }
    }

    @NonNull
    @Override
    public String getSectionHeader(int section) {
        if (trendGraph.getGraphType() == GraphType.HISTOGRAM) {
            GraphSample sample = sectionSamples.get(section);
            return sample.getXValue().substring(0, 1);
        } else {
            return "";
        }
    }

    @NonNull
    @Override
    public String getSectionFooter(int section) {
        if (TrendGraph.TIME_PERIOD_OVER_TIME_ALL.equals(trendGraph.getTimePeriod())) {
            return "";
        }

        GraphSample sample = sectionSamples.get(section);
        float value = sample.getYValue();
        if (value <= 0f) {
            return resources.getString(R.string.missing_data_placeholder);
        }

        switch (trendGraph.getDataType()) {
            default:
            case NONE:
            case SLEEP_SCORE: {
                if (trendGraph.getGraphType() == GraphType.TIME_SERIES_LINE) {
                    return trendGraph.formatSampleDate(sample);
                } else {
                    return String.format("%.0f", value);
                }
            }

            case SLEEP_DURATION: {
                int durationInHours = ((int) value) / 60;
                int remainingMinutes = ((int) value) % 60;

                String time = Integer.toString(durationInHours);
                if (remainingMinutes >= 30) {
                    time += ".5";
                }
                time += "h";
                return time;
            }
        }
    }


    private static class Result {
        private final List<GraphSample> sectionSamples;
        private final Extremes<Float> extremes;

        private Result(@NonNull List<GraphSample> sectionSamples,
                       @NonNull Extremes<Float> extremes) {
            this.sectionSamples = sectionSamples;
            this.extremes = extremes;
        }
    }
}
