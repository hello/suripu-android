package is.hello.sense.ui.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import is.hello.sense.api.model.SmartAlarm;

public class SmartAlarmAdapter extends ArrayAdapter<SmartAlarm> {
    public SmartAlarmAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
    }
}
