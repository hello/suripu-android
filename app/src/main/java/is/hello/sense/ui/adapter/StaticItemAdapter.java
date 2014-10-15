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
        super(context, R.layout.item_underside_horizontal);

        this.layoutInflater = LayoutInflater.from(context);
    }

    public Item addItem(@NonNull String title, @Nullable String value, @Nullable Runnable action) {
        Item item = new Item(title, value, action);
        add(item);
        return item;
    }

    public Item addItem(@NonNull String title, @Nullable String value) {
        Item item = new Item(title, value, null);
        add(item);
        return item;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.item_underside_horizontal, parent, false);
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


    public class Item {
        private String title;
        private String value;
        private Runnable action;

        public Item(@NonNull String title, @Nullable String value, @Nullable Runnable action) {
            this.title = title;
            this.value = value;
            this.action = action;
        }

        public Item(@NonNull String title, @Nullable String value) {
            this(title, value, null);
        }


        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
            notifyDataSetChanged();
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
            notifyDataSetChanged();
        }

        public Runnable getAction() {
            return action;
        }

        public void setAction(Runnable action) {
            this.action = action;
        }
    }
}
