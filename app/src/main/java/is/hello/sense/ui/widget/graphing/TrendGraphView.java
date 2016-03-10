package is.hello.sense.ui.widget.graphing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.drawables.BarGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.BubbleGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;

@SuppressLint("ViewConstructor")
public class TrendGraphView extends View implements TrendFeedViewItem.OnBindGraph {
    protected TrendGraphDrawable drawable;

    public TrendGraphView(@NonNull Context context,
                          @NonNull TrendGraphDrawable graphDrawable) {
        super(context);

        this.drawable = graphDrawable;

        setBackground(drawable);
        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                      ViewGroup.LayoutParams.WRAP_CONTENT));

        drawable.showGraphAnimation();
    }

    public TrendGraphView(@NonNull Context context) {
        super(context);

    }

    public Graph getGraph() {
        return drawable.getGraph();
    }

    @Override
    public void bindGraph(@NonNull Graph graph) {
        drawable.updateGraph(graph);
    }


    static class BarTrendGraphView extends TrendGraphView {
        public BarTrendGraphView(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context);
            this.drawable = new BarGraphDrawable(context, graph, animatorContext);
            setBackground(drawable);
            setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            drawable.showGraphAnimation();
        }
    }
    static class BubbleTrendGraphView extends TrendGraphView {
        public BubbleTrendGraphView(@NonNull Context context, @NonNull Graph graph, @NonNull AnimatorContext animatorContext) {
            super(context);
            this.drawable = new BubbleGraphDrawable(context, graph, animatorContext);
            setBackground(drawable);
            setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            drawable.showGraphAnimation();
        }
    }
}