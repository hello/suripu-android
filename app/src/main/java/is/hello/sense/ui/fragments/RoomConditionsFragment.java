package is.hello.sense.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitPrinter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

import static is.hello.sense.ui.adapter.SensorHistoryAdapter.Update;

public class RoomConditionsFragment extends BacksideTabFragment
        implements ArrayRecyclerAdapter.OnItemClickedListener<SensorState> {
    private final UpdateTimer updateTimer = new UpdateTimer(1, TimeUnit.MINUTES);

    @Inject RoomConditionsPresenter presenter;
    @Inject UnitFormatter unitFormatter;

    private Map<String, SensorHistoryAdapter> graphAdapters = new HashMap<>(5);
    @VisibleForTesting
    public Adapter adapter;

    //region Lifecycle

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser){
            Analytics.trackEvent(Analytics.Backside.EVENT_CURRENT_CONDITIONS, null);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPresenter(presenter);
        updateTimer.setOnUpdate(presenter::update);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_room_conditions, container, false);

        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_room_conditions_refresh_container);
        swipeRefreshLayout.setEnabled(false);
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_room_conditions_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new CardItemDecoration(resources));
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));

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
        WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_current_conditions, true);
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

        for (final SensorState sensor : sensors) {
            final String sensorName = sensor.getName();
            final ArrayList<SensorGraphSample> samplesForSensor =
                    roomSensorHistory.getSamplesForSensor(sensorName);
            final SensorHistoryAdapter sensorGraphAdapter = getSensorGraphAdapter(sensorName);
            bindAndSubscribe(Update.forHistorySeries(samplesForSensor, true),
                             sensorGraphAdapter::update,
                             e -> {
                                 Logger.error(getClass().getSimpleName(),
                                              "Could not update graph.", e);
                                 sensorGraphAdapter.clear();
                             });
        }

        adapter.dismissMessage();
        adapter.replaceAll(sensors);
    }

    public void conditionsUnavailable(@NonNull Throwable e) {
        Logger.error(RoomConditionsFragment.class.getSimpleName(), "Could not load conditions", e);

        adapter.clear();
        if (ApiException.isNetworkError(e)) {
            adapter.displayMessage(false, 0, getString(R.string.error_room_conditions_unavailable),
                                   R.string.action_retry,
                                   ignored -> presenter.update());
        } else if (ApiException.statusEquals(e, 404)) {
            adapter.displayMessage(true,
                                   0,
                                   getString(R.string.error_room_conditions_no_sense),
                                   R.string.action_pair_new_sense,
                                   ignored -> {
                                       Intent intent = new Intent(getActivity(), OnboardingActivity.class);
                                       intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
                                       intent.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
                                       startActivity(intent);
                                   });
        } else {
            final StringRef messageRef = Errors.getDisplayMessage(e);
            final String message = messageRef != null
                    ? messageRef.resolve(getActivity())
                    : e.getMessage();
            adapter.displayMessage(false, 0, message,
                                   R.string.action_retry,
                                   ignored -> presenter.update());
        }
        adapter.notifyDataSetChanged();
    }

    //endregion


    @Override
    public void onItemClicked(int position, SensorState sensorState) {
        final Intent intent = new Intent(getActivity(), SensorHistoryActivity.class);
        intent.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensorState.getName());
        startActivity(intent);
    }


    class Adapter extends ArrayRecyclerAdapter<SensorState, ArrayRecyclerAdapter.ViewHolder> {
        private final int VIEW_ID_SENSOR = 0;
        private final int VIEW_ID_MESSAGE = 1;

        private final LayoutInflater inflater;

        private boolean messageWantsSenseIcon;
        private @StringRes int messageTitle;
        private @Nullable CharSequence messageBody;
        private @StringRes int messageActionTitle;
        private @Nullable View.OnClickListener messageActionOnClick;

        Adapter(@NonNull Context context) {
            super(new ArrayList<>(5));

            this.inflater = LayoutInflater.from(context);
        }


        void displayMessage(boolean messageWantsSenseIcon,
                            @StringRes int title,
                            @NonNull CharSequence message,
                            @StringRes int actionTitle,
                            @NonNull View.OnClickListener actionOnClick) {
            this.messageWantsSenseIcon = messageWantsSenseIcon;
            this.messageTitle = title;
            this.messageBody = message;
            this.messageActionTitle = actionTitle;
            this.messageActionOnClick = actionOnClick;

            notifyDataSetChanged();
        }

        void dismissMessage() {
            this.messageTitle = 0;
            this.messageBody = null;
            this.messageActionTitle = 0;
            this.messageActionOnClick = null;
        }

        @Override
        public int getItemCount() {
            if (messageBody != null) {
                return 1;
            } else {
                return super.getItemCount();
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (messageBody != null) {
                return VIEW_ID_MESSAGE;
            } else {
                return VIEW_ID_SENSOR;
            }
        }

        @Override
        public ArrayRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_ID_MESSAGE: {
                    final View view = inflater.inflate(R.layout.item_message_card, parent, false);

                    final TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
                    if (messageTitle != 0) {
                        title.setText(messageTitle);
                        title.setVisibility(View.VISIBLE);
                    }else{
                        title.setVisibility(View.GONE);
                    }

                    final ImageView image = (ImageView) view.findViewById(R.id.item_message_card_image);
                    if (messageWantsSenseIcon) {
                        image.setImageResource(R.drawable.illustration_no_sense);
                        image.setVisibility(View.VISIBLE);
                    }else{
                        image.setVisibility(View.GONE);
                    }

                    final TextView messageText = (TextView) view.findViewById(R.id.item_message_card_message);
                    messageText.setText(messageBody);

                    final Button action = (Button) view.findViewById(R.id.item_message_card_action);
                    action.setText(messageActionTitle);
                    if (messageActionOnClick != null) {
                        Views.setSafeOnClickListener(action, messageActionOnClick);
                    }

                    return new ArrayRecyclerAdapter.ViewHolder(view);
                }
                case VIEW_ID_SENSOR: {
                    final View view = inflater.inflate(R.layout.item_room_sensor_condition, parent, false);
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

                final Resources resources = getResources();
                final ColorDrawableCompat fill = Styles.createGraphFillSolidDrawable(resources);
                this.lineGraphDrawable = new LineGraphDrawable(resources, fill);
                lineGraphDrawable.setBottomInset(0);

                final View graph = view.findViewById(R.id.fragment_room_sensor_condition_graph);
                graph.setBackground(lineGraphDrawable);

                view.setOnClickListener(this);
            }

            @Override
            public void bind(int position) {
                final SensorState sensorState = getItem(position);
                final String sensorName = sensorState.getName();
                final int sensorColor = ContextCompat.getColor(getActivity(), sensorState.getCondition().colorRes);

                final UnitPrinter printer;
                if (ApiService.SENSOR_NAME_PARTICULATES.equals(sensorName)) {
                    printer = UnitPrinter.SIMPLE;
                } else {
                    printer = unitFormatter.getUnitPrinterForSensor(sensorName);
                }
                final CharSequence readingText = sensorState.getFormattedValue(printer);
                if (!TextUtils.isEmpty(readingText)) {
                    reading.setText(readingText);
                    reading.setTextColor(sensorColor);
                } else {
                    reading.setText(R.string.missing_data_placeholder);
                    reading.setTextColor(ContextCompat.getColor(getActivity(), R.color.sensor_unknown));
                }
                message.setText(sensorState.getMessage());

                lineGraphDrawable.setColorFilter(sensorColor, PorterDuff.Mode.SRC_ATOP);
                lineGraphDrawable.setAdapter(getSensorGraphAdapter(sensorName));
            }
        }
    }
}
