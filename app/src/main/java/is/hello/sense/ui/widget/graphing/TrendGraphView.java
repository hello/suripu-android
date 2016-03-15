package is.hello.sense.ui.widget.graphing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.drawables.BubbleGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;

@SuppressLint("ViewConstructor")
public class TrendGraphView extends View implements TrendFeedViewItem.OnBindGraph {
    protected TrendGraphDrawable drawable;
    protected AnimatorContext animatorContext;
    protected static final float maxAnimationFactor = 1f;
    protected static final float minAnimationFactor = 0;


    protected TrendGraphView(@NonNull Context context, @NonNull AnimatorContext animatorContext) {
        super(context);
        this.animatorContext = animatorContext;

    }

    public Graph getGraph() {
        return drawable.getGraph();
    }

    @Override
    public void bindGraph(@NonNull Graph graph) {
        drawable.updateGraph(graph);
    }



    static class BubbleTrendGraphView extends TrendGraphView {
        public BubbleTrendGraphView(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context, animatorContext);
            this.drawable = new BubbleGraphDrawable(context, graph, animatorContext);
            setBackground(drawable);
            setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            drawable.showGraphAnimation();
        }
    }

}