package is.hello.sense.ui.adapter;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.databinding.ItemMessageCardBinding;
import is.hello.sense.flows.notification.ui.adapters.NotificationSettingsAdapter;

public abstract class ArrayRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private static final String KEY_ITEMS = NotificationSettingsAdapter.class.getSimpleName() + ".KEY_ITEMS";

    private final List<T> storage;
    private
    @Nullable
    OnItemClickedListener<T> onItemClickedListener;
    private ErrorHandler errorHandler = null;

    protected ArrayRecyclerAdapter(@NonNull final List<T> storage) {
        this.storage = storage;
    }

    //region Operations

    @Override
    public int getItemCount() {
        return storage.size();
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public T getItem(final int position) {
        return storage.get(position);
    }

    public List<T> getItems() {
        return this.storage;
    }

    public boolean replaceAll(@NonNull final Collection<? extends T> collection) {
        final int oldSize = getItemCount();
        final int newSize = collection.size();

        storage.clear();
        if (!storage.addAll(collection)) {
            return false;
        }

        if (oldSize > newSize) {
            notifyItemRangeRemoved(newSize, oldSize - newSize);
            notifyItemRangeChanged(0, newSize);
        } else if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize);
            notifyItemRangeChanged(0, oldSize);
        } else {
            notifyItemRangeChanged(0, newSize);
        }

        return true;
    }

    public boolean addAll(@NonNull final Collection<? extends T> collection) {
        final int oldSize = storage.size();
        if (storage.addAll(collection)) {
            notifyItemRangeInserted(oldSize, collection.size() - oldSize);
            return true;
        } else {
            return false;
        }
    }

    public boolean add(final T object) {
        final int oldSize = storage.size();
        if (storage.add(object)) {
            notifyItemInserted(oldSize);
            return true;
        } else {
            return false;
        }
    }

    public void add(final T item, final int position) {
        storage.add(position, item);
        notifyItemInserted(position);
    }

    public T remove(final int location) {
        final T removed = storage.remove(location);
        notifyItemRemoved(location);
        return removed;
    }

    public T set(final int location, final T object) {
        final T changed = storage.set(location, object);
        notifyItemChanged(location);
        return changed;
    }

    public int indexOf(final T needle) {
        return storage.indexOf(needle);
    }

    public void clear() {
        final int oldSize = storage.size();
        storage.clear();
        notifyItemRangeRemoved(0, oldSize);
    }

    public void setErrorHandler(@Nullable final ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;

    }

    @SuppressWarnings("unchecked")
    public void restoreState(@NonNull final Bundle savedState) {
        try {
            final Serializable serializable = savedState.getSerializable(KEY_ITEMS);
            if (serializable instanceof Object[]) {
                Collections.addAll(storage, (T[]) serializable);
            }
        } catch (final ClassCastException e) {
            Log.e(getClass().getSimpleName(), "restoreState failed to cast");
            storage.clear();
        }
    }

    public void saveState(@NonNull final Bundle outState) {
        outState.putSerializable(KEY_ITEMS, storage.toArray());
    }
    //endregion


    //region Selection Support

    public void setOnItemClickedListener(@Nullable final OnItemClickedListener<T> onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }

    protected void dispatchItemClicked(final int position) {
        if (onItemClickedListener != null) {
            onItemClickedListener.onItemClicked(position, getItem(position));
        }
    }

    protected void dispatchItemClicked(final int position, @NonNull final T item) {
        if (onItemClickedListener != null) {
            onItemClickedListener.onItemClicked(position, item);
        }
    }

    public interface OnItemClickedListener<T> {
        void onItemClicked(int position, T item);
    }

    @NonNull
    public View inflate(@LayoutRes final int layoutRes, @NonNull final ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
    }
    //endregion


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
        }

        public void bind(final int position) {
            // Do nothing
        }

        @Override
        public void onClick(final View ignored) {
            // View dispatches OnClickListener#onClick(View) calls on
            // the next looper cycle. It's possible for the adapter's
            // containing recycler view to update and invalidate a
            // view holder before the callback fires.
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                dispatchItemClicked(adapterPosition);
            }
        }
    }

    public class BindedViewHolder<T extends android.databinding.ViewDataBinding> extends ViewHolder {
        protected final T binding;

        public BindedViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.binding = DataBindingUtil.bind(itemView);
        }

        public void bind(final int position) {
            // Do nothing
        }
    }

    public class ItemMessageCardViewHolder extends BindedViewHolder<ItemMessageCardBinding> {
        public ItemMessageCardViewHolder(@NonNull final ViewGroup parent) {
            super(inflate(R.layout.item_message_card, parent));
        }

        public ItemMessageCardViewHolder(@NonNull final ViewGroup parent,
                                         @StringRes final int titleRes,
                                         @StringRes final int messageRes,
                                         @StringRes final int actionRes,
                                         @Nullable final View.OnClickListener clickListener) {
            super(inflate(R.layout.item_message_card, parent));
            this.binding.itemMessageCardImageText.setText(titleRes);
            this.binding.itemMessageCardMessage.setText(messageRes);
            this.binding.itemMessageCardAction.setText(actionRes);
            this.binding.itemMessageCardAction.setOnClickListener(clickListener);
        }
    }

    //todo replace all error view holders using item_message_card with this.
    public class ErrorViewHolder extends ItemMessageCardViewHolder
            implements View.OnClickListener {

        public ErrorViewHolder(@NonNull final ViewGroup parent) {
            super(parent);
            this.binding.itemMessageCardAction.setText(R.string.action_retry);
            this.binding.itemMessageCardAction.setOnClickListener(this);
            this.binding.itemMessageCardImageText.setVisibility(View.GONE);
            this.binding.itemMessageCardMessage.setText(R.string.error_internet_connection_generic_message);
        }

        @Override
        public void onClick(final View ignored) {
            super.onClick(ignored);
            if (ArrayRecyclerAdapter.this.errorHandler == null) {
                return;
            }
            ArrayRecyclerAdapter.this.errorHandler.retry();
        }
    }

    public interface ErrorHandler {
        void retry();
    }

}
