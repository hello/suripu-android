package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
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
    private TextView messageTitleText;
    private TextView messageText;
    private LineGraphView graphView;
    private ProgressBar loadingIndicator;
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
        this.messageTitleText = (TextView) view.findViewById(R.id.fragment_sensor_history_message_title);
        this.messageText = (TextView) view.findViewById(R.id.fragment_sensor_history_message);
        this.graphView = (LineGraphView) view.findViewById(R.id.fragment_sensor_history_graph);
        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_sensor_history_loading);
        graphView.setAdapter(adapter);

        SelectorLinearLayout historyMode = (SelectorLinearLayout) view.findViewById(R.id.fragment_sensor_history_mode);
        historyMode.setOnSelectionChangedListener(this);

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
            messageTitleText.setText(R.string.missing_data_placeholder);
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
                if (formattedValue != null)
                    readingText.setText(formattedValue);
                else
                    readingText.setText(R.string.missing_data_placeholder);

                messageTitleText.setText(sensor);
                messageText.setText(condition.getMessage());
            }
        }
    }

    public void conditionUnavailable(@NonNull Throwable e) {
        Logger.error(SensorHistoryFragment.class.getSimpleName(), "Could not load conditions", e);
        readingText.setText(R.string.missing_data_placeholder);
        messageTitleText.setText(R.string.dialog_error_title);
        messageText.setText(e.getMessage());
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
        sensorHistoryPresenter.setMode(newSelectionIndex);
    }


    public class Adapter implements LineGraphView.Adapter {
        private List<Section> sections = new ArrayList<>();
        private int peakMagnitude = 100;
        private UnitSystem unitSystem;
        private boolean use24Time = false;

        public void bindHistory(@NonNull Pair<List<SensorHistory>, UnitSystem> result) {
            sections.clear();

            List<SensorHistory> history = result.first;
            if (history.isEmpty()) {
                return;
            }

            Observable<List<Section>> generateSeries = Observable.create((Observable.OnSubscribe<List<Section>>) s -> {
                Function<SensorHistory, Integer> segmentKeyProducer;
                if (sensorHistoryPresenter.getMode() == SensorHistoryPresenter.MODE_WEEK) {
                    segmentKeyProducer = sensorHistory -> sensorHistory.getTime().getDayOfMonth();
                } else {
                    segmentKeyProducer = sensorHistory -> sensorHistory.getTime().getHourOfDay() / 6;
                }
                List<List<SensorHistory>> segments = segmentList(segmentKeyProducer, history);
                List<Section> sections = mapList(segments, Section::new);
                s.onNext(sections);
                s.onCompleted();
            }).subscribeOn(Schedulers.computation());

            bindAndSubscribe(generateSeries, sections -> {
                this.sections.addAll(sections);
                graphView.setNumberOfLines(sections.size());
                graphView.notifyDataChanged();
            }, Functions.LOG_ERROR);

            this.unitSystem = result.second;

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
        public int getPeakMagnitude() {
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

        @NonNull
        @Override
        public CharSequence getFormattedMagnitudeAt(int section, int position) {
            SensorHistory instant = sections.get(section).get(position);
            float value = instant.getValue();
            String formattedValue;
            switch (sensor) {
                case SensorHistory.SENSOR_NAME_TEMPERATURE:
                    formattedValue = unitSystem.formatTemperature(value);
                    break;

                case SensorHistory.SENSOR_NAME_PARTICULATES:
                    formattedValue = unitSystem.formatParticulates(value);
                    break;

                default:
                    formattedValue = Integer.toString((int) value) + "%";
                    break;
            }

            return formattedValue + " – " + dateFormatter.formatAsTime(instant.getTime(), use24Time);
        }

        @Override
        public String getSectionHeader(int section) {
            SensorHistory value = sections.get(section).getRepresentativeValue();
            if (sensorHistoryPresenter.getMode() == SensorHistoryPresenter.MODE_WEEK) {
                return value.getTime().toString("E").substring(0, 1);
            } else {
                if (use24Time)
                    return value.getTime().toString("H");
                else
                    return value.getTime().toString("h a");
            }
        }

        @Override
        public String getSectionFooter(int section) {
            float value = sections.get(section).getAverage();
            switch (sensor) {
                case SensorHistory.SENSOR_NAME_TEMPERATURE:
                    return unitSystem.formatTemperature(value);
                case SensorHistory.SENSOR_NAME_PARTICULATES:
                    return unitSystem.formatParticulates(value);
                default:
                    return Integer.toString((int) value) + "%";
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
