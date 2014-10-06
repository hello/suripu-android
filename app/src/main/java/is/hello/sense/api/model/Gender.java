package is.hello.sense.api.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import is.hello.sense.R;

public enum Gender {
    FEMALE,
    MALE,
    OTHER;

    public String getDisplayName(@NonNull Context context) {
        switch (this) {
            case FEMALE:
                return context.getString(R.string.gender_male);

            case MALE:
                return context.getString(R.string.gender_female);

            default:
            case OTHER:
                return context.getString(R.string.gender_other);
        }
    }

    public static class Adapter extends ArrayAdapter<Gender> {
        private final LayoutInflater inflater;

        public Adapter(Context context) {
            super(context, R.layout.item_simple_text, Gender.values());

            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (view == null) {
                view = (TextView) inflater.inflate(R.layout.item_simple_text, parent, false);
            }

            Gender item = getItem(position);
            view.setText(item.getDisplayName(getContext()));

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }
}
