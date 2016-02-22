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
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.graphing.GridGraphView;
import is.hello.sense.ui.widget.graphing.TrendGraphView;
import is.hello.sense.ui.widget.graphing.drawables.BarGraphDrawable;
import is.hello.sense.ui.widget.util.Styles;

@SuppressLint("ViewConstructor")
public class TrendCardView extends RoundedLinearLayout {
    private final LinearLayout annotationsLayout;
    private final TextView title;
    private final OnBindGraph graphBinder;


    public static TrendCardView createGraphCard(@NonNull TrendGraphView graphView,
                                                @NonNull Graph graph) {
        final TrendCardView cardView = new TrendCardView(graphView.getContext(), graphView, graphView);
        cardView.setTitle(graph.getTitle());
        cardView.populateAnnotations(graph.getAnnotations(), false);
        return cardView;
    }

    public static TrendCardView createGridCard(@NonNull GridGraphView graphView,
                                               @NonNull Graph graph) {
        final TrendCardView cardView = new TrendCardView(graphView.getContext(), graphView, graphView);
        cardView.setTitle(graph.getTitle());
        cardView.populateAnnotations(graph.getAnnotations(), true);
        graphView.bindGraph(graph);
        return cardView;
    }

    public static ErrorCardView createErrorCard(@NonNull Context context,
                                                @NonNull OnRetry onRetry) {
        return new ErrorCardView(context, onRetry);
    }

    public static WelcomeCardView createWelcomeCard(@NonNull Context context) {
        return new WelcomeCardView(context);
    }

    public static ComingSoonCardView createComingSoonCard(@NonNull Context context, int days) {
        return new ComingSoonCardView(context, days);
    }


    private TrendCardView(@NonNull Context context,
                          @NonNull View graphView,
                          @NonNull OnBindGraph graphBinder) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.item_trend, this);

        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.raised_item_normal);

        final Resources resources = getResources();
        final int padding = resources.getDimensionPixelSize(R.dimen.gap_card_content);
        setPadding(padding, 0, padding, padding);

        final LayoutParams myLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                             LayoutParams.WRAP_CONTENT);
        myLayoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.gap_outer_half);
        setLayoutParams(myLayoutParams);

        final float cornerRadius = resources.getDimension(R.dimen.raised_item_corner_radius);
        setCornerRadii(cornerRadius);

        this.title = (TextView) findViewById(R.id.item_trend_title);
        this.graphBinder = graphBinder;
        this.annotationsLayout = new LinearLayout(context);

        final LayoutParams annotationsLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                      LayoutParams.WRAP_CONTENT);
        annotationsLayoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.gap_card_content);

        final LayoutParams graphLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                LayoutParams.WRAP_CONTENT);
        if (graphView instanceof GridGraphView) {
            final GridGraphView gridGraphView = (GridGraphView) graphView;
            gridGraphView.addView(annotationsLayout, annotationsLayoutParams);
            addView(gridGraphView);
        } else {
            addView(graphView, graphLayoutParams);
            addView(annotationsLayout, annotationsLayoutParams);
        }
    }

    public void setTitle(String titleText) {
        title.setText(titleText);
    }

    public void bindGraph(@NonNull Graph graph) {
        populateAnnotations(graph.getAnnotations(), graph.getGraphType() == Graph.GraphType.GRID);
        graphBinder.bindGraph(graph);
    }

    private void populateAnnotations(List<Annotation> annotations, boolean isGrid) {
        annotationsLayout.removeAllViews();

        if (!Lists.isEmpty(annotations)) {
            annotationsLayout.setVisibility(VISIBLE);

            final LayoutInflater inflater = LayoutInflater.from(getContext());
            for (Annotation annotation : annotations) {
                inflater.inflate(R.layout.item_bargraph_annotation, annotationsLayout);
                final View annotationView = annotationsLayout.getChildAt(annotationsLayout.getChildCount() - 1);

                final TextView titleText =
                        (TextView) annotationView.findViewById(R.id.item_bargraph_annotation_title);
                titleText.setText(annotation.getTitle().toUpperCase());

                final CharSequence value =
                        Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue()),
                                                      BarGraphDrawable.HOUR_SYMBOL,
                                                      Styles.UNIT_STYLE_SUBSCRIPT);
                final TextView valueText =
                        ((TextView) annotationView.findViewById(R.id.item_bargraph_annotation_value));
                if (isGrid) {
                    final Condition condition = annotation.getCondition();
                    valueText.setTextColor(ContextCompat.getColor(getContext(), condition.colorRes));
                } else {
                    valueText.setTextColor(ContextCompat.getColor(getContext(), R.color.trends_bargraph_annotation_text));
                }
                valueText.setText(value);
            }
        } else {
            annotationsLayout.setVisibility(GONE);
        }
    }

    public interface OnRetry {
        void fetchTrends();
    }

    abstract static class StaticCardLayout extends FrameLayout {
        protected final ImageView image;
        protected final TextView title;
        protected final TextView message;
        protected final Button action;

        StaticCardLayout(@NonNull Context context) {
            super(context);

            final View view = LayoutInflater.from(getContext()).inflate(R.layout.item_message_card, this);
            this.image = (ImageView) findViewById(R.id.item_message_card_image);
            this.title = (TextView) findViewById(R.id.item_message_card_title);
            this.message = (TextView) findViewById(R.id.item_message_card_message);
            this.action = (Button) findViewById(R.id.item_message_card_action);
            view.setPadding(0, getContext().getResources().getDimensionPixelSize(R.dimen.gap_card_vertical), 0, 0);
        }
    }

    static class ErrorCardView extends StaticCardLayout {
        ErrorCardView(@NonNull Context context, @NonNull OnRetry onRetry) {
            super(context);

            title.setVisibility(View.GONE);

            action.setText(R.string.action_retry);
            action.setOnClickListener(v -> {
                onRetry.fetchTrends();
            });
            message.setText(R.string.error_trends_unavailable);
        }
    }

    static class WelcomeCardView extends StaticCardLayout {
        WelcomeCardView(@NonNull Context context) {
            super(context);

            title.setGravity(Gravity.CENTER_HORIZONTAL);
            title.setPadding(0, 0, 0, 0);
            title.getTotalPaddingTop();
            title.setText(getResources().getString(R.string.title_trends_welcome));
            image.setImageResource(R.drawable.trends_first_day);

            ((MarginLayoutParams) message.getLayoutParams()).topMargin = 0;
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

    static class ComingSoonCardView extends WelcomeCardView {
        ComingSoonCardView(@NonNull Context context, int days) {
            super(context);
            title.setText(getResources().getString(R.string.title_trends_coming_soon));
            final CharSequence styledText = Html.fromHtml(getResources().getQuantityString(R.plurals.message_trends_coming_soon, days, days + ""));
            message.setText(styledText);
        }
    }

    public interface OnBindGraph {
        void bindGraph(@NonNull Graph graph);
    }
}
