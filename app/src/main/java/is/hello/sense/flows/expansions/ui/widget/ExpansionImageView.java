package is.hello.sense.flows.expansions.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;

public class ExpansionImageView extends ImageView {
    private static final int NUMBER_OF_CHARS = 2;
    private final ExpansionTextDrawable drawable;

    public ExpansionImageView(final Context context) {
        this(context, null);
    }

    public ExpansionImageView(final Context context,
                              final AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public ExpansionImageView(final Context context,
                              final AttributeSet attrs,
                              final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.drawable = new ExpansionTextDrawable(context);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                     ViewGroup.LayoutParams.MATCH_PARENT));
        setBackground(this.drawable);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                ExpansionImageView.this.drawable.setDimensions(getMeasuredWidth(), getMeasuredHeight());
            }
        });
    }

    /**
     * Will show the first two letters of the String passed in.
     *
     * @param text if null or empty will render the old default image icon instead.
     */
    public void setText(@Nullable final String text) {
        if (text == null || text.isEmpty()) {
            setImageResource(R.drawable.icon_expansions_default);
        } else if (text.length() > NUMBER_OF_CHARS) {
            this.drawable.setText(text.substring(0, NUMBER_OF_CHARS).toUpperCase());
        } else {
            this.drawable.setText(text.toUpperCase());
        }
    }

    /**
     * Will render the expansion into this view.
     *
     * @param picasso   should be provided from dagger.
     * @param expansion expansion to render.
     */
    public void setExpansion(@NonNull final Picasso picasso,
                             @NonNull final Expansion expansion) {

        // The following code will be used in at least two places. Rather than duplicate it lets
        // use a function.
        setText(expansion.getCompanyName());
        picasso.cancelRequest(this);
        picasso.load(expansion.getIcon().getUrl(getResources()))
               .into(this, new Callback() {
                   @Override
                   public void onSuccess() {
                       ExpansionImageView.this.setBackground(null);
                   }

                   @Override
                   public void onError() {
                   }
               });

    }

}
