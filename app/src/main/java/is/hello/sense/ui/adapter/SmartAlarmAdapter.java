package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.ui.widget.util.Views;

public class SmartAlarmAdapter extends RecyclerView.Adapter<SmartAlarmAdapter.BaseViewHolder> {
    private static final int VIEW_ID_ALARM = 0;
    private static final int VIEW_ID_MESSAGE = 1;

    private final LayoutInflater inflater;
    private final InteractionListener interactionListener;

    private final List<Alarm> alarms = new ArrayList<>();
    private Message currentMessage;
    private boolean use24Time = false;

    public SmartAlarmAdapter(@NonNull Context context, @NonNull InteractionListener interactionListener) {
        this.inflater = LayoutInflater.from(context);
        this.interactionListener = interactionListener;
    }

    //region Binding

    public void setUse24Time(boolean use24Time) {
        this.use24Time = use24Time;
        notifyDataSetChanged();
    }

    public void bindAlarms(@NonNull List<Alarm> alarms) {
        this.currentMessage = null;
        this.alarms.clear();
        this.alarms.addAll(alarms);
        notifyDataSetChanged();
    }

    public void bindMessage(@NonNull Message message) {
        this.currentMessage = message;
        this.alarms.clear();
        notifyDataSetChanged();
    }

    public void clearMessage() {
        this.currentMessage = null;
        notifyDataSetChanged();
    }

    //endregion


    //region Data


    @Override
    public int getItemCount() {
        if (currentMessage != null) {
            return 1;
        } else {
            return alarms.size();
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (currentMessage != null) {
            return VIEW_ID_MESSAGE;
        } else {
            return VIEW_ID_ALARM;
        }
    }

    //endregion


    //region Views


    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_ID_MESSAGE: {
                View view = inflater.inflate(R.layout.item_message_card, parent, false);
                return new MessageViewHolder(view);
            }
            case VIEW_ID_ALARM: {
                View view = inflater.inflate(R.layout.item_smart_alarm, parent, false);
                return new AlarmViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(position);
    }


    abstract static class BaseViewHolder extends RecyclerView.ViewHolder {
        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(int position);
    }

    class AlarmViewHolder extends BaseViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        final CompoundButton enabled;
        final TextView time;
        final TextView timePeriod;
        final TextView repeat;

        AlarmViewHolder(@NonNull View view) {
            super(view);

            this.enabled = (CompoundButton) view.findViewById(R.id.item_smart_alarm_enabled);
            this.time = (TextView) view.findViewById(R.id.item_smart_alarm_time);
            this.timePeriod = (TextView) view.findViewById(R.id.item_smart_alarm_time_period);
            this.repeat = (TextView) view.findViewById(R.id.item_smart_alarm_repeat);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        void bind(int position) {
            Alarm alarm = alarms.get(position);

            enabled.setTag(position);
            enabled.setChecked(alarm.isEnabled());
            enabled.setOnClickListener(this);
            if (use24Time) {
                timePeriod.setVisibility(View.GONE);
                time.setText(alarm.getTime().toString("HH:mm"));
            } else {
                timePeriod.setVisibility(View.VISIBLE);
                time.setText(alarm.getTime().toString("h:mm"));
                timePeriod.setText(alarm.getTime().toString("a"));
            }
            repeat.setText(alarm.getDaysOfWeekSummary(repeat.getContext()));
        }

        @Override
        public void onClick(View sender) {
            int position = getAdapterPosition();
            if (sender == enabled) {
                interactionListener.onAlarmEnabledChanged(position, enabled.isChecked());
            } else {
                Alarm alarm = alarms.get(position);
                interactionListener.onAlarmClicked(position, alarm);
            }
        }

        @Override
        public boolean onLongClick(View ignored) {
            int position = getAdapterPosition();
            Alarm alarm = alarms.get(position);
            return interactionListener.onAlarmLongClicked(position, alarm);
        }
    }

    class MessageViewHolder extends BaseViewHolder {
        final TextView titleText;
        final TextView messageText;
        final Button actionButton;

        MessageViewHolder(@NonNull View view) {
            super(view);

            this.titleText = (TextView) view.findViewById(R.id.item_message_card_title);
            this.messageText = (TextView) view.findViewById(R.id.item_message_card_message);
            this.actionButton = (Button) view.findViewById(R.id.item_message_card_action);
        }

        @Override
        void bind(int ignored) {
            titleText.setAllCaps(false);
            titleText.setTextAppearance(titleText.getContext(), currentMessage.titleStyleRes);
            if (currentMessage.titleIconRes != 0) {
                titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(currentMessage.titleIconRes, 0, 0, 0);
            } else {
                titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            }
            titleText.setText(currentMessage.titleRes);
            messageText.setText(currentMessage.message.resolve(messageText.getContext()));

            actionButton.setText(currentMessage.actionRes);
            Views.setSafeOnClickListener(actionButton, currentMessage.onClickListener);
        }
    }

    //endregion


    public interface InteractionListener {
        void onAlarmEnabledChanged(int position, boolean enabled);
        void onAlarmClicked(int position, @NonNull Alarm alarm);
        boolean onAlarmLongClicked(int position, @NonNull Alarm alarm);
    }

    public static class Message {
        public @StringRes
        final int titleRes;
        public final StringRef message;

        public @DrawableRes int titleIconRes = 0;
        public @StyleRes int titleStyleRes = R.style.AppTheme_Text_SectionHeading;

        public @StringRes int actionRes = android.R.string.ok;
        public View.OnClickListener onClickListener;

        public Message(@StringRes int titleRes, @NonNull StringRef message) {
            this.titleRes = titleRes;
            this.message = message;
        }
    }
}
