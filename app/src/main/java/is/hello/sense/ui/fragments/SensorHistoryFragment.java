package is.hello.sense.ui.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.LineGraphView;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class SensorHistoryFragment extends InjectionFragment {
    @Inject CurrentConditionsPresenter conditionsPresenter;
    @Inject UnitFormatter unitsFormatter;
    @Inject SensorHistoryPresenter sensorHistoryPresenter;

    private TextView readingText;
    private TextView messageText;
    private LineGraphView graphView;
    private GraphAdapter adapter = new GraphAdapter();
    private ViewGroup historyModeContainer;

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

        this.historyModeContainer = (ViewGroup) view.findViewById(R.id.fragment_sensor_history_mode_container);
        View.OnClickListener checkedListener = this::onModeToggleChanged;
        for (int i = 0, count = historyModeContainer.getChildCount(); i < count; i++) {
            ToggleButton modeButton = (ToggleButton) historyModeContainer.getChildAt(i);
            boolean selected = (i == sensorHistoryPresenter.getMode());
            modeButton.setTag(i);
            modeButton.setOnClickListener(checkedListener);
            modeButton.setChecked(selected);
            modeButton.setTypeface(modeButton.getTypeface(), selected ? Typeface.BOLD : Typeface.NORMAL);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Pair<RoomConditions, UnitSystem>> currentConditions = Observable.combineLatest(conditionsPresenter.currentConditions, unitsFormatter.unitSystem, Pair::new);
        track(bindFragment(this, currentConditions).subscribe(this::bindConditions, this::presentError));

        Observable<List<SensorHistory>> history = bindFragment(this, sensorHistoryPresenter.history);
        track(history.subscribe(adapter::bindData, adapter::bindError));
    }


    public SensorHistoryActivity getSensorHistoryActivity() {
        return (SensorHistoryActivity) getActivity();
    }


    public void bindConditions(@NonNull Pair<RoomConditions, UnitSystem> pair) {
        RoomConditions conditions = pair.first;
        UnitSystem unitSystem = pair.second;

        SensorState condition = conditions.getSensorStateWithName(getSensorHistoryActivity().getSensor());
        if (condition != null) {
            UnitFormatter.Formatter formatter = SensorHistory.SENSOR_NAME_TEMPERATURE.equals(getSensorHistoryActivity().getSensor()) ? unitSystem::formatTemperature : null;
            String formattedValue = condition.getFormattedValue(formatter);
            if (formattedValue != null)
                readingText.setText(formattedValue);
            else
                readingText.setText(R.string.missing_data_placeholder);

            messageText.setText(condition.getMessage());
        }
    }

    public void presentError(@NonNull Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    public void onModeToggleChanged(@NonNull View button) {
        sensorHistoryPresenter.setMode((Integer) button.getTag());

        for (int i = 0, count = historyModeContainer.getChildCount(); i < count; i++) {
            ToggleButton modeButton = (ToggleButton) historyModeContainer.getChildAt(i);
            boolean selected = (i == sensorHistoryPresenter.getMode());
            modeButton.setChecked(selected);
            modeButton.setTypeface(modeButton.getTypeface(), selected ? Typeface.BOLD : Typeface.NORMAL);
        }
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
                SensorHistory peak = Collections.max(history, (l, r) -> Float.compare(l.getValue(), r.getValue()));
                this.peakY = Math.max(100, (int) peak.getValue());
                this.sectionCount = 7;
                this.pointCount = data.size() / sectionCount;
            }

            graphView.notifyDataChanged();
        }

        @SuppressWarnings("UnusedParameters")
        public void bindError(@NonNull Throwable ignored) {
            this.data.clear();
            graphView.notifyDataChanged();
        }


        @Override
        public int getSectionCount() {
            return sectionCount;
        }

        @Override
        public int getPeakY() {
            return peakY;
        }

        @Override
        public int getPointCount(int section) {
            return pointCount;
        }

        @Override
        public float getPointX(int section, int position) {
            return position;
        }

        @Override
        public float getPointY(int section, int position) {
            int index = (section * pointCount) + position;
            return data.get(index).getValue();
        }

        @Override
        public boolean wantsMarkerAt(int section, int position) {
            return (position == pointCount / 2);
        }
    }
}
