package is.hello.sense.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.Styles;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;

public class CurrentConditionsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject CurrentConditionsPresenter presenter;
    @Inject Markdown markdown;

    private final ConditionData temperature = new ConditionData(SensorHistory.SENSOR_NAME_TEMPERATURE);
    private final ConditionData humidity = new ConditionData(SensorHistory.SENSOR_NAME_HUMIDITY);
    private final ConditionData particulates = new ConditionData(SensorHistory.SENSOR_NAME_PARTICULATES);
    private ConditionsAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside_list, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        this.adapter = new ConditionsAdapter(getActivity(), new ConditionData[] { temperature, humidity, particulates });
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
    }


    //region Displaying Data

    public void bindConditions(@Nullable CurrentConditionsPresenter.Result result) {
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
        Logger.error(CurrentConditionsFragment.class.getSimpleName(), "Could not load conditions", e);

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
        ConditionData condition = adapter.getItem(position);
        showSensorHistory(condition.sensorName);
    }


    static class ConditionData {
        final @NonNull String sensorName;

        @Nullable UnitFormatter.Formatter formatter;
        @Nullable SensorState sensorState;

        ConditionData(@NonNull String sensorName) {
            this.sensorName = sensorName;
        }
    }

    class ConditionsAdapter extends ArrayAdapter<ConditionData> {
        private final LayoutInflater inflater;

        ConditionsAdapter(Context context, ConditionData[] conditions) {
            super(context, R.layout.item_sensor_condition, conditions);

            this.inflater = LayoutInflater.from(context);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_sensor_condition, parent, false);
                view.setTag(new ViewHolder(view));
            }

            ConditionData conditionData = getItem(position);
            ViewHolder holder = (ViewHolder) view.getTag();
            Resources resources = getContext().getResources();
            if (conditionData.sensorState != null) {
                int sensorColor = resources.getColor(conditionData.sensorState.getCondition().colorRes);

                holder.reading.setText(conditionData.sensorState.getFormattedValue(conditionData.formatter));
                holder.reading.setTextColor(sensorColor);

                String message = conditionData.sensorState.getMessage();
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

            ViewHolder(@NonNull View view) {
                this.reading = (TextView) view.findViewById(R.id.item_sensor_condition_reading);
                this.message = (TextView) view.findViewById(R.id.item_sensor_condition_message);
            }
        }
    }
}
