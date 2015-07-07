package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.SafeOnClickListener;

public class SmartAlarmAdapter extends BaseAdapter implements View.OnClickListener {
    private static final int VIEW_ID_ALARM = 0;
    private static final int VIEW_ID_MESSAGE = 1;
    private static final int VIEW_ID_COUNT = 2;

    private final Context context;
    private final LayoutInflater inflater;
    private final OnAlarmEnabledChanged onAlarmEnabledChanged;
    private final SafeOnClickListener onClickListener = new SafeOnClickListener(this);

    private final List<Alarm> alarms = new ArrayList<>();
    private @Nullable Message currentMessage;
    private boolean use24Time = false;

    public SmartAlarmAdapter(@NonNull Context context, @NonNull OnAlarmEnabledChanged onAlarmEnabledChanged) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.onAlarmEnabledChanged = onAlarmEnabledChanged;
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

    public boolean isShowingMessage() {
        return (currentMessage != null);
    }

    public void clearMessage() {
        this.currentMessage = null;
        notifyDataSetChanged();
    }

    public void clear() {
        this.alarms.clear();
        this.currentMessage = null;
        notifyDataSetChanged();
    }

    //endregion


    //region Data

    @Override
    public int getCount() {
        if (currentMessage != null) {
            return 1;
        } else {
            return alarms.size();
        }
    }

    @Override
    public Object getItem(int position) {
        if (currentMessage != null) {
            if (position > 0) {
                throw new IndexOutOfBoundsException();
            }

            return currentMessage;
        } else {
            return alarms.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_ID_COUNT;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (currentMessage != null) {
            if (view == null) {
                view = inflater.inflate(R.layout.item_message_card, parent, false);
                view.setTag(new MessageViewHolder(view));
            }

            MessageViewHolder holder = (MessageViewHolder) view.getTag();
            holder.bind(currentMessage);

        } else {
            if (view == null) {
                view = inflater.inflate(R.layout.item_smart_alarm, parent, false);
                view.setTag(new AlarmViewHolder(view));
            }

            Alarm alarm = alarms.get(position);
            AlarmViewHolder holder = (AlarmViewHolder) view.getTag();
            holder.bind(position, alarm);
        }

        return view;
    }

    @Override
    public void onClick(View view) {
        int position = (Integer) view.getTag();
        boolean enabled = ((CompoundButton) view).isChecked();
        onAlarmEnabledChanged.onAlarmEnabledChanged(position, enabled);
    }


    private class AlarmViewHolder {
        final CompoundButton enabled;
        final TextView time;
        final TextView timePeriod;
        final TextView repeat;

        AlarmViewHolder(@NonNull View view) {
            this.enabled = (CompoundButton) view.findViewById(R.id.item_smart_alarm_enabled);
            this.time = (TextView) view.findViewById(R.id.item_smart_alarm_time);
            this.timePeriod = (TextView) view.findViewById(R.id.item_smart_alarm_time_period);
            this.repeat = (TextView) view.findViewById(R.id.item_smart_alarm_repeat);
        }

        void bind(int position, @NonNull Alarm alarm) {
            enabled.setTag(position);
            enabled.setChecked(alarm.isEnabled());
            enabled.setOnClickListener(onClickListener);
            if (use24Time) {
                timePeriod.setVisibility(View.GONE);
                time.setText(alarm.getTime().toString("H:mm"));
            } else {
                timePeriod.setVisibility(View.VISIBLE);
                time.setText(alarm.getTime().toString("h:mm"));
                timePeriod.setText(alarm.getTime().toString("a"));
            }
            repeat.setText(alarm.getDaysOfWeekSummary(context));
        }
    }

    private class MessageViewHolder {
        final TextView titleText;
        final TextView messageText;
        final Button actionButton;

        MessageViewHolder(@NonNull View view) {
            this.titleText = (TextView) view.findViewById(R.id.item_message_card_title);
            this.messageText = (TextView) view.findViewById(R.id.item_message_card_message);
            this.actionButton = (Button) view.findViewById(R.id.item_message_card_action);
        }

        void bind(@NonNull Message message) {
            titleText.setAllCaps(false);
            titleText.setTextAppearance(context, message.titleStyleRes);
            if (message.titleIconRes != 0) {
                titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(message.titleIconRes, 0, 0, 0);
            } else {
                titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            }
            titleText.setText(message.titleRes);
            messageText.setText(message.message.resolve(context));

            actionButton.setText(message.actionRes);
            Views.setSafeOnClickListener(actionButton, message.onClickListener);
        }
    }

    //endregion


    public interface OnAlarmEnabledChanged {
        void onAlarmEnabledChanged(int position, boolean enabled);
    }

    public static class Message {
        public @StringRes int titleRes;
        public StringRef message;

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
