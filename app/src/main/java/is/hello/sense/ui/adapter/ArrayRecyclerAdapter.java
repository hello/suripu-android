package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ArrayRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private final List<T> storage = new ArrayList<>();

    protected ArrayRecyclerAdapter() {

    }

    protected ArrayRecyclerAdapter(@NonNull Collection<T> source) {
        this();
        storage.addAll(source);
    }


    @Override
    public int getItemCount() {
        return storage.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public T getItem(int position) {
        return storage.get(position);
    }

    public void add(T object) {
        storage.add(object);
        notifyItemInserted(storage.size());
    }

    public void addAll(@NonNull Collection<? extends T> collection) {
        int oldSize = storage.size();
        storage.addAll(collection);
        notifyItemRangeInserted(oldSize, oldSize + collection.size());
    }

    public void clear() {
        int oldSize = storage.size();
        storage.clear();
        notifyItemRangeRemoved(0, oldSize);
    }

    public void insert(T object, int index) {
        storage.add(index, object);
        notifyItemInserted(index);
    }

    public void remove(@NonNull T object) {
        int indexOfObject = storage.indexOf(object);
        if (indexOfObject != -1) {
            storage.remove(indexOfObject);
            notifyItemRemoved(indexOfObject);
        }
    }
}
