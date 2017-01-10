package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.databinding.ItemSettingsDetailBinding;
import is.hello.sense.databinding.ItemSettingsToggleBinding;
import is.hello.sense.ui.widget.util.Views;

public class SettingsRecyclerAdapter extends ArrayRecyclerAdapter<SettingsRecyclerAdapter.Item,
        SettingsRecyclerAdapter.ViewHolder> {
    private final Resources resources;
    protected final LayoutInflater inflater;
    private boolean wantsDividers = true;

    public SettingsRecyclerAdapter(@NonNull Context context) {
        super(new ArrayList<>());

        this.resources = context.getResources();
        this.inflater = LayoutInflater.from(context);
    }

    public void setWantsDividers(boolean wantsDividers) {
        this.wantsDividers = wantsDividers;
        notifyDataSetChanged();
    }

    //region Binding

    @Override
    public void add(final Item item, final int position){
        super.add(item, position);
        item.bind(this, position);
    }

    @Override
    public boolean add(Item item) {
        if (super.add(item)) {
            item.bind(this, getItemCount() - 1);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getItemViewType(final int position) {
        final Item item = getItem(position);
        return item.getId();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case DetailItem.ID: {
                final View view = inflater.inflate(R.layout.item_settings_detail, parent, false);
                return new DetailViewHolder(view);
            }
            case ToggleItem.ID: {
                final View view = inflater.inflate(R.layout.item_settings_toggle, parent, false);
                return new ToggleViewHolder(view);
            }
            case CheckBoxItem.ID: {
                final View view = inflater.inflate(R.layout.item_settings_checkbox, parent, false);
                return new TextViewHolder(view);
            }
            case TextItem.ID: {
                final View view = inflater.inflate(R.layout.item_settings_text, parent, false);
                return new TextViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException("Unknown view type " + viewType);
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Item item = getItem(position);
        //noinspection unchecked
        holder.bind(item);
    }

    //endregion


    //region View Holders

    abstract class ViewHolder<T extends Item> extends RecyclerView.ViewHolder
        implements View.OnClickListener{
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setBackgroundResource(R.drawable.selectable_dark_bounded);
            Views.setTimeOffsetOnClickListener(itemView, this);
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
                final Item item = getItem(adapterPosition);
                final Runnable onClick = item.onClick;
                if (onClick != null) {
                    onClick.run();
                }
            }
        }
    }

    class TextViewHolder extends ViewHolder<TextItem> {
        final TextView text;

        TextViewHolder(@NonNull final View itemView) {
            super(itemView);

            this.text = (TextView) itemView.findViewById(R.id.item_text);
        }

        @Override
        void bind(@NonNull final TextItem item) {
            text.setText(item.text);
        }
    }

    class DetailViewHolder extends ViewHolder<DetailItem> {

        private final ItemSettingsDetailBinding binding;

        DetailViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.binding = DataBindingUtil.bind(itemView);
        }

        @Override
        void bind(@NonNull DetailItem item) {
            if (item.icon != 0) {
                this.binding.itemSettingsDetailIcon.setImageResource(item.icon);
                this.binding.itemSettingsDetailIcon.setContentDescription(resources.getString(item.iconContentDescription));
                this.binding.itemSettingsDetailIcon.setVisibility(View.VISIBLE);
            } else {
                this.binding.itemSettingsDetailIcon.setImageDrawable(null);
                this.binding.itemSettingsDetailIcon.setVisibility(View.GONE);
            }
            this.binding.itemSettingsDetailTitle.setText(item.text);
            this.binding.itemSettingsDetailDetail.setText(item.value);

            if (!wantsDividers || item.position == getItemCount() - 1) {
                this.binding.itemSettingsDetailDivider.setVisibility(View.GONE);
            } else {
                this.binding.itemSettingsDetailDivider.setVisibility(View.VISIBLE);
            }

            this.binding.executePendingBindings();
        }
    }

    class ToggleViewHolder extends ViewHolder<ToggleItem> {
        private final ItemSettingsToggleBinding binding;

        ToggleViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.binding = DataBindingUtil.bind(itemView);
        }

        @Override
        void bind(@NonNull final ToggleItem item) {
            if(item.icon != 0) {
                this.binding.itemSettingsToggleIcon.setImageResource(item.icon);
            } else {
                this.binding.itemSettingsToggleIcon.setImageDrawable(null);
            }
            this.binding.itemSettingsToggleCheckTitle.setText(item.text);
            this.binding.itemSettingsToggleCheckBox.setChecked(item.value);
        }
    }

    //endregion


    //region Items

    public abstract static class Item<T> {

        T value;
        final
        @Nullable
        Runnable onClick;

        @Nullable
        SettingsRecyclerAdapter adapter;
        int position = RecyclerView.NO_POSITION;

        public Item(@Nullable Runnable onClick) {
            this.onClick = onClick;
        }

        void bind(@NonNull SettingsRecyclerAdapter adapter, int position) {
            this.position = position;
            this.adapter = adapter;
        }

        public void setValue(T value) {
            this.value = value;
            notifyChanged();
        }

        public T getValue() {
            return value;
        }

        protected void notifyChanged(){
            if (position != RecyclerView.NO_POSITION && adapter != null) {
                adapter.notifyItemChanged(position);
            }
        }

        public abstract int getId();
    }

    public static class TextItem<T> extends Item<T>{
        static final int ID = 1;

        String text;

        public TextItem(@NonNull final String text,
                        @Nullable final Runnable onClick) {
            super(onClick);
            this.text = text;
        }

        public void setText(final String text) {
            this.text = text;
            notifyChanged();
        }

        @Override
        public int getId() {
            return ID;
        }
    }

    public static class DetailItem extends TextItem<String> {
        static final int ID = 2;

        @DrawableRes
        int icon;
        @StringRes
        int iconContentDescription;

        public DetailItem(@NonNull final String title,
                          @Nullable final Runnable onClick) {
            super(title, onClick);
        }

        public void setIcon(@DrawableRes final int icon,
                            @StringRes final int iconContentDescription) {
            this.icon = icon;
            this.iconContentDescription = iconContentDescription;
        }

        @Override
        public int getId() {
            return ID;
        }
    }

    public static class ToggleItem extends TextItem<Boolean> {
        static final int ID = 3;

        @DrawableRes
        int icon;

        public ToggleItem(@NonNull final String title,
                          @Nullable final Runnable onClick) {
            super(title, onClick);

            setValue(false);
        }

        @Override
        public int getId() {
            return ID;
        }

        public void setIcon(@DrawableRes final int iconRes) {
            this.icon = iconRes;
        }
    }

    public static class CheckBoxItem<T> extends TextItem<T> {
        static final int ID = 4;

        public CheckBoxItem(@NonNull final String text,
                            @Nullable final Runnable onClick) {
            super(text, onClick);
        }

        @Override
        public int getId() { return ID; }
    }

    //endregion
}
