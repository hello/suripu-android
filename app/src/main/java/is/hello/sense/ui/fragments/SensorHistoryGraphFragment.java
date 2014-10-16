package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.LineGraphView;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class SensorHistoryGraphFragment extends InjectionFragment {
    public static final String ARG_SENSOR = SensorHistoryGraphFragment.class.getName() + ".ARG_SENSOR";
    public static final String ARG_MODE = SensorHistoryGraphFragment.class.getName() + ".ARG_MODE";

    @Inject SensorHistoryPresenter historyPresenter;

    private LineGraphView graphView;
    private GraphAdapter adapter = new GraphAdapter();

    public static SensorHistoryGraphFragment newInstance(@NonNull String sensorName, int mode) {
        SensorHistoryGraphFragment fragment = new SensorHistoryGraphFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_SENSOR, sensorName);
        arguments.putInt(ARG_MODE, mode);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String sensor = getArguments().getString(ARG_SENSOR);
        historyPresenter.setSensorName(sensor);

        int mode = getArguments().getInt(ARG_MODE);
        historyPresenter.setMode(mode);

        addPresenter(historyPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_history_graph, container, false);

        this.graphView = (LineGraphView) view.findViewById(R.id.fragment_sensor_history_graph);
        graphView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<List<SensorHistory>> history = bindFragment(this, historyPresenter.history);
        track(history.subscribe(this::bindHistory, this::presentError));
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
