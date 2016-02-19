package is.hello.sense.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Annotation;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.ui.widget.graphing.GridGraphView;
import is.hello.sense.ui.widget.graphing.TrendCardView;
import is.hello.sense.ui.widget.graphing.drawables.BarGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;
import rx.functions.Action1;

@SuppressLint("ViewConstructor")
public class TrendLayout extends RoundedLinearLayout {
    private LinearLayout annotationsLayout;
    private TextView title;
    private Action1<Graph> graphUpdater;


    public static View getGraphItem(@NonNull Context context, @NonNull Graph graph, @NonNull TrendCardView graphView) {
        final Action1<Graph> graphUpdater = graphView::updateGraph;
        TrendLayout view = new TrendLayout(context, graphView, graphUpdater);
        view.setTag(graph.getGraphType());
        view.setTitle(graph.getTitle());
        view.checkForAnnotations(graph.getAnnotations(), false);
        return view;
    }

    public static View getGridGraphItem(@NonNull Context context, @NonNull Graph graph, @NonNull GridGraphView graphView) {
        final Action1<Graph> graphUpdater = graphView::setGraphAdapter;
        graphView.setGraphAdapter(graph);
        TrendLayout view = new TrendLayout(context, graphView, graphUpdater);
        view.setTag(graph.getGraphType());
        view.setTitle(graph.getTitle());
        view.checkForAnnotations(graph.getAnnotations(), true);
        graphView.setAnnotationView(view.annotationsLayout);
        return view;
    }

    public static View getErrorItem(@NonNull Context context, @NonNull OnRetry onRetry) {
        return new TrendError(context, onRetry);
    }

    public static View getWelcomeItem(@NonNull Context context) {
        View view = new TrendWelcome(context);
        view.setTag(TrendMiscLayout.class);
        return view;
    }

    public static View getComingSoonItem(@NonNull Context context, int days) {
        View view = new TrendComingSoon(context, days);
        view.setTag(TrendMiscLayout.class);
        return view;
    }


    private TrendLayout(@NonNull Context context,
                        @NonNull View graphView,
                        @NonNull Action1<Graph> graphUpdate) {
        super(context);

        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.raised_item_normal);

        final Resources resources = getResources();
        final int padding = resources.getDimensionPixelSize(R.dimen.gap_card_content);
        setPadding(padding, 0, padding, padding);

        final int margin = resources.getDimensionPixelSize(R.dimen.gap_outer_half);
        final LayoutParams myLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                             LayoutParams.WRAP_CONTENT);
        myLayoutParams.topMargin = margin;
        setLayoutParams(myLayoutParams);

        final float cornerRadius = resources.getDimension(R.dimen.raised_item_corner_radius);
        setCornerRadii(cornerRadius);

        LayoutInflater.from(context).inflate(R.layout.item_trend, this);
        this.title = (TextView) findViewById(R.id.item_trend_title);
        this.annotationsLayout = (LinearLayout) findViewById(R.id.item_trend_annotations);
        this.graphUpdater = graphUpdate;

        final int annotationsPosition = indexOfChild(annotationsLayout);
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                           LayoutParams.WRAP_CONTENT);
        addView(graphView, annotationsPosition, layoutParams);
    }

    private void setTitle(String titleText) {
        title.setText(titleText);
    }

    public void bindGraph(@NonNull Graph graph) {
        graphUpdater.call(graph);
        checkForAnnotations(graph.getAnnotations(), graph.getGraphType() == Graph.GraphType.GRID);
    }

    private void checkForAnnotations(List<Annotation> annotations, boolean isGrid) {
        if (annotations != null) {
            annotationsLayout.removeAllViews();
            annotationsLayout.setVisibility(VISIBLE);
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            for (Annotation annotation : annotations) {

                inflater.inflate(R.layout.item_bargraph_annotation, annotationsLayout);
                View annotationView = annotationsLayout.getChildAt(annotationsLayout.getChildCount() - 1);
                ((TextView) annotationView.findViewById(R.id.item_bargraph_annotation_title)).setText(annotation.getTitle().toUpperCase());
                CharSequence value = Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue()),
                                                                   BarGraphDrawable.HOUR_SYMBOL,
                                                                   Styles.UNIT_STYLE_SUBSCRIPT);
                TextView valueTextView = ((TextView) annotationView.findViewById(R.id.item_bargraph_annotation_value));
                if (isGrid) {
                    String condition = annotation.getCondition();
                    if (condition == null) {
                        condition = "";
                    }
                    valueTextView.setTextColor(ContextCompat.getColor(getContext(), Condition.fromString(condition).colorRes));
                } else {
                    valueTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.trends_bargraph_annotation_text));
                }
                valueTextView.setText(value);
            }
        } else {
            annotationsLayout.setVisibility(GONE);
        }
    }

    public interface OnRetry {
        void fetchTrends();
    }

    public static class TrendMiscLayout extends FrameLayout {
        protected final ImageView image;
        protected final TextView title;
        protected final TextView message;
        protected final Button action;

        public TrendMiscLayout(@NonNull Context context) {
            super(context);
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_message_card, this);
            image = (ImageView) findViewById(R.id.item_message_card_image);
            title = (TextView) findViewById(R.id.item_message_card_title);
            message = (TextView) findViewById(R.id.item_message_card_message);
            action = (Button) findViewById(R.id.item_message_card_action);
            view.setPadding(0, getContext().getResources().getDimensionPixelSize(R.dimen.gap_card_vertical), 0, 0);
        }
    }

    public static class TrendError extends TrendMiscLayout {
        public TrendError(@NonNull Context context, @NonNull OnRetry onRetry) {
            super(context);
            title.setVisibility(View.GONE);

            action.setText(R.string.action_retry);
            action.setOnClickListener(v -> {
                onRetry.fetchTrends();
            });
            message.setText(R.string.error_trends_unavailable);
        }
    }

    public static class TrendWelcome extends TrendMiscLayout {

        public TrendWelcome(@NonNull Context context) {
            super(context);
            title.setGravity(Gravity.CENTER_HORIZONTAL);
            title.setPadding(0, 0, 0, 0);
            title.getTotalPaddingTop();
            title.setText(getResources().getString(R.string.title_trends_welcome));
            image.setImageResource(R.drawable.trends_first_day);
            ((LinearLayout.LayoutParams) message.getLayoutParams()).topMargin = 0;
            message.setText(getResources().getString(R.string.message_trends_welcome));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //noinspection deprecation
                message.setTextAppearance(getContext(), R.style.AppTheme_Text_Body_Small_New);
            } else {
                message.setTextAppearance(R.style.AppTheme_Text_Body_Small_New);
            }
            message.setLineSpacing(0, 1);
            action.setVisibility(GONE);
        }
    }

    public static class TrendComingSoon extends TrendWelcome {

        public TrendComingSoon(@NonNull Context context, int days) {
            super(context);
            title.setText(getResources().getString(R.string.title_trends_coming_soon));
            CharSequence styledText = Html.fromHtml(getResources().getQuantityString(R.plurals.message_trends_coming_soon, days, days + ""));
            message.setText(styledText);
        }
    }
}
