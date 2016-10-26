package is.hello.sense.adapter;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;
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
    private static final long UPDATE_DURATION = 100; //ms

    private final LayoutInflater inflater;
    private final int min;
    private final int max;

    private final int textSizeNormal;
    private final int textSizeLarge;

    private int centerItemPosition = 0;
    private boolean animateAbove = false;
    private TextView currentCenter;
    private long lastUpdate = 0;


    public CustomAdapter(@NonNull final LayoutInflater inflater,
                         final int min,
                         final int max) {
        super(new ArrayList<>());
        this.inflater = inflater;
        this.min = min;
        this.max = max;
        this.textSizeNormal = inflater.getContext().getResources().getDimensionPixelSize(R.dimen.text_body);
        this.textSizeLarge = inflater.getContext().getResources().getDimensionPixelSize(R.dimen.text_h4);

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Integer getItem(final int position) {
        return position % (max - min + 1);
        //return min + position + 1;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new BaseViewHolder(inflater.inflate(android.R.layout.simple_list_item_1, parent, false));
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    public void setCenterItemPosition(final int position) {
        if (this.centerItemPosition == position) {
            Log.e("Both", "Are: " + position);
            return;
        }
        Log.e("SetCenter", "Position: " + position);
        if (currentCenter != null) {
            animateTextSize(currentCenter, false);
        }
        animateAbove = centerItemPosition > position;
        this.centerItemPosition = position;
        notifyItemChanged(centerItemPosition);
    }


    public class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final TextView textView;

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.textView = (TextView) itemView.findViewById(android.R.id.text1);
            textView.setTextSize(textSizeNormal);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            if (position == centerItemPosition) {
                //Log.e("Animating", "Position: " + position);
                currentCenter = textView;
                textView.setTextColor(Color.BLUE);
                animateTextSize(textView, true);
            } else if (animateAbove && position == centerItemPosition - 1) {

            } else if (!animateAbove && position == centerItemPosition + 1) {

            } else {
                //    textView.setTextSize(textSizeNormal);
            }
            textView.setText(getItem(position) + "%");
        }
    }

    private void animateTextSize(@NonNull final TextView textView, boolean increase) {
        final ValueAnimator animator;
        if (increase) {
            animator = ValueAnimator.ofFloat(textSizeNormal, textSizeLarge);
        } else {
            animator = ValueAnimator.ofFloat(textSizeLarge, textSizeNormal);
        }
        animator.setDuration(Anime.DURATION_FAST);
        animator.setInterpolator(Anime.INTERPOLATOR_DEFAULT);
        animator.addUpdateListener(animation -> textView.setTextSize((float) animation.getAnimatedValue()));
        animator.start();
    }
}
