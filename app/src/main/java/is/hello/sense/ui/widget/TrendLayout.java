package is.hello.sense.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Annotation;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.GridGraphView;
import is.hello.sense.ui.widget.graphing.TrendCardView;
import is.hello.sense.ui.widget.graphing.drawables.BarGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import rx.functions.Action1;

@SuppressLint("ViewConstructor")
public class TrendLayout extends FrameLayout {

    protected LinearLayout annotationsLayout;
    protected TextView title;
    protected FrameLayout view;
    protected Action1<Graph> graphUpdater;


    public static TrendLayout getGraphItem(@NonNull Context context, @NonNull Graph graph, @NonNull TrendCardView graphView) {
        final Action1<Graph> graphUpdater = graphView::updateGraph;
        TrendLayout view = new TrendLayout(context, graphView, graphUpdater);
        view.setTag(graph.getGraphType());
        view.setTitle(graph.getTitle());
        view.checkForAnnotations(graph.getAnnotations());
        return view;
    }

    public static TrendLayout getGridGraphItem(@NonNull Context context, @NonNull Graph graph, @NonNull GridGraphView graphView) {
        final Action1<Graph> graphUpdater;
        graphView.setGraphAdapter(graph);
        graphUpdater = graphView::setGraphAdapter;
        TrendLayout view = new TrendLayout(context, graphView, graphUpdater);
        view.setTag(graph.getGraphType());
        view.setTitle(graph.getTitle());
        view.checkForAnnotations(graph.getAnnotations());
        return view;
    }

    public static TrendLayout getErrorItem(@NonNull Context context, @NonNull OnRetry onRetry) {
        return new TrendLayout(context, onRetry);
    }


    public TrendLayout(Context context, @NonNull View graphView, @NonNull Action1<Graph> graphUpdate) {
        super(context);
        View inflatedView = LayoutInflater.from(context).inflate(R.layout.item_trend, this);
        title = (TextView) inflatedView.findViewById(R.id.item_trend_title);
        annotationsLayout = (LinearLayout) inflatedView.findViewById(R.id.item_trend_annotations);
        view = (FrameLayout) inflatedView.findViewById(R.id.item_trend_view);
        view.addView(graphView);
        graphUpdater = graphUpdate;
    }

    public TrendLayout(@NonNull Context context, @NonNull OnRetry onRetry) {
        super(context);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_message_card, this);
        view.findViewById(R.id.item_message_card_image).setVisibility(View.GONE);

        final TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
        title.setVisibility(View.GONE);

        final Button action = (Button) view.findViewById(R.id.item_message_card_action);
        action.setText(R.string.action_retry);
        action.setOnClickListener(v -> {
            onRetry.fetchTrends();
        });
        final TextView message = (TextView) view.findViewById(R.id.item_message_card_message);
        message.setText(R.string.error_trends_unavailable);
        view.setPadding(0, getContext().getResources().getDimensionPixelSize(R.dimen.gap_card_vertical), 0, 0);
    }


    private void setTitle(String titleText) {
        title.setText(titleText);
    }

    public void bindGraph(@NonNull Graph graph) {
        graphUpdater.call(graph);
        checkForAnnotations(graph.getAnnotations());
    }

    private void checkForAnnotations(List<Annotation> annotations) {
        if (annotations != null) {
            annotationsLayout.removeAllViews();
            annotationsLayout.setVisibility(VISIBLE);
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            for (Annotation annotation : annotations) {
                //todo handle conditions
                inflater.inflate(R.layout.item_bargraph_annotation, annotationsLayout);
                View annotationView = annotationsLayout.getChildAt(annotationsLayout.getChildCount() - 1);
                ((TextView) annotationView.findViewById(R.id.item_bargraph_annotation_title)).setText(annotation.getTitle().toUpperCase());
                CharSequence value = Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue()),
                                                                   BarGraphDrawable.HOUR_SYMBOL,
                                                                   Styles.UNIT_STYLE_SUBSCRIPT);
                ((TextView) annotationView.findViewById(R.id.item_bargraph_annotation_value)).setText(value);
            }
        } else {
            annotationsLayout.setVisibility(GONE);
        }
    }

    public interface OnRetry {
        void fetchTrends();
    }
}
