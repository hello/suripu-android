package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.primitives.Floats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.LineGraphView;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Logger;

public class SensorHistoryFragment extends InjectionFragment implements SelectorLinearLayout.OnSelectionChangedListener {
    @Inject CurrentConditionsPresenter conditionsPresenter;
    @Inject SensorHistoryPresenter sensorHistoryPresenter;
    @Inject BuildValues buildValues;

    private TextView readingText;
    private TextView messageText;
    private LineGraphView graphView;
    private GraphAdapter adapter = new GraphAdapter();
    private ViewGroup annotationsContainer;

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
        this.messageText = (TextView) view.findViewById(R.id.fragment_sensor_history_message);
        this.graphView = (LineGraphView) view.findViewById(R.id.fragment_sensor_history_graph);
        graphView.setAdapter(adapter);

        SelectorLinearLayout historyMode = (SelectorLinearLayout) view.findViewById(R.id.fragment_sensor_history_mode);
        historyMode.setOnSelectionChangedListener(this);

        this.annotationsContainer = (ViewGroup) view.findViewById(R.id.fragment_sensor_history_graph_annotations);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(conditionsPresenter.currentConditions, this::bindConditions, this::conditionUnavailable);
        bindAndSubscribe(sensorHistoryPresenter.history, adapter::bindData, adapter::bindError);
    }


    public SensorHistoryActivity getSensorHistoryActivity() {
        return (SensorHistoryActivity) getActivity();
    }

    public void bindConditions(@NonNull CurrentConditionsPresenter.Result result) {
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

            messageText.setText(condition.getMessage());
        }
    }

    public void conditionUnavailable(@NonNull Throwable e) {
        if (buildValues.isDebugBuild()) {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        } else {
            Logger.error(SensorHistoryFragment.class.getSimpleName(), "Could not load conditions", e);
            readingText.setText(R.string.missing_data_placeholder);
            messageText.setText(R.string.missing_data_placeholder);
        }
    }


    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        sensorHistoryPresenter.setMode(newSelectionIndex);
    }


    private class GraphAdapter implements LineGraphView.Adapter {
        private final List<SensorHistory> data = new ArrayList<>();
        private int peakY = 100;
        private int sectionCount = 0;
        private int pointCount = 0;


        public void bindData(@NonNull List<SensorHistory> history) {
            this.data.clear();
            this.data.addAll(history);

            if (history.isEmpty()) {
                this.peakY = 100;
                this.sectionCount = 0;
                this.pointCount = 0;
            } else {
                SensorHistory peak = Collections.max(history, (l, r) -> Floats.compare(l.getValue(), r.getValue()));
                this.peakY = Math.max(100, (int) peak.getValue());
                this.sectionCount = 7;
                this.pointCount = data.size() / 8;
            }

            for (int section = 0, count = annotationsContainer.getChildCount(); section < count; section++) {
                TextView annotation = (TextView) annotationsContainer.getChildAt(section);
                if (section < sectionCount) {
                    CharSequence sectionTitle = getSectionTitle(section);
                    CharSequence sectionRepresentativeValue = getFormattedMagnitudeAt(section, getSectionPointCount(section) / 2);
                    annotation.setText(sectionTitle + "\n" + sectionRepresentativeValue);
                } else {
                    annotation.setText(R.string.missing_data_placeholder);
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
            return (section * pointCount) + position;
        }

        @Override
        public int getSectionCount() {
            return sectionCount;
        }

        public @NonNull CharSequence getSectionTitle(int section) {
            SensorHistory representativeSample = data.get(calculateIndex(section, 0));
            if (sensorHistoryPresenter.getMode() == SensorHistoryPresenter.MODE_DAY)
                return representativeSample.getTime().toString("h a");
            else
                return representativeSample.getTime().toString("EE");
        }

        @Override
        public int getPeakMagnitude() {
            return peakY;
        }

        @Override
        public int getSectionPointCount(int section) {
            return pointCount;
        }

        @Override
        public float getMagnitudeAt(int section, int position) {
            return data.get(calculateIndex(section, position)).getValue();
        }

        @NonNull
        @Override
        public CharSequence getFormattedMagnitudeAt(int section, int position) {
            SensorHistory point = data.get(calculateIndex(section, position));
            return Float.toString(point.getValue());
        }

        @Override
        public boolean wantsMarkerAt(int section, int position) {
            return (position == pointCount / 2);
        }
    }
}
