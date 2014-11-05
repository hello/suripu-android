package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.LineGraphView;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Logger;

public class SensorHistoryFragment extends InjectionFragment implements SelectorLinearLayout.OnSelectionChangedListener {
    @Inject CurrentConditionsPresenter conditionsPresenter;
    @Inject SensorHistoryPresenter sensorHistoryPresenter;

    private TextView readingText;
    private TextView messageTitleText;
    private TextView messageText;
    private LineGraphView graphView;
    private GraphAdapter adapter = new GraphAdapter();
    private ViewGroup titleAnnotationsContainer;
    private ViewGroup valueAnnotationsContainer;

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

        this.titleAnnotationsContainer = (ViewGroup) view.findViewById(R.id.fragment_sensor_history_graph_title_annotations);
        this.valueAnnotationsContainer = (ViewGroup) view.findViewById(R.id.fragment_sensor_history_graph_value_annotations);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(conditionsPresenter.currentConditions, this::bindConditions, this::conditionUnavailable);
        bindAndSubscribe(sensorHistoryPresenter.history, adapter::bindData, adapter::bindError);
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


    private class GraphAdapter implements LineGraphView.Adapter {
        private final List<SensorHistory> data = new ArrayList<>();
        private int peakY = 100;
        private int sectionCount = 0;
        private int sectionPointCount = 0;


        public void clear() {
            this.data.clear();
            graphView.notifyDataChanged();
        }

        public void bindData(@NonNull List<SensorHistory> history) {
            this.data.clear();
            this.data.addAll(history);

            if (history.isEmpty()) {
                this.peakY = 100;
                this.sectionCount = 0;
                this.sectionPointCount = 0;
            } else {
                SensorHistory peak = Collections.max(history, (l, r) -> Float.compare(l.getValue(), r.getValue()));
                this.peakY = Math.max(100, (int) peak.getValue());
                this.sectionCount = 7;
                this.sectionPointCount = (int) Math.ceil((float) data.size() / (float) sectionCount);
            }

            for (int section = 0, count = titleAnnotationsContainer.getChildCount(); section < count; section++) {
                TextView title = (TextView) titleAnnotationsContainer.getChildAt(section);
                TextView value = (TextView) valueAnnotationsContainer.getChildAt(section);
                if (section < sectionCount) {
                    CharSequence sectionTitle = getSectionTitle(section);
                    title.setText(sectionTitle);

                    CharSequence sectionValue = getFormattedMagnitudeAt(section, getSectionPointCount(section) / 2);
                    value.setText(sectionValue);
                } else {
                    title.setText(R.string.missing_data_placeholder);
                    value.setText(R.string.missing_data_placeholder);
                }
            }

            graphView.notifyDataChanged();
        }

        @SuppressWarnings("UnusedParameters")
        public void bindError(@NonNull Throwable ignored) {
            this.data.clear();
            graphView.notifyDataChanged();
        }


        private int calculateIndex(int section, int position) {
            return Math.min(data.size() - 1, (section * sectionPointCount) + position);
        }

        private SensorHistory getPoint(int section, int position) {
            int index = calculateIndex(section, position);
            return data.get(index);
        }

        @Override
        public int getSectionCount() {
            return sectionCount;
        }

        public @NonNull CharSequence getSectionTitle(int section) {
            SensorHistory representativeSample = data.get(calculateIndex(section, 0));
            if (sensorHistoryPresenter.getMode() == SensorHistoryPresenter.MODE_DAY)
                return representativeSample.getTime().toString("h");
            else
                return representativeSample.getTime().toString("E").substring(0, 1);
        }

        @Override
        public int getPeakMagnitude() {
            return peakY;
        }

        @Override
        public int getSectionPointCount(int section) {
            return sectionPointCount;
        }

        @Override
        public float getMagnitudeAt(int section, int position) {
            return getPoint(section, position).getValue();
        }

        @NonNull
        @Override
        public CharSequence getFormattedMagnitudeAt(int section, int position) {
            SensorHistory point = getPoint(section, position);
            return Integer.toString((int) point.getValue());
        }

        @Override
        public boolean wantsMarkerAt(int section, int position) {
            return false;
        }
    }
}
