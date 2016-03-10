package is.hello.sense.ui.widget.graphing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import is.hello.sense.ui.widget.RoundedLinearLayout;
import is.hello.sense.ui.widget.ShimmerDividerDrawable;

@SuppressLint("ViewConstructor")
public class TrendFeedViewItem extends RoundedLinearLayout {
    private final LinearLayout annotationsLayout;
    private final TextView title;
    private final ShimmerDividerDrawable dividerDrawable;
    private final OnBindGraph graphBinder;

    public static TrendFeedViewItem createGridCard(@NonNull GridGraphView graphView,
                                                   @NonNull Graph graph) {
        graphView.bindGraph(graph);
        final TrendFeedViewItem cardView = new TrendFeedViewItem(graphView.getContext(), graphView, graphView);
        cardView.setTitle(graph.getTitle());
        cardView.populateAnnotations(graph.getDataType(), graph.getAnnotations());
        return cardView;
    }

    public static TrendFeedViewItem createMultiGridCard(@NonNull MultiGridGraphView graphView,
                                                        @NonNull Graph graph) {
        graphView.bindGraph(graph);
        final TrendFeedViewItem cardView = new TrendFeedViewItem(graphView.getContext(), graphView, graphView);
        cardView.setTitle(graph.getTitle());
        cardView.populateAnnotations(graph.getDataType(), graph.getAnnotations());
        return cardView;
    }

    public static ErrorCardView createErrorCard(@NonNull Context context,
                                                @NonNull OnRetry onRetry) {
        return new ErrorCardView(context, onRetry);
    }

    public static WelcomeCardView createWelcomeCard(@NonNull Context context) {
        return new WelcomeCardView(context);
    }

    public static WelcomeBackCardView createWelcomeBackCard(@NonNull Context context) {
        return new WelcomeBackCardView(context);
    }

    public static ComingSoonCardView createComingSoonCard(@NonNull Context context, int days) {
        return new ComingSoonCardView(context, days);
    }

    public TrendFeedViewItem(@NonNull TrendGraphView trendGraphView) {
        super(trendGraphView.getContext());

        LayoutInflater.from(getContext()).inflate(R.layout.item_trend, this);

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

        final View divider = findViewById(R.id.item_trend_divider);
        this.dividerDrawable = ShimmerDividerDrawable.createTrendCardDivider(resources);
        divider.setBackground(dividerDrawable);

        this.title = (TextView) findViewById(R.id.item_trend_title);
        this.graphBinder = trendGraphView;
        this.annotationsLayout = new LinearLayout(getContext());

        final LayoutParams annotationsLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                      LayoutParams.WRAP_CONTENT);
        annotationsLayoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.gap_card_content);

        final LayoutParams graphLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                LayoutParams.WRAP_CONTENT);

        addView(trendGraphView, graphLayoutParams);
        addView(annotationsLayout, annotationsLayoutParams);
        Graph graph = trendGraphView.getGraph();
        setTitle(graph.getTitle());
        populateAnnotations(graph.getDataType(), graph.getAnnotations());

    }


    private TrendFeedViewItem(@NonNull Context context,
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

        final View divider = findViewById(R.id.item_trend_divider);
        this.dividerDrawable = ShimmerDividerDrawable.createTrendCardDivider(resources);
        divider.setBackground(dividerDrawable);

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

    public void setLoading(boolean loading) {
        if (loading) {
            dividerDrawable.start();
        } else {
            dividerDrawable.stop();
        }
    }

    public void setTitle(String titleText) {
        title.setText(titleText);
    }

    public void bindGraph(@NonNull Graph graph) {
        graphBinder.bindGraph(graph);
        populateAnnotations(graph.getDataType(),
                            graph.getAnnotations());
    }

    private void populateAnnotations(@NonNull Graph.DataType dataType,
                                     @Nullable List<Annotation> annotations) {
        if (!Lists.isEmpty(annotations)) {
            annotationsLayout.removeAllViews();

            final LayoutInflater inflater = LayoutInflater.from(getContext());
            for (Annotation annotation : annotations) {
                inflater.inflate(R.layout.item_bargraph_annotation, annotationsLayout);
                final View annotationView = annotationsLayout.getChildAt(annotationsLayout.getChildCount() - 1);

                final TextView titleText =
                        (TextView) annotationView.findViewById(R.id.item_bargraph_annotation_title);
                titleText.setText(annotation.getTitle());

                final @ColorRes int textColor;
                if (dataType.wantsConditionTinting()) {
                    final Condition condition = annotation.getCondition();
                    if (condition == null) {
                        textColor = Condition.UNKNOWN.colorRes;
                    } else {
                        textColor = condition.colorRes;
                    }
                } else {
                    textColor = R.color.trends_bargraph_annotation_text;
                }
                final TextView valueText =
                        ((TextView) annotationView.findViewById(R.id.item_bargraph_annotation_value));
                valueText.setTextColor(ContextCompat.getColor(getContext(), textColor));
                valueText.setText(dataType.renderAnnotation(annotation));
            }

            annotationsLayout.setVisibility(VISIBLE);
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
            title.setText(getResources().getString(R.string.title_trends_welcome));
            image.setImageResource(R.drawable.trends_first_day);

            ((MarginLayoutParams) message.getLayoutParams()).topMargin = getResources().getDimensionPixelSize(R.dimen.gap_small);
            ;
            message.setText(getResources().getString(R.string.message_trends_welcome));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //noinspection deprecation
                message.setTextAppearance(getContext(), R.style.AppTheme_Text_Body_Small_New);
            } else {
                message.setTextAppearance(R.style.AppTheme_Text_Body_Small_New);
            }
            action.setVisibility(GONE);
        }
    }

    static class WelcomeBackCardView extends WelcomeCardView {
        WelcomeBackCardView(@NonNull Context context) {
            super(context);
            title.setText(getResources().getString(R.string.title_trends_welcome_back));
            message.setText(getResources().getString(R.string.message_trends_welcome_back));
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
