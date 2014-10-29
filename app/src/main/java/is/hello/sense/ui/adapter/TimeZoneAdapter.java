package is.hello.sense.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import is.hello.sense.R;

public class TimeZoneAdapter extends ArrayAdapter<String> {
    private final String[] names;

    public TimeZoneAdapter(Context context) {
        super(context, R.layout.item_simple_text, context.getResources().getStringArray(R.array.timezone_ids));
        this.names = context.getResources().getStringArray(R.array.timezone_names);
    }


    protected String getDisplayName(int position) {
        return names[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) super.getView(position, convertView, parent);
        textView.setText(getDisplayName(position));
        return textView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
        textView.setText(getDisplayName(position));
        return textView;
    }
}
