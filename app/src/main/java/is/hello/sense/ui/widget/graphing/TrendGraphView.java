package is.hello.sense.ui.widget.graphing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.drawables.TrendGraphDrawable;

@SuppressLint("ViewConstructor")
public class TrendGraphView extends View implements TrendFeedViewItem.OnBindGraph {
    private final TrendGraphDrawable drawable;

    public TrendGraphView(@NonNull Context context,
                          @NonNull TrendGraphDrawable graphDrawable) {
        super(context);

        this.drawable = graphDrawable;

        setBackground(drawable);
        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                      ViewGroup.LayoutParams.WRAP_CONTENT));

        drawable.showGraphAnimation();
    }

    @Override
    public void bindGraph(@NonNull Graph graph) {
        drawable.updateGraph(graph);
    }
}