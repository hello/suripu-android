package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;

public class TimeZoneAdapter extends ArrayAdapter<String> {
    private final LayoutInflater inflater;
    private final String[] names;

    public TimeZoneAdapter(@NonNull Context context) {
        super(context,
              R.layout.item_static_choice,
              context.getResources().getStringArray(R.array.timezone_ids));

        this.inflater = LayoutInflater.from(context);
        this.names = context.getResources().getStringArray(R.array.timezone_names);
    }


    protected String getDisplayName(int position) {
        return names[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_static_choice, parent, false);

            final ViewHolder viewHolder = new ViewHolder(view);
            viewHolder.setChecked(false);
            view.setTag(viewHolder);
        }

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.title.setText(getDisplayName(position));

        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }


    static class ViewHolder {
        final ImageView checked;
        final TextView title;

        ViewHolder(@NonNull View view) {
            this.checked = (ImageView) view.findViewById(R.id.item_static_choice_checked);
            this.title = (TextView) view.findViewById(R.id.item_static_choice_name);
        }

        void setChecked(boolean checked) {
            if (checked) {
                this.checked.setImageResource(R.drawable.radio_on);
            } else {
                this.checked.setImageResource(R.drawable.radio_off);
            }
        }
    }
}
