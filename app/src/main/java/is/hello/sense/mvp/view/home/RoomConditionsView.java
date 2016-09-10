package is.hello.sense.mvp.view.home;

import android.app.Activity;
import android.content.Context;
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

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.SensorHistoryAdapter;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitPrinter;

public final class RoomConditionsView extends PresenterView {
    @VisibleForTesting
    Adapter adapter;
    @VisibleForTesting
    UnitFormatter unitFormatter;

    private final Map<String, SensorHistoryAdapter> graphAdapters = new HashMap<>(5);

    public RoomConditionsView(@NonNull final Activity activity,
                              @NonNull final UnitFormatter unitFormatter) {
        super(activity);
        this.unitFormatter = unitFormatter;
    }

    @NonNull
    @Override
    public final View createView(@NonNull final LayoutInflater inflater, @NonNull final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_room_conditions, container, false);

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_room_conditions_refresh_container);
        swipeRefreshLayout.setEnabled(false);
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_room_conditions_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = context.getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new CardItemDecoration(resources));
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
        this.adapter = new Adapter(context);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public final void detach() {
        super.detach();
        adapter.setOnItemClickedListener(null);
        unitFormatter = null;
    }

    public final void setOnAdapterItemClickListener(@NonNull final ArrayRecyclerAdapter.OnItemClickedListener<SensorState> listener) {
        adapter.setOnItemClickedListener(listener);
    }

    public final void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    public final void replaceAllSensors(@NonNull final List<SensorState> sensors) {
        adapter.dismissMessage();
        adapter.replaceAll(sensors);
    }

    public final void displayMessage(final boolean messageWantsSenseIcon,
                                     @StringRes final int title,
                                     @NonNull final CharSequence message,
                                     @StringRes final int actionTitle,
                                     @NonNull final View.OnClickListener actionOnClick) {
        adapter.clear();
        adapter.displayMessage(messageWantsSenseIcon, title, message, actionTitle, actionOnClick);
        adapter.notifyDataSetChanged();

    }


    public final SensorHistoryAdapter getSensorGraphAdapter(@NonNull final String name) {
        SensorHistoryAdapter adapter = graphAdapters.get(name);
        if (adapter == null) {
            adapter = new SensorHistoryAdapter();
            graphAdapters.put(name, adapter);
        }
        return adapter;
    }


    public class Adapter extends ArrayRecyclerAdapter<SensorState, ArrayRecyclerAdapter.ViewHolder> {
        private final int VIEW_ID_SENSOR = 0;
        private final int VIEW_ID_MESSAGE = 1;
        private final LayoutInflater inflater;
        private boolean messageWantsSenseIcon;

        @StringRes
        private int messageTitle;

        @Nullable
        private CharSequence messageBody;

        @StringRes
        private int messageActionTitle;

        @Nullable
        private View.OnClickListener messageActionOnClick;

        Adapter(@NonNull final Context context) {
            super(new ArrayList<>(5));
            this.inflater = LayoutInflater.from(context);
        }

        public void displayMessage(final boolean messageWantsSenseIcon,
                                   @StringRes final int title,
                                   @NonNull final CharSequence message,
                                   @StringRes final int actionTitle,
                                   @NonNull final View.OnClickListener actionOnClick) {
            this.messageWantsSenseIcon = messageWantsSenseIcon;
            this.messageTitle = title;
            this.messageBody = message;
            this.messageActionTitle = actionTitle;
            this.messageActionOnClick = actionOnClick;

            notifyDataSetChanged();
        }

        public void dismissMessage() {
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
        public int getItemViewType(final int position) {
            if (messageBody != null) {
                return VIEW_ID_MESSAGE;
            } else {
                return VIEW_ID_SENSOR;
            }
        }

        @Override
        public ArrayRecyclerAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            switch (viewType) {
                case VIEW_ID_MESSAGE: {
                    final View view = inflater.inflate(R.layout.item_message_card, parent, false);

                    final TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
                    if (messageTitle != 0) {
                        title.setText(messageTitle);
                        title.setVisibility(View.VISIBLE);
                    } else {
                        title.setVisibility(View.GONE);
                    }

                    final ImageView image = (ImageView) view.findViewById(R.id.item_message_card_image);
                    if (messageWantsSenseIcon) {
                        image.setImageResource(R.drawable.illustration_no_sense);
                        image.setVisibility(View.VISIBLE);
                    } else {
                        image.setVisibility(View.GONE);
                    }

                    final TextView messageText = (TextView) view.findViewById(R.id.item_message_card_message);
                    messageText.setText(messageBody);

                    final Button action = (Button) view.findViewById(R.id.item_message_card_action);
                    action.setText(messageActionTitle);
                    if (messageActionOnClick != null) {
                        Views.setSafeOnClickListener(action, messageActionOnClick);
                    }

                    return new SensorViewHolder(view);
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
        public void onBindViewHolder(final ArrayRecyclerAdapter.ViewHolder holder, final int position) {
            holder.bind(position);
        }

        class SensorViewHolder extends ArrayRecyclerAdapter.ViewHolder {
            final TextView reading;
            final TextView message;
            final LineGraphDrawable lineGraphDrawable;

            SensorViewHolder(@NonNull final View view) {
                super(view);

                this.reading = (TextView) view.findViewById(R.id.item_sensor_condition_reading);
                this.message = (TextView) view.findViewById(R.id.item_sensor_condition_message);

                final Resources resources = context.getResources();
                final ColorDrawableCompat fill = Styles.createGraphFillSolidDrawable(resources);
                this.lineGraphDrawable = new LineGraphDrawable(resources, fill);
                lineGraphDrawable.setBottomInset(0);

                final View graph = view.findViewById(R.id.fragment_room_sensor_condition_graph);
                graph.setBackground(lineGraphDrawable);

                view.setOnClickListener(this);
            }

            @Override
            public void bind(final int position) {
                final SensorState sensorState = getItem(position);
                final String sensorName = sensorState.getName();
                final int sensorColor = ContextCompat.getColor(context, sensorState.getCondition().colorRes);

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
                    reading.setTextColor(ContextCompat.getColor(context, R.color.sensor_unknown));
                }
                message.setText(sensorState.getMessage());

                lineGraphDrawable.setColorFilter(sensorColor, PorterDuff.Mode.SRC_ATOP);
                lineGraphDrawable.setAdapter(getSensorGraphAdapter(sensorName));
            }
        }
    }


}
