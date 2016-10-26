package is.hello.sense.adapter;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;


/**
 * todo rename this file
 */
public class CustomAdapter extends ArrayRecyclerAdapter<Integer, CustomAdapter.BaseViewHolder> {

    private static final float MIN_SCALE = 0.7f;
    private static final float MAX_SCALE = 1.5f;

    private static final float MIN_ALPHA = 0.2f;
    private static final float MAX_ALPHA = 1f;

    private final LayoutInflater inflater;
    private final int min;
    private final int difference;
    private final String symbol;
    private final int selectedColor;
    private final int normalColor;


    private int currentCenter;

    public CustomAdapter(@NonNull final LayoutInflater inflater,
                         final int min,
                         final int max,
                         final String symbol) {
        super(new ArrayList<>());
        this.inflater = inflater;
        this.min = min;
        this.difference = max - min + 1;
        this.symbol = symbol;
        this.selectedColor = ContextCompat.getColor(this.inflater.getContext(), R.color.primary);
        this.normalColor = ContextCompat.getColor(this.inflater.getContext(), R.color.standard);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return difference;
    }

    @Override
    public Integer getItem(final int position) {
        return (position % difference) + min;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new BaseViewHolder(inflater.inflate(R.layout.custom_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    public CustomInsetDecoration getDecorationWithInset() {
        return new CustomInsetDecoration();
    }


    public class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final TextView textView;

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.textView = (TextView) itemView.findViewById(R.id.custom_item_text);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            if (currentCenter == position) {
                textView.setTextColor(selectedColor);
            } else {
                textView.setTextColor(normalColor);
            }
            textView.setText(inflater.getContext()
                                     .getResources()
                                     .getString(R.string.custom_adapter_item,
                                                getItem(position), symbol));
        }
    }


    /**
     * This is unique to this adapter. The decoration is currently responsible for deciding what
     * the selected value is since it's measuring the distance of each item relative to the center
     * of the parent.
     * <p>
     * todo Rename this with CustomAdapter
     */
    public class CustomInsetDecoration extends RecyclerView.ItemDecoration {

        @Override
        public void getItemOffsets(final Rect outRect,
                                   final View view,
                                   final RecyclerView parent,
                                   final RecyclerView.State state) {
            final int padding = parent.getMeasuredHeight() / 2;
            final int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.top += padding;
            } else if (position == getItemCount() - 1) {
                outRect.bottom += padding;
            }
        }

        @Override
        public void onDraw(final Canvas c,
                           final RecyclerView parent,
                           final RecyclerView.State state) {
            super.onDraw(c, parent, state);
            final float recyclerCenter = parent.getHeight() / 2;
            float greatestDistance = 0;
            int tempCenter = 0;
            for (int i = 0, size = parent.getChildCount(); i < size; i++) {
                final View child = parent.getChildAt(i);
                final float childCenter = (child.getTop() + child.getBottom()) / 2;
                final float distanceAmount = 1f - Math.abs((childCenter - recyclerCenter) / recyclerCenter);
                final float childScale = Anime.interpolateFloats(distanceAmount, MIN_SCALE, MAX_SCALE);
                final float childAlpha = Anime.interpolateFloats(distanceAmount, MIN_ALPHA, MAX_ALPHA);
                child.setScaleX(childScale);
                child.setScaleY(childScale);
                child.setAlpha(childAlpha);
                if (distanceAmount > greatestDistance) {
                    greatestDistance = distanceAmount;
                    tempCenter = parent.getChildAdapterPosition(child);
                }
            }
            final int oldCenter = currentCenter;
            currentCenter = tempCenter;
            notifyItemChanged(oldCenter);
            notifyItemChanged(currentCenter);
        }
    }
}
