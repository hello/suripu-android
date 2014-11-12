package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import is.hello.sense.R;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.util.DateFormatter;

public class SmartAlarmAdapter extends ArrayAdapter<SmartAlarm> implements View.OnClickListener {
    private final LayoutInflater inflater;
    private final OnAlarmEnabledChanged onAlarmEnabledChanged;

    private boolean use24Time = false;

    public SmartAlarmAdapter(@NonNull Context context, @NonNull OnAlarmEnabledChanged onAlarmEnabledChanged) {
        super(context, R.layout.item_smart_alarm);

        this.inflater = LayoutInflater.from(context);
        this.onAlarmEnabledChanged = onAlarmEnabledChanged;
    }

    public void setUse24Time(boolean use24Time) {
        this.use24Time = use24Time;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_smart_alarm, parent, false);
            view.setTag(new ViewHolder(view));
        }

        SmartAlarm alarm = getItem(position);

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.enabled.setTag(position);
        holder.enabled.setChecked(alarm.isEnabled());
        holder.enabled.setOnClickListener(this);
        if (use24Time) {
            holder.timePeriod.setVisibility(View.GONE);
            holder.time.setText(alarm.getTime().toString("H:mm"));
        } else {
            holder.timePeriod.setVisibility(View.VISIBLE);
            holder.time.setText(alarm.getTime().toString("h:mm"));
            holder.timePeriod.setText(alarm.getTime().toString("a"));
        }
        holder.repeat.setText(alarm.getDaysOfWeekSummary(getContext()));

        return view;
    }

    @Override
    public void onClick(View view) {
        int position = (Integer) view.getTag();
        boolean enabled = ((ToggleButton) view).isChecked();
        onAlarmEnabledChanged.onAlarmEnabledChanged(position, enabled);
    }


    private class ViewHolder {
        final ToggleButton enabled;
        final TextView time;
        final TextView timePeriod;
        final TextView repeat;

        ViewHolder(@NonNull View view) {
            this.enabled = (ToggleButton) view.findViewById(R.id.item_smart_alarm_enabled);
            this.time = (TextView) view.findViewById(R.id.item_smart_alarm_time);
            this.timePeriod = (TextView) view.findViewById(R.id.item_smart_alarm_time_period);
            this.repeat = (TextView) view.findViewById(R.id.item_smart_alarm_repeat);
        }
    }

    public interface OnAlarmEnabledChanged {
        void onAlarmEnabledChanged(int position, boolean enabled);
    }
}
