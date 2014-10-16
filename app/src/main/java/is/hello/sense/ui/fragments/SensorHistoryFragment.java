package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    @Inject SensorHistoryPresenter historyPresenter;
    @Inject UnitFormatter unitsFormatter;

    private TextView readingText;
    private TextView messageText;
    private LineGraphView graphView;

    private GraphAdapter adapter = new GraphAdapter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        historyPresenter.setSensorName(getSensorHistoryActivity().getSensor());
        addPresenter(conditionsPresenter);
        addPresenter(historyPresenter);

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

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Pair<RoomConditions, UnitSystem>> currentConditions = Observable.combineLatest(conditionsPresenter.currentConditions, unitsFormatter.unitSystem, Pair::new);
        track(bindFragment(this, currentConditions).subscribe(this::bindConditions, this::presentError));

        Observable<List<SensorHistory>> history = bindFragment(this, historyPresenter.history);
        track(history.subscribe(this::bindHistory, this::presentError));
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

    public void bindHistory(@NonNull List<SensorHistory> history) {
        adapter.displayHistory(history);
    }

    public void presentError(@NonNull Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    private class GraphAdapter implements LineGraphView.Adapter {
        private final int DEFAULT_MAX = 100;

        private final ArrayList<SensorHistory> history = new ArrayList<>();

        private int maxY = DEFAULT_MAX;

        public void displayHistory(@NonNull List<SensorHistory> history) {
            this.history.clear();
            this.history.addAll(history);
            if (history.isEmpty()) {
                this.maxY = DEFAULT_MAX;
            } else {
                SensorHistory peakSensor = Collections.max(history, (l, r) -> Float.compare(l.getValue(), r.getValue()));
                this.maxY = Math.max((int) peakSensor.getValue(), DEFAULT_MAX);
            }
            graphView.reloadData();
        }

        @Override
        public int getMaxX() {
            return history.size();
        }

        @Override
        public int getMaxY() {
            return maxY;
        }

        @Override
        public int getPointCount() {
            return history.size();
        }

        @Override
        public float getPointX(int position) {
            return position;
        }

        @Override
        public float getPointY(int position) {
            return history.get(position).getValue();
        }

        @Override
        public boolean wantsMarkerAt(int position) {
            return (position % 15) == 0;
        }
    }
}
