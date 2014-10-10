package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import is.hello.sense.R;

public class StaticItemAdapter extends ArrayAdapter<StaticItemAdapter.Item> {
    private final LayoutInflater layoutInflater;

    public StaticItemAdapter(Context context) {
        super(context, R.layout.list_horizontal_item);

        this.layoutInflater = LayoutInflater.from(context);
    }

    public void addItem(@NonNull String title, @Nullable String value, @Nullable Runnable action) {
        add(new Item(title, value, action));
    }

    public void addItem(@NonNull String title, @Nullable String value) {
        add(new Item(title, value, null));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.list_horizontal_item, parent, false);
            view.setTag(new ViewHolder(view));
        }

        Item item = getItem(position);
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.title.setText(item.title);
        holder.detail.setText(item.value);

        return view;
    }

    private class ViewHolder {
        private final TextView title;
        private final TextView detail;

        private ViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.list_horizontal_item_title);
            this.detail = (TextView) view.findViewById(R.id.list_horizontal_item_detail);
        }
    }


    public static class Item {
        public final String title;
        public final String value;
        public final Runnable action;

        public Item(@NonNull String title, @Nullable String value, @Nullable Runnable action) {
            this.title = title;
            this.value = value;
            this.action = action;
        }

        public Item(@NonNull String title, @Nullable String value) {
            this(title, value, null);
        }
    }
}
