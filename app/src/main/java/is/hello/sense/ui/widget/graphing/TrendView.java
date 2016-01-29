package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;

public class TrendView extends View {
    private TrendGraphDrawable drawable;

    public TrendView(@NonNull Context context, @NonNull TrendGraphDrawable graphDrawable) {
        super(context, null);
        this.drawable = graphDrawable;
        drawable.setBounds(0, 0, getWidth(), drawable.getIntrinsicHeight());
        setBackground(drawable);
        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        drawable.showGraphAnimation();
        invalidate();
    }


    public TrendView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrendView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void updateGraph(@NonNull Graph graph) {
        drawable.updateGraph(graph);
    }
}