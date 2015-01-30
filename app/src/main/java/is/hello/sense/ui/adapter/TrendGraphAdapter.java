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
import is.hello.sense.api.model.GraphType;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.graphing.GraphView;
import is.hello.sense.ui.widget.graphing.adapters.GraphAdapter;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class TrendGraphAdapter implements GraphAdapter, GraphView.HeaderFooterProvider {
    private static final int DAY_CUT_OFF = 9;
    private static final int DAY_STEP = 5;

    private final Resources resources;
    private final List<ChangeObserver> observers = new ArrayList<>();
    private TrendGraph trendGraph;
    private List<TrendGraph.GraphSample> sectionSamples = Collections.emptyList();
    private float baseMagnitude = 0f;
    private float peakMagnitude = 0f;

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

    public void setTrendGraph(@Nullable TrendGraph trendGraph) {
        if (trendGraph == null || Lists.isEmpty(trendGraph.getDataPoints())) {
            this.trendGraph = null;
            this.sectionSamples = Collections.emptyList();
            this.baseMagnitude = 0f;
            this.peakMagnitude = 0f;

            notifyDataChanged();
        } else {
            Observable<Result> calculateMagnitudes = Observable.create(s -> {
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

                List<TrendGraph.GraphSample> sectionSamples;
                if (trendGraph.getDataPoints().size() > DAY_CUT_OFF) {
                    sectionSamples = new ArrayList<>();
                    List<TrendGraph.GraphSample> samples = trendGraph.getDataPoints();
                    for (int i = 0, count = samples.size(); i < count; i++) {
                        if (i % DAY_STEP == 0) {
                            sectionSamples.add(samples.get(i));
                        }
                    }
                } else {
                    sectionSamples = trendGraph.getDataPoints();
                }

                s.onNext(new Result(sectionSamples, base, peak));
                s.onCompleted();
            });

            calculateMagnitudes.subscribeOn(Schedulers.computation())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(result -> {
                                   this.trendGraph = trendGraph;
                                   this.sectionSamples = result.sectionSamples;
                                   this.baseMagnitude = result.baseMagnitude;
                                   this.peakMagnitude = result.peakMagnitude;

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
    public int getSectionHeaderFooterCount() {
        return sectionSamples.size();
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
        if (trendGraph.getDataType() == TrendGraph.DataType.SLEEP_SCORE) {
            if (trendGraph.getGraphType() == GraphType.HISTOGRAM) {
                TrendGraph.GraphSample sample = sectionSamples.get(section);
                return resources.getColor(sample.getDataLabel().colorRes);
            } else {
                return Color.GRAY;
            }
        } else {
            return resources.getColor(R.color.light_accent);
        }
    }

    @NonNull
    @Override
    public String getSectionHeader(int section) {
        TrendGraph.GraphSample sample = sectionSamples.get(section);
        return sample.getXValue();
    }

    @NonNull
    @Override
    public String getSectionFooter(int section) {
        if (TrendGraph.TIME_PERIOD_OVER_TIME_ALL.equals(trendGraph.getTimePeriod())) {
            return "";
        }

        TrendGraph.GraphSample sample = sectionSamples.get(section);
        float value = sample.getYValue();
        switch (trendGraph.getDataType()) {
            default:
            case NONE:
            case SLEEP_SCORE: {
                if (value <= 0f) {
                    return resources.getString(R.string.missing_data_placeholder);
                } else {
                    if (trendGraph.getGraphType() == GraphType.TIME_SERIES_LINE) {
                        return sample.getDateTime().toString("MMM d");
                    } else {
                        return String.format("%.0f", value);
                    }
                }
            }

            case SLEEP_DURATION: {
                if (value <= 0f) {
                    return resources.getString(R.string.missing_data_placeholder);
                }

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
        private final List<TrendGraph.GraphSample> sectionSamples;
        private final float baseMagnitude;
        private final float peakMagnitude;

        private Result(@NonNull List<TrendGraph.GraphSample> sectionSamples,
                       float baseMagnitude,
                       float peakMagnitude) {
            this.sectionSamples = sectionSamples;
            this.baseMagnitude = baseMagnitude;
            this.peakMagnitude = peakMagnitude;
        }
    }
}
