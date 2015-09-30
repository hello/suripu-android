package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;

public class SettingsRecyclerAdapter extends ArrayRecyclerAdapter<SettingsRecyclerAdapter.TextItem,
        SettingsRecyclerAdapter.ViewHolder> {
    private final LayoutInflater inflater;

    public SettingsRecyclerAdapter(@NonNull Context context) {
        super(new ArrayList<>());

        this.inflater = LayoutInflater.from(context);
    }


    //region Binding

    @Override
    public boolean add(TextItem item) {
        if (super.add(item)) {
            item.bind(this, getItemCount() - 1);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getItemViewType(int position) {
        final TextItem item = getItem(position);
        if (item instanceof DetailItem) {
            return DetailItem.ID;
        } else if (item instanceof ToggleItem) {
            return ToggleItem.ID;
        } else {
            return TextItem.ID;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case DetailItem.ID: {
                final View view = inflater.inflate(R.layout.item_static_text, parent, false);
                return new DetailViewHolder(view);
            }
            case ToggleItem.ID: {
                final View view = inflater.inflate(R.layout.item_static_check, parent, false);
                return new ToggleViewHolder(view);
            }
            case TextItem.ID: {
                final View view = inflater.inflate(R.layout.item_section_footer, parent, false);
                return new TextViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException("Unknown view type " + viewType);
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final TextItem item = getItem(position);
        //noinspection unchecked
        holder.bind(item);
    }

    //endregion


    //region View Holders

    abstract class ViewHolder<T extends TextItem> extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        ViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setBackgroundResource(R.drawable.selectable_dark_bounded);
            itemView.setOnClickListener(this);
        }

        abstract void bind(T item);

        @Override
        public void onClick(View ignored) {
            // View dispatches OnClickListener#onClick(View) calls on
            // the next looper cycle. It's possible for the adapter's
            // containing recycler view to update and invalidate a
            // view holder before the callback fires.
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                final TextItem item = getItem(adapterPosition);
                final Runnable onClick = item.onClick;
                if (onClick != null) {
                    onClick.run();
                }
            }
        }
    }

    class TextViewHolder extends ViewHolder<TextItem> {
        final TextView text;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);

            this.text = (TextView) itemView.findViewById(R.id.item_section_footer);
        }

        @Override
        void bind(TextItem item) {
            text.setText(item.text);
        }
    }

    class DetailViewHolder extends ViewHolder<DetailItem> {
        final ImageView icon;
        final TextView title;
        final TextView detail;
        final View divider;

        DetailViewHolder(@NonNull View itemView) {
            super(itemView);

            this.icon = (ImageView) itemView.findViewById(R.id.item_static_text_icon);
            this.title = (TextView) itemView.findViewById(R.id.item_static_text_title);
            this.detail = (TextView) itemView.findViewById(R.id.item_static_text_detail);
            this.divider = itemView.findViewById(R.id.item_static_text_divider);
        }

        @Override
        void bind(DetailItem item) {
            if (item.icon != 0) {
                icon.setImageResource(item.icon);
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setImageDrawable(null);
                icon.setVisibility(View.GONE);
            }
            title.setText(item.text);
            detail.setText(item.detail);

            if (item.position == getItemCount() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
        }
    }

    class ToggleViewHolder extends ViewHolder<ToggleItem> {
        final TextView title;
        final CompoundButton toggle;

        ToggleViewHolder(@NonNull View itemView) {
            super(itemView);

            this.title = (TextView) itemView.findViewById(R.id.item_static_check_title);
            this.toggle = (CompoundButton) itemView.findViewById(R.id.item_static_check_box);
        }

        @Override
        void bind(ToggleItem item) {
            title.setText(item.text);
            toggle.setChecked(item.active);
        }
    }

    //endregion


    //region Items

    public static class TextItem {
        static final int ID = 0;

        public String text;
        final @Nullable Runnable onClick;

        @Nullable SettingsRecyclerAdapter adapter;
        int position = RecyclerView.NO_POSITION;

        public TextItem(@NonNull String text,
                        @Nullable Runnable onClick) {
            this.text = text;
            this.onClick = onClick;
        }

        void bind(@NonNull SettingsRecyclerAdapter adapter, int position) {
            this.position = position;
            this.adapter = adapter;
        }

        public void notifyChanged() {
            if (position != RecyclerView.NO_POSITION && adapter != null) {
                adapter.notifyItemChanged(position);
            }
        }
    }

    public static class DetailItem extends TextItem {
        static final int ID = 1;

        public final @DrawableRes int icon;
        public String detail;

        public DetailItem(@DrawableRes int icon,
                          @NonNull String title,
                          @Nullable String detail,
                          @Nullable Runnable onClick) {
            super(title, onClick);

            this.icon = icon;
            this.detail = detail;
        }
    }

    public static class ToggleItem extends TextItem {
        static final int ID = 2;

        public boolean active;

        public ToggleItem(@NonNull String title,
                          boolean active,
                          @Nullable Runnable onClick) {
            super(title, onClick);
            this.active = active;
        }
    }

    //endregion
}
