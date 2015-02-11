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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.dialogs.WelcomeDialog;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Observable;

import static is.hello.sense.ui.adapter.SensorHistoryAdapter.Update;

public class RoomConditionsFragment extends UndersideTabFragment implements AdapterView.OnItemClickListener {
    @Inject RoomConditionsPresenter presenter;
    @Inject Markdown markdown;

    private Adapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(presenter);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_CURRENT_CONDITIONS, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside_list, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        // Always change order of RoomSensorHistory and RoomConditions too.
        this.adapter = new Adapter(getActivity(), new SensorEntry[] {
                new SensorEntry(ApiService.SENSOR_NAME_TEMPERATURE),
                new SensorEntry(ApiService.SENSOR_NAME_HUMIDITY),
                new SensorEntry(ApiService.SENSOR_NAME_LIGHT),
                new SensorEntry(ApiService.SENSOR_NAME_SOUND),
        });
        Styles.addCardSpacing(listView, Styles.CARD_SPACING_HEADER_AND_FOOTER);
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
    public void onSwipeInteractionDidFinish() {
        presenter.update();
        WelcomeDialog.showIfNeeded(getActivity(), R.xml.welcome_dialog_current_conditions);
    }


    //region Displaying Data

    public void bindConditions(@NonNull RoomConditionsPresenter.Result result) {
        List<ArrayList<SensorGraphSample>> histories = result.roomSensorHistory.toList();
        List<SensorState> sensors = result.conditions.toList();

        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            SensorEntry sensorInfo = adapter.getItem(i);

            SensorState sensor = sensors.get(i);
            sensorInfo.formatter = result.units.getUnitFormatterForSensor(sensorInfo.sensorName);
            sensorInfo.sensorState = sensor;
            sensorInfo.errorMessage = null;

            ArrayList<SensorGraphSample> sensorDataRun = histories.get(i);
            Observable<Update> update = Update.forHistorySeries(sensorDataRun, SensorHistoryPresenter.Mode.DAY);
            bindAndSubscribe(update,
                             sensorInfo.graphAdapter::update,
                             e -> {
                                 Logger.error(getClass().getSimpleName(), "Could not update graph.", e);
                                 sensorInfo.graphAdapter.clear();
                             });
        }

        adapter.notifyDataSetChanged();
    }

    public void conditionsUnavailable(@NonNull Throwable e) {
        Logger.error(RoomConditionsFragment.class.getSimpleName(), "Could not load conditions", e);

        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            SensorEntry sensorInfo = adapter.getItem(i);
            sensorInfo.formatter = null;
            sensorInfo.sensorState = null;
            sensorInfo.errorMessage = getString(R.string.error_cannot_retrieve_condition, sensorInfo.sensorName);
        }

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
        SensorEntry sensorEntry = (SensorEntry) adapterView.getItemAtPosition(position);
        showSensorHistory(sensorEntry.sensorName);
    }


    static class SensorEntry {
        final SensorHistoryAdapter graphAdapter = new SensorHistoryAdapter();
        final @NonNull String sensorName;

        @Nullable UnitFormatter.Formatter formatter;
        @Nullable SensorState sensorState;
        @Nullable String errorMessage;

        SensorEntry(@NonNull String sensorName) {
            this.sensorName = sensorName;
        }
    }

    class Adapter extends ArrayAdapter<SensorEntry> {
        private final LayoutInflater inflater;
        private final int graphBottomInset;

        Adapter(@NonNull Context context, @NonNull SensorEntry[] conditions) {
            super(context, R.layout.item_room_sensor_condition, conditions);

            this.inflater = LayoutInflater.from(context);

            Resources resources = context.getResources();
            this.graphBottomInset = resources.getDimensionPixelSize(R.dimen.item_room_sensor_condition_graph_inset);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_room_sensor_condition, parent, false);
                view.setTag(new ViewHolder(view));
            }

            SensorEntry sensorEntry = getItem(position);
            ViewHolder holder = (ViewHolder) view.getTag();
            Resources resources = getContext().getResources();
            if (sensorEntry.sensorState != null) {
                int sensorColor = resources.getColor(sensorEntry.sensorState.getCondition().colorRes);

                CharSequence readingText = sensorEntry.sensorState.getFormattedValue(sensorEntry.formatter);
                if (!TextUtils.isEmpty(readingText)) {
                    holder.reading.setText(readingText);
                    holder.reading.setTextColor(sensorColor);
                } else {
                    holder.reading.setText(R.string.missing_data_placeholder);
                    holder.reading.setTextColor(resources.getColor(R.color.sensor_unknown));
                }
                markdown.renderInto(holder.message, sensorEntry.sensorState.getMessage());

                holder.lineGraphDrawable.setColorFilter(sensorColor, PorterDuff.Mode.SRC_ATOP);
                holder.lineGraphDrawable.setAdapter(sensorEntry.graphAdapter);
            } else {
                holder.reading.setText(R.string.missing_data_placeholder);
                holder.reading.setTextColor(resources.getColor(R.color.sensor_unknown));
                if (TextUtils.isEmpty(sensorEntry.errorMessage)) {
                    holder.message.setText(null);
                } else {
                    holder.message.setText(sensorEntry.errorMessage);
                }

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
                lineGraphDrawable.setBottomInset(graphBottomInset);

                View graph = view.findViewById(R.id.fragment_room_sensor_condition_graph);
                graph.setBackground(lineGraphDrawable);
            }
        }
    }
}
