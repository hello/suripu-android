package is.hello.sense.ui.widget.timeline;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class TimelineNoDataHeaderView extends LinearLayout {
    private final ImageView diagram;
    private final TextView title;
    private final TextView message;
    private final Button action;

    //region Lifecycle

    public TimelineNoDataHeaderView(@NonNull final Context context) {
        this(context, null);
    }

    public TimelineNoDataHeaderView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineNoDataHeaderView(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);
        setBackgroundResource(R.color.background_timeline);

        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.view_timeline_no_data_header, this, true);

        this.diagram = (ImageView) findViewById(R.id.view_timeline_no_data_header_diagram);
        this.title = (TextView) findViewById(R.id.view_timeline_no_data_header_title);
        this.message = (TextView) findViewById(R.id.view_timeline_no_data_header_message);
        this.action = (Button) findViewById(R.id.view_timeline_no_data_header_action);
    }

    //endregion


    //region Attributes

    public void setDiagramResource(@DrawableRes final int diagramRes) {
        diagram.setImageResource(diagramRes);
    }

    public void setTitle(@StringRes final int titleRes) {
        title.setText(titleRes);
    }

    public void setMessage(@StringRes final int messageRes) {
        message.setText(messageRes);
    }

    public void setMessage(@Nullable final CharSequence newMessage) {
        message.setText(newMessage);
    }

    public void setAction(@StringRes final int titleRes, @Nullable final OnClickListener onClick) {
        action.setText(titleRes);
        if (onClick != null) {
            Views.setSafeOnClickListener(action, onClick);
            action.setVisibility(VISIBLE);
        }
    }

    //endregion
}
