package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.DateFormatter;

public class SmartAlarmAdapter extends RecyclerView.Adapter<SmartAlarmAdapter.BaseViewHolder> {
    @VisibleForTesting static final int VIEW_ID_ALARM = 0;
    @VisibleForTesting static final int VIEW_ID_MESSAGE = 1;

    private final LayoutInflater inflater;
    private final InteractionListener interactionListener;
    private final DateFormatter dateFormatter;

    private final List<Alarm> alarms = new ArrayList<>();
    private Message currentMessage;
    private boolean use24Time = false;

    public SmartAlarmAdapter(@NonNull Context context,
                             @NonNull InteractionListener interactionListener,
                             @NonNull DateFormatter dateFormatter) {
        this.dateFormatter = dateFormatter;
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
        final TextView repeat;

        AlarmViewHolder(@NonNull View view) {
            super(view);

            this.enabled = (CompoundButton) view.findViewById(R.id.item_smart_alarm_enabled);
            this.time = (TextView) view.findViewById(R.id.item_smart_alarm_time);
            this.repeat = (TextView) view.findViewById(R.id.item_smart_alarm_repeat);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        void bind(int position) {
            final Alarm alarm = alarms.get(position);
            enabled.setTag(position);
            enabled.setChecked(alarm.isEnabled());
            enabled.setOnClickListener(this);
            time.setText(dateFormatter.formatAsAlarmTime(alarm.getTime(), use24Time));
            repeat.setText(alarm.getDaysOfWeekSummary(repeat.getContext()));
        }

        @Override
        public void onClick(View sender) {
            // View dispatches OnClickListener#onClick(View) calls on
            // the next looper cycle. It's possible for the adapter's
            // containing recycler view to update and invalidate a
            // view holder before the callback fires.
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                if (sender == enabled) {
                    interactionListener.onAlarmEnabledChanged(adapterPosition, enabled.isChecked());
                } else {
                    final Alarm alarm = alarms.get(adapterPosition);
                    interactionListener.onAlarmClicked(adapterPosition, alarm);
                }
            }
        }

        @Override
        public boolean onLongClick(View ignored) {
            // View dispatches OnClickListener#onClick(View) calls on
            // the next looper cycle. It's possible for the adapter's
            // containing recycler view to update and invalidate a
            // view holder before the callback fires.
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                final Alarm alarm = alarms.get(adapterPosition);
                return interactionListener.onAlarmLongClicked(adapterPosition, alarm);
            }

            return false;
        }
    }

    class MessageViewHolder extends BaseViewHolder {
        final TextView titleText;
        final TextView messageText;
        final Button actionButton;
        final ImageView imageView;

        MessageViewHolder(@NonNull View view) {
            super(view);

            this.titleText = (TextView) view.findViewById(R.id.item_message_card_title);
            this.messageText = (TextView) view.findViewById(R.id.item_message_card_message);
            this.actionButton = (Button) view.findViewById(R.id.item_message_card_action);
            this.imageView = (ImageView) view.findViewById(R.id.item_message_card_image);
        }

        @Override
        void bind(int ignored) {
            if (currentMessage.titleRes != 0 ) {
                titleText.setAllCaps(false);
                titleText.setTextAppearance(titleText.getContext(), currentMessage.titleStyleRes);
                titleText.setText(currentMessage.titleRes);
                titleText.setVisibility(View.VISIBLE);
            }else{
                titleText.setVisibility(View.GONE);
            }

            if (currentMessage.titleIconRes != 0) {
                imageView.setImageResource(currentMessage.titleIconRes);
                imageView.setVisibility(View.VISIBLE);
            }else{
                imageView.setVisibility(View.GONE);
            }
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
        public @StringRes final int titleRes;
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
