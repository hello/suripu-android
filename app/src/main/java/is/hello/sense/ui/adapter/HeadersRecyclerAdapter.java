package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public abstract class HeadersRecyclerAdapter<VH extends ContentViewHolder>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_BASE_HEADER = Integer.MIN_VALUE;
    public static final int VIEW_TYPE_CONTENT = 0;

    private final List<View> headers = new ArrayList<>();

    //region Providing Content

    public abstract int getContentItemCount();
    public int getContentItemViewType(int position) {
        return VIEW_TYPE_CONTENT;
    }

    public abstract VH onCreateContentViewHolder(ViewGroup parent, int viewType);
    public abstract void onBindContentViewHolder(VH viewHolder, int position);
    public void onContentViewRecycled(VH viewHolder) {
        // Do nothing.
    }

    //endregion


    //region Headers

    public int getHeaderCount() {
        return headers.size();
    }

    public View getHeaderAt(int position) {
        return headers.get(position);
    }

    public void addHeader(@NonNull View header) {
        final int oldHeadersSize = getHeaderCount();
        headers.add(header);
        notifyItemInserted(oldHeadersSize);
    }

    public void replaceHeader(int position, @NonNull View header) {
        headers.set(position, header);
        notifyItemChanged(position);
    }

    public void removeHeader(int position) {
        headers.remove(position);
        notifyItemRemoved(position);
    }

    public void clearHeaders() {
        final int oldSize = getHeaderCount();
        headers.clear();
        notifyItemRangeChanged(0, oldSize);
    }

    //endregion


    //region Data

    @Override
    public final int getItemCount() {
        return getContentItemCount() + getHeaderCount();
    }

    @Override
    public final int getItemViewType(int position) {
        if (position < headers.size()) {
            return VIEW_TYPE_BASE_HEADER + position;
        } else {
            return getContentItemViewType(position - getHeaderCount());
        }
    }

    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType < VIEW_TYPE_CONTENT) {
            final FrameLayout root = new FrameLayout(parent.getContext());
            root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                            ViewGroup.LayoutParams.WRAP_CONTENT));
            return new HeaderViewHolder(root);
        } else {
            return onCreateContentViewHolder(parent, viewType);
        }
    }

    @Override
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(getHeaderAt(position));
        } else {
            @SuppressWarnings("unchecked")
            final VH contentViewHolder = (VH) holder;
            final int contentPosition = (position - getHeaderCount());
            contentViewHolder.setContentPosition(contentPosition);
            onBindContentViewHolder(contentViewHolder, contentPosition);
        }
    }

    @Override
    public final void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).recycle();
        } else {
            @SuppressWarnings("unchecked")
            final VH contentViewHolder = (VH) holder;
            contentViewHolder.setContentPosition(RecyclerView.NO_POSITION);
            onContentViewRecycled(contentViewHolder);
        }
    }

    //endregion


    //region Changes

    public void notifyContentItemChanged(int position) {
        notifyItemChanged(position + getHeaderCount());
    }

    public void notifyContentItemRangeChanged(int start, int count) {
        notifyItemRangeChanged(start + getHeaderCount(), count);
    }

    public void notifyContentItemInserted(int position) {
        notifyItemInserted(position + getHeaderCount());
    }

    public void notifyContentItemRangeInserted(int start, int count) {
        notifyItemRangeInserted(start + getHeaderCount(), count);
    }

    public void notifyContentItemRemoved(int position) {
        notifyItemRemoved(position + getHeaderCount());
    }

    public void notifyContentItemRangeRemoved(int start, int count) {
        notifyItemRangeRemoved(start + getHeaderCount(), count);
    }

    //endregion


    //region View Holders

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        static final FrameLayout.LayoutParams CONTENT_LAYOUT_PARAMS =
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                             FrameLayout.LayoutParams.WRAP_CONTENT);
        final FrameLayout container;

        HeaderViewHolder(@NonNull FrameLayout container) {
            super(container);

            this.container = container;
        }

        void bind(@NonNull View view) {
            if (view.getParent() != container) {
                container.removeAllViews();
                final ViewGroup oldParent = (ViewGroup) view.getParent();
                if (oldParent != null) {
                    oldParent.removeView(view);
                }
                container.addView(view, CONTENT_LAYOUT_PARAMS);
            }
        }

        void recycle() {
            container.removeAllViews();
        }
    }

    //endregion
}
