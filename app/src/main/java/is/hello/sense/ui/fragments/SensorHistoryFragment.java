package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.LineGraphView;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.sense.functional.Functions.mapList;
import static is.hello.sense.functional.Functions.segmentList;

public class SensorHistoryFragment extends InjectionFragment implements SelectorLinearLayout.OnSelectionChangedListener {
    @Inject CurrentConditionsPresenter conditionsPresenter;
    @Inject SensorHistoryPresenter sensorHistoryPresenter;

    private TextView readingText;
    private TextView messageTitleText;
    private TextView messageText;
    private LineGraphView graphView;
    private Adapter adapter = new Adapter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorHistoryPresenter.setSensorName(getSensorHistoryActivity().getSensor());
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
            String sensor = getSensorHistoryActivity().getSensor();
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
        sensorHistoryPresenter.setMode(newSelectionIndex);
    }


    public class Adapter implements LineGraphView.Adapter {
        private List<Section> sections = new ArrayList<>();
        private int peakMagnitude = 100;

        public void bindHistory(@NonNull List<SensorHistory> history) {
            sections.clear();
            if (history.isEmpty()) {
                return;
            }

            Observable<List<Section>> generateSeries = Observable.create((Observable.OnSubscribe<List<Section>>) s -> {
                Function<SensorHistory, Integer> segmentKeyProducer = sensorHistory -> sensorHistory.getTime().getDayOfMonth();
                List<List<SensorHistory>> segments = segmentList(segmentKeyProducer, history);
                List<Section> sections = mapList(segments, seg -> new Section(seg));
                s.onNext(sections);
                s.onCompleted();
            }).subscribeOn(Schedulers.computation());

            bindAndSubscribe(generateSeries, sections -> {
                this.sections.addAll(sections);
                graphView.notifyDataChanged();
            }, Functions.LOG_ERROR);
        }

        public void historyUnavailable(Throwable e) {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
            clear();
        }

        public void clear() {
            sections.clear();
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
            return (int) instant.getValue() + " – " + instant.getTime().toString("h:mm a");
        }

        @Override
        public boolean wantsMarkerAt(int section, int position) {
            return false;
        }


        private class Section {
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
}
