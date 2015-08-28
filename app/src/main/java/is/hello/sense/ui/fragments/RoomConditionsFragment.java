package is.hello.sense.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitPrinter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;

import static is.hello.sense.ui.adapter.SensorHistoryAdapter.Update;

public class RoomConditionsFragment extends UndersideTabFragment
        implements ArrayRecyclerAdapter.OnItemClickedListener<SensorState> {
    private final UpdateTimer updateTimer = new UpdateTimer(1, TimeUnit.MINUTES);

    @Inject RoomConditionsPresenter presenter;
    @Inject Markdown markdown;
    @Inject UnitFormatter unitFormatter;

    private Map<String, SensorHistoryAdapter> graphAdapters = new HashMap<>(5);
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
        View view = inflater.inflate(R.layout.fragment_room_conditions, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_room_conditions_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new CardItemDecoration(getResources(), true));
        recyclerView.setItemAnimator(null);

        this.adapter = new Adapter(getActivity());
        adapter.setOnItemClickedListener(this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(unitFormatter.unitPreferenceChanges(),
                         ignored -> adapter.notifyDataSetChanged(),
                         Functions.LOG_ERROR);
        bindAndSubscribe(presenter.currentConditions,
                         this::bindConditions,
                         this::conditionsUnavailable);
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

    public SensorHistoryAdapter getSensorGraphAdapter(@NonNull String name) {
        SensorHistoryAdapter adapter = graphAdapters.get(name);
        if (adapter == null) {
            adapter = new SensorHistoryAdapter();
            graphAdapters.put(name, adapter);
        }
        return adapter;
    }

    public void bindConditions(@NonNull RoomConditionsPresenter.Result result) {
        final RoomSensorHistory roomSensorHistory = result.roomSensorHistory;
        final List<SensorState> sensors = result.conditions.toList();

        for (SensorState sensor : sensors) {
            final String sensorName = sensor.getName();
            final ArrayList<SensorGraphSample> samplesForSensor = roomSensorHistory.getSamplesForSensor(sensorName);
            final SensorHistoryAdapter sensorGraphAdapter = getSensorGraphAdapter(sensorName);
            bindAndSubscribe(Update.forHistorySeries(samplesForSensor, true),
                             sensorGraphAdapter::update,
                             e -> {
                                 Logger.error(getClass().getSimpleName(), "Could not update graph.", e);
                                 sensorGraphAdapter.clear();
                             });
        }

        adapter.setShowNoSenseMessage(false);
        adapter.replaceAll(sensors);
    }

    public void conditionsUnavailable(@NonNull Throwable e) {
        Logger.error(RoomConditionsFragment.class.getSimpleName(), "Could not load conditions", e);

        adapter.setShowNoSenseMessage(ApiException.statusEquals(e, 404));
        adapter.clear();
    }

    //endregion


    @Override
    public void onItemClicked(int position, SensorState sensorState) {
        Intent intent = new Intent(getActivity(), SensorHistoryActivity.class);
        intent.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensorState.getName());
        startActivity(intent);
    }


    class Adapter extends ArrayRecyclerAdapter<SensorState, ArrayRecyclerAdapter.ViewHolder> {
        private final int VIEW_ID_SENSOR = 0;
        private final int VIEW_ID_NO_SENSE = 1;

        private final Resources resources;
        private final LayoutInflater inflater;
        private final int graphBottomInset;

        private boolean showNoSenseMessage = false;

        Adapter(@NonNull Context context) {
            super(new ArrayList<>(5));

            this.resources = context.getResources();
            this.inflater = LayoutInflater.from(context);

            Resources resources = context.getResources();
            this.graphBottomInset = resources.getDimensionPixelSize(R.dimen.item_room_sensor_condition_graph_inset);
        }


        void setShowNoSenseMessage(boolean showNoSenseMessage) {
            this.showNoSenseMessage = showNoSenseMessage;
        }

        @Override
        public int getItemCount() {
            if (showNoSenseMessage) {
                return 1;
            } else {
                return super.getItemCount();
            }
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
        public ArrayRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_ID_NO_SENSE: {
                    View view = inflater.inflate(R.layout.item_message_card, parent, false);

                    TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
                    title.setText(R.string.device_sense);
                    title.setTextAppearance(title.getContext(), R.style.AppTheme_Text_Body);
                    title.setAllCaps(false);
                    title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.sense_icon, 0, 0, 0);

                    TextView message = (TextView) view.findViewById(R.id.item_message_card_message);
                    message.setText(R.string.error_room_conditions_no_sense);

                    Button action = (Button) view.findViewById(R.id.item_message_card_action);
                    action.setText(R.string.action_pair_new_sense);
                    Views.setSafeOnClickListener(action, ignored -> {
                        DeviceListFragment.startStandaloneFrom(getActivity());
                    });

                    return new ArrayRecyclerAdapter.ViewHolder(view);
                }
                case VIEW_ID_SENSOR: {
                    View view = inflater.inflate(R.layout.item_room_sensor_condition, parent, false);
                    return new SensorViewHolder(view);
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
        }

        @Override
        public void onBindViewHolder(ArrayRecyclerAdapter.ViewHolder holder, int position) {
            holder.bind(position);
        }


        class SensorViewHolder extends ArrayRecyclerAdapter.ViewHolder {
            final TextView reading;
            final TextView message;
            final LineGraphDrawable lineGraphDrawable;

            SensorViewHolder(@NonNull View view) {
                super(view);

                this.reading = (TextView) view.findViewById(R.id.item_sensor_condition_reading);
                this.message = (TextView) view.findViewById(R.id.item_sensor_condition_message);

                Resources resources = getResources();
                ColorDrawableCompat fill = Styles.createGraphFillSolidDrawable(resources);
                this.lineGraphDrawable = new LineGraphDrawable(resources, fill);
                lineGraphDrawable.setBottomInset(graphBottomInset);

                View graph = view.findViewById(R.id.fragment_room_sensor_condition_graph);
                graph.setBackground(lineGraphDrawable);

                view.setOnClickListener(this);
            }

            @Override
            public void bind(int position) {
                final SensorState sensorState = getItem(position);
                final int sensorColor = resources.getColor(sensorState.getCondition().colorRes);

                final UnitPrinter printer = unitFormatter.getUnitPrinterForSensor(sensorState.getName());
                final CharSequence readingText = sensorState.getFormattedValue(printer);
                if (!TextUtils.isEmpty(readingText)) {
                    reading.setText(readingText);
                    reading.setTextColor(sensorColor);
                } else {
                    reading.setText(R.string.missing_data_placeholder);
                    reading.setTextColor(resources.getColor(R.color.sensor_unknown));
                }
                markdown.renderInto(message, sensorState.getMessage());

                lineGraphDrawable.setColorFilter(sensorColor, PorterDuff.Mode.SRC_ATOP);
                lineGraphDrawable.setAdapter(getSensorGraphAdapter(sensorState.getName()));
            }
        }
    }
}
