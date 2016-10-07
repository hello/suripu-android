package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Collection;
import java.util.List;

public abstract class ArrayRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private final List<T> storage;
    private
    @Nullable
    OnItemClickedListener<T> onItemClickedListener;

    protected ArrayRecyclerAdapter(@NonNull List<T> storage) {
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

    public T getItem(int position) {
        return storage.get(position);
    }

    public boolean replaceAll(@NonNull Collection<? extends T> collection) {
        int oldSize = getItemCount();
        int newSize = collection.size();

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

    public boolean addAll(@NonNull Collection<? extends T> collection) {
        int oldSize = storage.size();
        if (storage.addAll(collection)) {
            notifyItemRangeInserted(oldSize, collection.size() - oldSize);
            return true;
        } else {
            return false;
        }
    }

    public boolean add(T object) {
        int oldSize = storage.size();
        if (storage.add(object)) {
            notifyItemInserted(oldSize);
            return true;
        } else {
            return false;
        }
    }

    public T remove(int location) {
        T removed = storage.remove(location);
        notifyItemRemoved(location);
        return removed;
    }

    public T set(int location, T object) {
        T changed = storage.set(location, object);
        notifyItemChanged(location);
        return changed;
    }

    public int indexOf(T needle) {
        return storage.indexOf(needle);
    }

    public void clear() {
        int oldSize = storage.size();
        storage.clear();
        notifyItemRangeRemoved(0, oldSize);
    }

    //endregion


    //region Selection Support

    public void setOnItemClickedListener(@Nullable OnItemClickedListener<T> onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }

    protected void dispatchItemClicked(int position) {
        if (onItemClickedListener != null) {
            onItemClickedListener.onItemClicked(position, getItem(position));
        }
    }

    protected void dispatchItemClicked(int position, @NonNull final T item) {
        if (onItemClickedListener != null) {
            onItemClickedListener.onItemClicked(position, item);
        }
    }

    public interface OnItemClickedListener<T> {
        void onItemClicked(int position, T item);
    }

    //endregion


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(int position) {
            // Do nothing
        }

        @Override
        public void onClick(View ignored) {
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
}
