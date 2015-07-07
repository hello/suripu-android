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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Observable;

import static is.hello.sense.ui.adapter.SensorHistoryAdapter.Update;

public class RoomConditionsFragment extends UndersideTabFragment implements AdapterView.OnItemClickListener {
    private final UpdateTimer updateTimer = new UpdateTimer(1, TimeUnit.MINUTES);

    @Inject RoomConditionsPresenter presenter;
    @Inject Markdown markdown;

    private Adapter adapter;

    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_CURRENT_CONDITIONS, null);
        }

        addPresenter(presenter);

        updateTimer.setOnUpdate(presenter::update);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_cards_compact, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        // This order applies to:
        // - RoomSensorHistory
        // - RoomConditions
        // - RoomConditionsFragment
        // - UnitSystem
        // - OnboardingRoomCheckFragment
        this.adapter = new Adapter(getActivity(), new SensorEntry[] {
                new SensorEntry(ApiService.SENSOR_NAME_TEMPERATURE),
                new SensorEntry(ApiService.SENSOR_NAME_HUMIDITY),
                new SensorEntry(ApiService.SENSOR_NAME_LIGHT),
                new SensorEntry(ApiService.SENSOR_NAME_SOUND),
        });
        Styles.addCardSpacing(listView, Styles.CARD_SPACING_HEADER_AND_FOOTER | Styles.CARD_SPACING_USE_COMPACT);
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

        updateTimer.schedule();
    }

    @Override
    public void onPause() {
        super.onPause();

        updateTimer.unschedule();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.adapter = null;
    }

    @Override
    public void onSwipeInteractionDidFinish() {
        WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_current_conditions);
    }

    @Override
    public void onUpdate() {
        presenter.update();
    }

    //endregion


    //region Displaying Data

    public void bindConditions(@NonNull RoomConditionsPresenter.Result result) {
        List<ArrayList<SensorGraphSample>> histories = result.roomSensorHistory.toList();
        List<SensorState> sensors = result.conditions.toList();
        List<UnitSystem.Unit> units = result.units.toUnitList();

        adapter.setShowNoSenseMessage(false);
        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            SensorEntry sensorInfo = adapter.getItem(i);

            SensorState sensor = sensors.get(i);
            sensorInfo.formatter = units.get(i).getFormatter();
            sensorInfo.sensorState = sensor;
            sensorInfo.errorMessage = null;

            ArrayList<SensorGraphSample> sensorDataRun = histories.get(i);
            Observable<Update> update = Update.forHistorySeries(sensorDataRun, true);
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

        adapter.setShowNoSenseMessage(ApiException.statusEquals(e, 404));
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
        if (adapter.getShowNoSenseMessage()) {
            return;
        }

        SensorEntry sensorEntry = (SensorEntry) adapterView.getItemAtPosition(position);
        showSensorHistory(sensorEntry.sensorName);
    }


    static class SensorEntry {
        final SensorHistoryAdapter graphAdapter = new SensorHistoryAdapter();
        final @NonNull String sensorName;

        @Nullable UnitSystem.Formatter formatter;
        @Nullable SensorState sensorState;
        @Nullable String errorMessage;

        SensorEntry(@NonNull String sensorName) {
            this.sensorName = sensorName;
        }
    }

    class Adapter extends ArrayAdapter<SensorEntry> {
        private final int VIEW_ID_SENSOR = 0;
        private final int VIEW_ID_NO_SENSE = 1;
        private final int VIEW_ID_COUNT = 2;

        private final Resources resources;
        private final LayoutInflater inflater;
        private final int graphBottomInset;

        private boolean showNoSenseMessage = false;

        Adapter(@NonNull Context context, @NonNull SensorEntry[] conditions) {
            super(context, R.layout.item_room_sensor_condition, conditions);

            this.resources = context.getResources();
            this.inflater = LayoutInflater.from(context);

            Resources resources = context.getResources();
            this.graphBottomInset = resources.getDimensionPixelSize(R.dimen.item_room_sensor_condition_graph_inset);
        }


        void setShowNoSenseMessage(boolean showNoSenseMessage) {
            this.showNoSenseMessage = showNoSenseMessage;
        }

        public boolean getShowNoSenseMessage() {
            return showNoSenseMessage;
        }

        @Override
        public int getCount() {
            if (showNoSenseMessage) {
                return 1;
            } else {
                return super.getCount();
            }
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_ID_COUNT;
        }

        @Override
        public int getItemViewType(int position) {
            if (showNoSenseMessage) {
                return VIEW_ID_NO_SENSE;
            } else {
                return VIEW_ID_SENSOR;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (showNoSenseMessage) {
                if (view == null) {
                    view = inflater.inflate(R.layout.item_message_card, parent, false);

                    TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
                    title.setText(R.string.device_sense);
                    title.setTextAppearance(getContext(), R.style.AppTheme_Text_Body);
                    title.setAllCaps(false);
                    title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.sense_icon, 0, 0, 0);

                    TextView message = (TextView) view.findViewById(R.id.item_message_card_message);
                    message.setText(R.string.error_room_conditions_no_sense);

                    Button action = (Button) view.findViewById(R.id.item_message_card_action);
                    action.setText(R.string.action_pair_new_sense);
                    Views.setSafeOnClickListener(action, ignored -> {
                        DeviceListFragment.startStandaloneFrom(getActivity());
                    });
                }
            } else {
                if (view == null) {
                    view = inflater.inflate(R.layout.item_room_sensor_condition, parent, false);
                    view.setTag(new SensorViewHolder(view));
                }

                SensorEntry sensorEntry = getItem(position);
                SensorViewHolder holder = (SensorViewHolder) view.getTag();
                holder.bind(sensorEntry);
            }

            return view;
        }


        class SensorViewHolder {
            final TextView reading;
            final TextView message;
            final LineGraphDrawable lineGraphDrawable;

            SensorViewHolder(@NonNull View view) {
                this.reading = (TextView) view.findViewById(R.id.item_sensor_condition_reading);
                this.message = (TextView) view.findViewById(R.id.item_sensor_condition_message);

                Resources resources = getResources();
                ColorDrawableCompat fill = Styles.createGraphFillSolidDrawable(resources);
                this.lineGraphDrawable = new LineGraphDrawable(resources, fill);
                lineGraphDrawable.setBottomInset(graphBottomInset);

                View graph = view.findViewById(R.id.fragment_room_sensor_condition_graph);
                graph.setBackground(lineGraphDrawable);
            }

            void bind(@NonNull SensorEntry sensorEntry) {
                if (sensorEntry.sensorState != null) {
                    int sensorColor = resources.getColor(sensorEntry.sensorState.getCondition().colorRes);

                    CharSequence readingText = sensorEntry.sensorState.getFormattedValue(sensorEntry.formatter);
                    if (!TextUtils.isEmpty(readingText)) {
                        reading.setText(readingText);
                        reading.setTextColor(sensorColor);
                    } else {
                        reading.setText(R.string.missing_data_placeholder);
                        reading.setTextColor(resources.getColor(R.color.sensor_unknown));
                    }
                    markdown.renderInto(message, sensorEntry.sensorState.getMessage());

                    lineGraphDrawable.setColorFilter(sensorColor, PorterDuff.Mode.SRC_ATOP);
                    lineGraphDrawable.setAdapter(sensorEntry.graphAdapter);
                } else {
                    reading.setText(R.string.missing_data_placeholder);
                    reading.setTextColor(resources.getColor(R.color.sensor_unknown));
                    if (TextUtils.isEmpty(sensorEntry.errorMessage)) {
                        message.setText(null);
                    } else {
                        message.setText(sensorEntry.errorMessage);
                    }

                    lineGraphDrawable.setColorFilter(null);
                    lineGraphDrawable.setAdapter(null);
                }
            }
        }
    }
}
