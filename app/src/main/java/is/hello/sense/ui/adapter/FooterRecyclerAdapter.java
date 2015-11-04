package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class FooterRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static int VIEW_ID_FOOTER = Integer.MIN_VALUE;

    private final FrameLayout.LayoutParams layoutParams =
            new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                         FrameLayout.LayoutParams.WRAP_CONTENT);

    @VisibleForTesting final List<View> footers = new ArrayList<>();
    @VisibleForTesting final RecyclerView.Adapter adapter;
    private boolean flattenChanges = false;

    public FooterRecyclerAdapter(@NonNull RecyclerView.Adapter<?> adapter) {
        if (adapter instanceof FooterRecyclerAdapter) {
            throw new IllegalArgumentException("Cannot nest HeaderRecyclerAdapter.");
        }

        this.adapter = adapter;
        adapter.registerAdapterDataObserver(new ForwardingObserver());
    }


    //region Footers

    public FooterRecyclerAdapter addFooter(@NonNull View footer) {
        int oldSize = getItemCount();
        footers.add(footer);
        notifyItemInserted(oldSize);
        return this;
    }

    public FooterRecyclerAdapter setFlattenChanges(boolean flattenChanges) {
        this.flattenChanges = flattenChanges;
        return this;
    }

    //endregion


    //region Population

    @Override
    public int getItemCount() {
        return footers.size() + adapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= adapter.getItemCount()) {
            return VIEW_ID_FOOTER;
        } else {
            return adapter.getItemViewType(position);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position >= adapter.getItemCount()) {
            int shiftedPosition = position - adapter.getItemCount();
            return Long.MAX_VALUE - shiftedPosition;
        } else {
            return adapter.getItemViewType(position);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_ID_FOOTER) {
            FrameLayout frameLayout = new FrameLayout(parent.getContext());
            frameLayout.setLayoutParams(layoutParams);
            return new FooterViewHolder(frameLayout);
        } else {
            return adapter.createViewHolder(parent, viewType);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterViewHolder) {
            final int shiftedPosition = position - adapter.getItemCount();
            final View view = footers.get(shiftedPosition);

            final FrameLayout root = ((FooterViewHolder) holder).root;
            if (view.getParent() != root) {
                root.removeAllViews();
                root.addView(view, layoutParams);
            }
        } else {
            adapter.bindViewHolder(holder, position);
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof FooterViewHolder) {
            final FrameLayout root = ((FooterViewHolder) holder).root;
            root.removeAllViews();
        }
    }

    //endregion


    static class FooterViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout root;

        FooterViewHolder(@NonNull FrameLayout itemView) {
            super(itemView);

            this.root = itemView;
        }
    }

    class ForwardingObserver extends RecyclerView.AdapterDataObserver {
        public void onChanged() {
            notifyDataSetChanged();
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (flattenChanges) {
                notifyDataSetChanged();
            } else {
                notifyItemRangeChanged(positionStart, itemCount);
            }
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (flattenChanges) {
                notifyDataSetChanged();
            } else {
                notifyItemRangeInserted(positionStart, itemCount);
            }
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (flattenChanges) {
                notifyDataSetChanged();
            } else {
                notifyItemRangeRemoved(positionStart, itemCount);
            }
        }

        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (flattenChanges) {
                notifyDataSetChanged();
            } else {
                for (int i = 0; i < itemCount; i++) {
                    notifyItemMoved(fromPosition + i, toPosition + i);
                }
            }
        }
    }
}
