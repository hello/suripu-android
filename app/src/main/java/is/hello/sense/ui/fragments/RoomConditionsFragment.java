package is.hello.sense.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
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

import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Observable;

public class RoomConditionsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final long THREE_HOURS = (3 * 60 * 60 * 1000);

    @Inject RoomConditionsPresenter presenter;
    @Inject Markdown markdown;

    @Inject ApiService apiService;
    @Inject DateFormatter dateFormatter;

    private final RoomSensorInfo temperature = new RoomSensorInfo(SensorHistory.SENSOR_NAME_TEMPERATURE);
    private final RoomSensorInfo humidity = new RoomSensorInfo(SensorHistory.SENSOR_NAME_HUMIDITY);
    private final RoomSensorInfo particulates = new RoomSensorInfo(SensorHistory.SENSOR_NAME_PARTICULATES);
    private final RoomSensorInfo light = new RoomSensorInfo(SensorHistory.SENSOR_NAME_LIGHT);
    private Adapter adapter;

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

        this.adapter = new Adapter(getActivity(), new RoomSensorInfo[] { temperature, humidity, particulates, light });
        Styles.addCardSpacingHeaderAndFooter(listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadSensorGraphs();
        bindAndSubscribe(presenter.currentConditions, this::bindConditions, this::conditionsUnavailable);
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter.update();
    }


    //region Displaying Data

    public void loadSensorGraphs() {
        long timestamp = SensorHistory.timeForLatest();
        DateTimeZone timeZone = dateFormatter.getTargetTimeZone();
        for (int i = 0, size = adapter.getCount(); i < size; i++) {
            RoomSensorInfo info = adapter.getItem(i);

            Observable<ArrayList<SensorHistory>> history = apiService.sensorHistoryForDay(info.sensorName, timestamp);
            Observable<List<SensorHistory>> threeHours = history.map(h -> Lists.filtered(h, item -> (timestamp - item.getTime().getMillis()) < THREE_HOURS));
            Observable<SensorHistoryAdapter.Update> graphUpdate = threeHours.flatMap(h -> SensorHistory.createAdapterUpdate(h, SensorHistoryPresenter.MODE_DAY, timeZone));
            bindAndSubscribe(graphUpdate,
                             info.graphAdapter::update,
                             e -> {
                                 Logger.error(getClass().getSimpleName(), "Could not load sensor history for " + info.sensorName, e);
                                 info.graphAdapter.clear();
                             });
        }
    }

    public void bindConditions(@Nullable RoomConditionsPresenter.Result result) {
        if (result == null) {
            temperature.formatter = null;
            temperature.sensorState = null;

            humidity.formatter = null;
            humidity.sensorState = null;

            particulates.formatter = null;
            particulates.sensorState = null;

            light.formatter = null;
            light.sensorState = null;
        } else {
            temperature.formatter = result.units.getUnitFormatterForSensor(SensorHistory.SENSOR_NAME_TEMPERATURE);
            temperature.sensorState = result.conditions.getTemperature();

            humidity.formatter = result.units.getUnitFormatterForSensor(SensorHistory.SENSOR_NAME_HUMIDITY);
            humidity.sensorState = result.conditions.getHumidity();

            particulates.formatter = result.units.getUnitFormatterForSensor(SensorHistory.SENSOR_NAME_PARTICULATES);
            particulates.sensorState = result.conditions.getParticulates();

            light.formatter = result.units.getUnitFormatterForSensor(SensorHistory.SENSOR_NAME_LIGHT);
            light.sensorState = result.conditions.getLight();
        }

        adapter.notifyDataSetChanged();
    }

    public void conditionsUnavailable(@NonNull Throwable e) {
        Logger.error(RoomConditionsFragment.class.getSimpleName(), "Could not load conditions", e);

        temperature.formatter = null;
        temperature.sensorState = null;

        humidity.formatter = null;
        humidity.sensorState = null;

        particulates.formatter = null;
        particulates.sensorState = null;

        light.formatter = null;
        light.sensorState = null;

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
        final SensorHistoryAdapter graphAdapter = new SensorHistoryAdapter();
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
                markdown.render(message)
                        .subscribe(holder.message::setText, Functions.LOG_ERROR);

                holder.lineGraphDrawable.setColorFilter(sensorColor, PorterDuff.Mode.SRC_ATOP);
                holder.lineGraphDrawable.setAdapter(roomSensorInfo.graphAdapter);
            } else {
                holder.reading.setText(R.string.missing_data_placeholder);
                holder.reading.setTextColor(resources.getColor(R.color.sensor_unknown));
                holder.message.setText(R.string.missing_data_placeholder);

                holder.lineGraphDrawable.setColorFilter(null);
                holder.lineGraphDrawable.setAdapter(null);
            }
            return view;
        }


        class ViewHolder {
            final TextView reading;
            final TextView message;
            final LineGraphDrawable lineGraphDrawable;

            ViewHolder(@NonNull View view) {
                this.reading = (TextView) view.findViewById(R.id.item_sensor_condition_reading);
                this.message = (TextView) view.findViewById(R.id.item_sensor_condition_message);

                this.lineGraphDrawable = new LineGraphDrawable(getResources());

                View graph = view.findViewById(R.id.fragment_room_sensor_condition_graph);
                graph.setBackground(lineGraphDrawable);
            }
        }
    }
}
