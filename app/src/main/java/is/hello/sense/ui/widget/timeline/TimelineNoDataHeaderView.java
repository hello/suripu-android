package is.hello.sense.ui.widget.timeline;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.R;

public class TimelineNoDataHeaderView extends LinearLayout {
    private final ImageView diagram;
    private final TextView title;
    private final TextView message;

    //region Lifecycle

    public TimelineNoDataHeaderView(@NonNull Context context) {
        this(context, null);
    }

    public TimelineNoDataHeaderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineNoDataHeaderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);
        setBackgroundResource(R.color.background_timeline);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.view_timeline_no_data_header, this, true);

        this.diagram = (ImageView) findViewById(R.id.view_timeline_no_data_header_diagram);
        this.title = (TextView) findViewById(R.id.view_timeline_no_data_header_title);
        this.message = (TextView) findViewById(R.id.view_timeline_no_data_header_message);
    }

    //endregion


    //region Attributes

    public void setDiagramResource(@DrawableRes int diagramRes) {
        diagram.setImageResource(diagramRes);
    }

    public void setTitle(@StringRes int titleRes) {
        title.setText(titleRes);
    }

    public void setMessage(@StringRes int messageRes) {
        message.setText(messageRes);
    }

    public void setMessage(@Nullable CharSequence newMessage) {
        message.setText(newMessage);
    }

    //endregion
}
