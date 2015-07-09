package is.hello.sense.ui.widget.timeline;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class TimelineToolbar extends RelativeLayout {
    private final ImageButton overflow;
    private final ImageButton share;
    private final TextView title;


    //region Lifecycle

    public TimelineToolbar(@NonNull Context context) {
        this(context, null);
    }

    public TimelineToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_timeline_toolbar, this, true);

        this.overflow = (ImageButton) findViewById(R.id.view_timeline_toolbar_overflow);
        this.share = (ImageButton) findViewById(R.id.view_timeline_toolbar_share);
        this.title = (TextView) findViewById(R.id.view_timeline_toolbar_title);
    }

    //endregion


    //region Attributes

    public void setOverflowOnClickListener(@NonNull OnClickListener onClickListener) {
        Views.setSafeOnClickListener(overflow, onClickListener);
    }

    public void setOverflowOpen(boolean isOpen) {
        if (isOpen) {
            overflow.setImageResource(R.drawable.icon_menu_open);
        } else {
            overflow.setImageResource(R.drawable.icon_menu_closed);
        }
    }

    public void setShareOnClickListener(@NonNull OnClickListener onClickListener) {
        Views.setSafeOnClickListener(share, onClickListener);
    }

    public void setShareVisible(boolean visible) {
        if (visible) {
            share.setVisibility(VISIBLE);
        } else {
            share.setVisibility(INVISIBLE);
        }
    }

    public void setTitleOnClickListener(@NonNull OnClickListener onClickListener) {
        Views.setSafeOnClickListener(title, onClickListener);
    }

    public void setTitle(@Nullable CharSequence text) {
        title.setText(text);
    }

    public void setTitleDimmed(boolean dimmed) {
        if (dimmed) {
            title.setTextColor(getResources().getColor(R.color.text_dim));
        } else {
            title.setTextColor(getResources().getColor(R.color.text_dark));
        }
    }

    //endregion
}
