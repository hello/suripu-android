package is.hello.sense.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.Styles;
import is.hello.sense.ui.widget.graphing.SimpleLineGraphDrawable;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Observable;

public class RoomConditionsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject RoomConditionsPresenter presenter;
    @Inject Markdown markdown;

    @Inject
    ApiService apiService;

    @Inject
    DateFormatter dateFormatter;

    private final RoomSensorInfo temperature = new RoomSensorInfo(SensorHistory.SENSOR_NAME_TEMPERATURE);
    private final RoomSensorInfo humidity = new RoomSensorInfo(SensorHistory.SENSOR_NAME_HUMIDITY);
    private final RoomSensorInfo particulates = new RoomSensorInfo(SensorHistory.SENSOR_NAME_PARTICULATES);
    private Adapter adapter;

    private SensorHistoryAdapter graphAdapter = new SensorHistoryAdapter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(presenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside_list, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        this.adapter = new Adapter(getActivity(), new RoomSensorInfo[] { temperature, humidity, particulates });
        Styles.addCardSpacingHeaderAndFooter(listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.currentConditions, this::bindConditions, this::conditionsUnavailable);
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter.update();

        Observable<ArrayList<SensorHistory>> history = apiService.sensorHistoryForDay(SensorHistory.SENSOR_NAME_TEMPERATURE, SensorHistory.timeForLatest());
        Observable<SensorHistoryAdapter.Update> graphUpdate = history.flatMap(h -> SensorHistory.createAdapterUpdate(h, SensorHistoryPresenter.MODE_DAY, dateFormatter.getTargetTimeZone()));
        bindAndSubscribe(graphUpdate, graphAdapter::update, Functions.LOG_ERROR);
    }


    //region Displaying Data

    public void bindConditions(@Nullable RoomConditionsPresenter.Result result) {
        if (result == null) {
            temperature.formatter = null;
            temperature.sensorState = null;

            humidity.sensorState = null;

            particulates.formatter = null;
            particulates.sensorState = null;
        } else {
            temperature.formatter = result.units::formatTemperature;
            temperature.sensorState = result.conditions.getTemperature();

            humidity.sensorState = result.conditions.getHumidity();

            particulates.formatter = result.units::formatParticulates;
            particulates.sensorState = result.conditions.getParticulates();
        }

        adapter.notifyDataSetChanged();
    }

    public void conditionsUnavailable(@NonNull Throwable e) {
        Logger.error(RoomConditionsFragment.class.getSimpleName(), "Could not load conditions", e);

        temperature.formatter = null;
        temperature.sensorState = null;

        humidity.sensorState = null;

        particulates.formatter = null;
        particulates.sensorState = null;

        adapter.notifyDataSetChanged();
    }

    //endregion


    public void showSensorHistory(@NonNull String sensor) {
        Intent intent = new Intent(getActivity(), SensorHistoryActivity.class);
        intent.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensor);
        startActivity(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        RoomSensorInfo condition = (RoomSensorInfo) adapterView.getItemAtPosition(position);
        showSensorHistory(condition.sensorName);
    }


    static class RoomSensorInfo {
        final @NonNull String sensorName;

        @Nullable UnitFormatter.Formatter formatter;
        @Nullable SensorState sensorState;

        RoomSensorInfo(@NonNull String sensorName) {
            this.sensorName = sensorName;
        }
    }

    class Adapter extends ArrayAdapter<RoomSensorInfo> {
        private final LayoutInflater inflater;

        Adapter(@NonNull Context context, @NonNull RoomSensorInfo[] conditions) {
            super(context, R.layout.item_room_sensor_condition, conditions);

            this.inflater = LayoutInflater.from(context);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_room_sensor_condition, parent, false);
                view.setTag(new ViewHolder(view));
            }

            RoomSensorInfo roomSensorInfo = getItem(position);
            ViewHolder holder = (ViewHolder) view.getTag();
            Resources resources = getContext().getResources();
            if (roomSensorInfo.sensorState != null) {
                int sensorColor = resources.getColor(roomSensorInfo.sensorState.getCondition().colorRes);

                String readingText = roomSensorInfo.sensorState.getFormattedValue(roomSensorInfo.formatter);
                if (!TextUtils.isEmpty(readingText)) {
                    holder.reading.setText(readingText);
                    holder.reading.setTextColor(sensorColor);
                } else {
                    holder.reading.setText(R.string.missing_data_placeholder);
                    holder.reading.setTextColor(resources.getColor(R.color.sensor_unknown));
                }

                String message = roomSensorInfo.sensorState.getMessage();
                holder.message.setText(message);
                markdown.renderWithEmphasisColor(sensorColor, message)
                        .subscribe(holder.message::setText, Functions.LOG_ERROR);
            } else {
                holder.reading.setText(R.string.missing_data_placeholder);
                holder.reading.setTextColor(resources.getColor(R.color.sensor_unknown));
                holder.message.setText(R.string.missing_data_placeholder);
            }
            return view;
        }


        class ViewHolder {
            final TextView reading;
            final TextView message;
            final SimpleLineGraphDrawable lineGraphDrawable;

            ViewHolder(@NonNull View view) {
                this.reading = (TextView) view.findViewById(R.id.item_sensor_condition_reading);
                this.message = (TextView) view.findViewById(R.id.item_sensor_condition_message);

                this.lineGraphDrawable = new SimpleLineGraphDrawable(getResources());
                lineGraphDrawable.setAdapter(graphAdapter);

                View graph = view.findViewById(R.id.fragment_room_sensor_condition_graph);
                graph.setBackground(lineGraphDrawable);
            }
        }
    }
}
