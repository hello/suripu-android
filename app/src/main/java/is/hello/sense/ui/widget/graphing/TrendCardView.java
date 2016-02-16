package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;

public class TrendCardView extends View {
    private TrendGraphDrawable drawable;

    public TrendCardView(@NonNull Context context, @NonNull TrendGraphDrawable graphDrawable) {
        super(context, null);
        this.drawable = graphDrawable;
        setBackground(drawable);
        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        drawable.showGraphAnimation();
    }


    public TrendCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrendCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void updateGraph(@NonNull Graph graph) {
        drawable.updateGraph(graph);
    }
}