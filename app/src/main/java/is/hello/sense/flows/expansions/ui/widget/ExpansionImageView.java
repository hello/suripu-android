package is.hello.sense.flows.expansions.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.ui.widget.util.RoundedCornersTransformation;

public class ExpansionImageView extends ImageView {
    private static final int NUMBER_OF_CHARS = 2;
    private final ExpansionTextDrawable drawable;
    private final RoundedCornersTransformation transformation;

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
        this.transformation = new RoundedCornersTransformation(this.drawable.getRadius(),
                                                               this.drawable.getBorderWidth(),
                                                               this.drawable.getBorderColor());
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                     ViewGroup.LayoutParams.MATCH_PARENT));
        this.drawable.setDimensions(getMinimumWidth(), getMinimumHeight());
    }

    @Override
    protected void onSizeChanged(final int width,
                                 final int height,
                                 final int oldWidth,
                                 final int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if(width != oldWidth || height != oldHeight) {
            this.drawable.setDimensions(width, height);
        }
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
        setText(expansion.getCompanyName());
        picasso.cancelRequest(this);
        picasso.load(expansion.getIcon().getUrl(getResources()))
               .transform(transformation)
               .placeholder(this.drawable)
               .into(this);

    }

}
