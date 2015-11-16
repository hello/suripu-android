package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class RecyclerAdapterTesting {
    @SuppressWarnings("unchecked")
    public static <VH extends RecyclerView.ViewHolder> VH createAndBindView(@NonNull RecyclerView.Adapter adapter,
                                                                            @NonNull ViewGroup parent,
                                                                            int viewId,
                                                                            int adapterPosition) {
        VH holder = spy((VH) adapter.createViewHolder(parent, viewId));
        adapter.bindViewHolder(holder, adapterPosition);
        doReturn(adapterPosition).when(holder).getAdapterPosition();
        return holder;
    }

    public static <VH extends RecyclerView.ViewHolder> VH createAndBindView(@NonNull RecyclerView.Adapter adapter,
                                                                            @NonNull ViewGroup parent,
                                                                            int adapterPosition) {
        return createAndBindView(adapter,
                                 parent,
                                 adapter.getItemViewType(adapterPosition),
                                 adapterPosition);
    }

    public static <VH extends RecyclerView.ViewHolder> void recycle(@NonNull RecyclerView.Adapter<VH> adapter,
                                                                    @NonNull VH holder) {
        doReturn(RecyclerView.NO_POSITION).when(holder).getAdapterPosition();
        adapter.onViewRecycled(holder);
    }

    public static class Observer extends RecyclerView.AdapterDataObserver {
        public final List<Change> changes = new ArrayList<>();

        public void onChanged() {
            changes.add(new Change(Change.Type.DATA_SET_CHANGED));
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            changes.add(new Change(Change.Type.CHANGED, positionStart, itemCount));
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            changes.add(new Change(Change.Type.INSERTED, positionStart, itemCount));
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            changes.add(new Change(Change.Type.REMOVED, positionStart, itemCount));
        }

        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            changes.add(new Change(Change.Type.MOVED, fromPosition, toPosition, itemCount));
        }


        public void reset() {
            changes.clear();
        }

        public boolean hasObservedChange(@NonNull Change.Type type, @NonNull int... values) {
            for (Change change : changes) {
                if (change.type == type && Arrays.equals(change.values, values)) {
                    return true;
                }
            }

            return false;
        }

        public void assertChangeOccurred(@NonNull Change.Type type, @NonNull int... values) {
            if (!hasObservedChange(type, values)) {
                Assert.fail("Expected change '" + type + "' " + Arrays.toString(values));
            }
        }


        public static class Change {
            public final Type type;
            public final int[] values;

            public Change(@NonNull Type type, @NonNull int... values) {
                this.type = type;
                this.values = values;
            }

            public enum Type {
                DATA_SET_CHANGED,
                CHANGED,
                INSERTED,
                REMOVED,
                MOVED,
            }
        }
    }
}
