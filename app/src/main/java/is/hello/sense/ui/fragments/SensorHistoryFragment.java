package is.hello.sense.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Function;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.LineGraphView;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SuperscriptSpanAdjuster;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.sense.functional.Functions.mapList;
import static is.hello.sense.functional.Functions.segmentList;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class SensorHistoryFragment extends InjectionFragment implements SelectorLinearLayout.OnSelectionChangedListener {
    @Inject CurrentConditionsPresenter conditionsPresenter;
    @Inject SensorHistoryPresenter sensorHistoryPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;

    private TextView readingText;
    private TextView messageText;
    private SelectorLinearLayout historyModeSelector;
    private LineGraphView graphView;
    private ProgressBar loadingIndicator;
    private TextView insightText;
    private Adapter adapter = new Adapter();
    private String sensor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.sensor = getSensorHistoryActivity().getSensor();

        sensorHistoryPresenter.setSensorName(sensor);
        addPresenter(sensorHistoryPresenter);
        addPresenter(conditionsPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_history, container, false);

        this.readingText = (TextView) view.findViewById(R.id.fragment_sensor_history_reading);
        this.messageText = (TextView) view.findViewById(R.id.fragment_sensor_history_message);
        this.graphView = (LineGraphView) view.findViewById(R.id.fragment_sensor_history_graph);
        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_sensor_history_loading);
        this.insightText = (TextView) view.findViewById(R.id.fragment_sensor_history_insight);

        graphView.setAdapter(adapter);

        this.historyModeSelector = (SelectorLinearLayout) view.findViewById(R.id.fragment_sensor_history_mode);
        historyModeSelector.setOnSelectionChangedListener(this);
        historyModeSelector.setButtonTags(SensorHistoryPresenter.MODE_DAY, SensorHistoryPresenter.MODE_WEEK);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(conditionsPresenter.currentConditions, this::bindConditions, this::conditionUnavailable);
        bindAndSubscribe(sensorHistoryPresenter.history, adapter::bindHistory, adapter::historyUnavailable);
        bindAndSubscribe(preferences.observableBoolean(PreferencesPresenter.USE_24_TIME, false), adapter::setUse24Time, Functions.LOG_ERROR);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= Presenter.BASE_TRIM_LEVEL) {
            adapter.clear();
        }
    }


    public SensorHistoryActivity getSensorHistoryActivity() {
        return (SensorHistoryActivity) getActivity();
    }

    public void bindConditions(@Nullable CurrentConditionsPresenter.Result result) {
        if (result == null) {
            readingText.setText(R.string.missing_data_placeholder);
            messageText.setText(R.string.missing_data_placeholder);
            insightText.setText(R.string.missing_data_placeholder);
        } else {
            SensorState condition = result.conditions.getSensorStateWithName(sensor);
            if (condition != null) {
                UnitFormatter.Formatter formatter = null;
                if (SensorHistory.SENSOR_NAME_TEMPERATURE.equals(sensor)) {
                    formatter = result.units::formatTemperature;
                } else if (SensorHistory.SENSOR_NAME_PARTICULATES.equals(sensor)) {
                    formatter = result.units::formatParticulates;
                }

                String formattedValue = condition.getFormattedValue(formatter);
                if (formattedValue != null) {
                    SpannableString formattableString = new SpannableString(formattedValue);
                    if (!SensorHistory.SENSOR_NAME_PARTICULATES.equals(sensor)) {
                        boolean isTemperature = SensorHistory.SENSOR_NAME_TEMPERATURE.equals(sensor);
                        float unitScale = isTemperature ? 0.5f : 0.25f;
                        float adjustment = isTemperature ? 0.75f : 2.2f;
                        formattableString.setSpan(new RelativeSizeSpan(unitScale),
                                                  formattableString.length() - 1,
                                                  formattableString.length(),
                                                  Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        formattableString.setSpan(new SuperscriptSpanAdjuster(adjustment),
                                                  formattableString.length() - 1,
                                                  formattableString.length(),
                                                  Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }

                    readingText.setText(formattableString);
                } else {
                    readingText.setText(R.string.missing_data_placeholder);
                }
                readingText.setTextColor(getResources().getColor(condition.getCondition().colorRes));

                messageText.setText(condition.getMessage());
                insightText.setText("[placeholder] current conditions text");
            } else {
                readingText.setText(R.string.missing_data_placeholder);
                messageText.setText(R.string.missing_data_placeholder);
                insightText.setText(R.string.missing_data_placeholder);
            }
        }
    }

    public void conditionUnavailable(@NonNull Throwable e) {
        Logger.error(SensorHistoryFragment.class.getSimpleName(), "Could not load conditions", e);
        readingText.setText(R.string.missing_data_placeholder);
        messageText.setText(e.getMessage());
        insightText.setText(R.string.missing_data_placeholder);
    }


    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        loadingIndicator.setScaleX(0f);
        loadingIndicator.setScaleY(0f);
        animate(loadingIndicator)
                .fadeIn()
                .scale(1f)
                .start();
        adapter.clear();

        int newMode = (Integer) historyModeSelector.getButtonTag(newSelectionIndex);
        sensorHistoryPresenter.setMode(newMode);
    }


    public class Adapter implements LineGraphView.Adapter {
        private List<Section> sections = new ArrayList<>();
        private float baseMagnitude = 0f;
        private float peakMagnitude = 0f;
        private UnitSystem unitSystem;
        private boolean use24Time = false;

        public void bindHistory(@NonNull Pair<List<SensorHistory>, UnitSystem> historyAndUnits) {
            sections.clear();

            List<SensorHistory> history = historyAndUnits.first;
            if (history.isEmpty()) {
                return;
            }

            Observable<Result> generateSeries = Observable.create((Observable.OnSubscribe<Result>) s -> {
                Function<SensorHistory, Integer> segmentKeyProducer;
                DateTimeZone timeZone = dateFormatter.getTargetTimeZone();
                if (sensorHistoryPresenter.getMode() == SensorHistoryPresenter.MODE_WEEK) {
                    segmentKeyProducer = sensorHistory -> sensorHistory.getTime().withZone(timeZone).getDayOfMonth();
                } else {
                    segmentKeyProducer = sensorHistory -> {
                        DateTime shiftedTime = sensorHistory.getTime().withZone(timeZone);
                        return (shiftedTime.getDayOfMonth() * 100) + (shiftedTime.getHourOfDay() / 6);
                    };
                }
                List<List<SensorHistory>> segments = segmentList(segmentKeyProducer, history);
                List<Section> sections = mapList(segments, Section::new);

                Collections.sort(history, (l, r) -> Float.compare(l.getValue(), r.getValue()));

                float peak = history.get(0).getValue();
                float base = history.get(history.size() - 1).getValue();

                s.onNext(new Result(sections, peak, base));
                s.onCompleted();
            }).subscribeOn(Schedulers.computation());

            bindAndSubscribe(generateSeries, result -> {
                Log.i(SensorHistoryFragment.class.getSimpleName(), "segments delivered");

                this.sections.addAll(result.sections);
                this.peakMagnitude = result.peak;
                this.baseMagnitude = result.base;

                graphView.setNumberOfLines(sections.size());
                graphView.notifyDataChanged();
            }, Functions.LOG_ERROR);

            this.unitSystem = historyAndUnits.second;

            animate(loadingIndicator)
                    .fadeOut(View.GONE)
                    .scale(0f)
                    .start();
        }

        public void historyUnavailable(Throwable e) {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
            clear();

            animate(loadingIndicator)
                    .fadeOut(View.GONE)
                    .scale(0f)
                    .start();
        }

        public void clear() {
            sections.clear();
            graphView.setNumberOfLines(0);
            graphView.notifyDataChanged();
        }

        public void setUse24Time(boolean use24Time) {
            this.use24Time = use24Time;
            graphView.notifyDataChanged();
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
        public int getSectionLineColor(int section) {
            return getResources().getColor(R.color.grey);
        }

        @Override
        public int getSectionTextColor(int section) {
            if (section == sections.size() - 1) {
                return Color.BLACK;
            } else {
                return Color.GRAY;
            }
        }

        @Override
        public float getMagnitudeAt(int section, int position) {
            return sections.get(section).get(position).getValue();
        }

        private String formatSensorValue(float value) {
            switch (sensor) {
                case SensorHistory.SENSOR_NAME_TEMPERATURE:
                    return unitSystem.formatTemperature(value);

                case SensorHistory.SENSOR_NAME_PARTICULATES:
                    return unitSystem.formatParticulates(value);

                case SensorHistory.SENSOR_NAME_HUMIDITY:
                    return Integer.toString((int) value) + "%";

                default:
                    return Integer.toString((int) value);
            }
        }

        @NonNull
        @Override
        public CharSequence getFormattedMagnitudeAt(int section, int position) {
            SensorHistory instant = sections.get(section).get(position);
            String formattedValue = formatSensorValue(instant.getValue());
            return formattedValue + " – " + dateFormatter.formatAsTime(instant.getTime(), use24Time);
        }

        @Override
        public String getSectionHeader(int section) {
            SensorHistory value = sections.get(section).getRepresentativeValue();
            if (sensorHistoryPresenter.getMode() == SensorHistoryPresenter.MODE_WEEK) {
                return dateFormatter.formatDateTime(value.getTime(), "E").substring(0, 1);
            } else {
                if (use24Time)
                    return dateFormatter.formatDateTime(value.getTime(), "H");
                else
                    return dateFormatter.formatDateTime(value.getTime(), "h a");
            }
        }

        @Override
        public String getSectionFooter(int section) {
            float value = sections.get(section).getAverage();
            return formatSensorValue(value);
        }

        private class Result {
            final @NonNull List<Section> sections;
            final float peak;
            final float base;

            Result(@NonNull List<Section> sections, float peak, float base) {
                this.sections = sections;
                this.peak = peak;
                this.base = base;
            }
        }
    }

    private static class Section {
        private final List<SensorHistory> instants;
        private final float average;

        private Section(@NonNull List<SensorHistory> instants) {
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
