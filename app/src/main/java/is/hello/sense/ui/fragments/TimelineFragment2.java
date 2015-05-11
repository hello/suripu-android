package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.json.JSONObject;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;

public class TimelineFragment2 extends InjectionFragment implements SlidingLayersView.OnInteractionListener {
    private static final String ARG_DATE = TimelineFragment2.class.getName() + ".ARG_DATE";
    private static final String ARG_CACHED_TIMELINE = TimelineFragment2.class.getName() + ".ARG_CACHED_TIMELINE";


    @Inject TimelinePresenter presenter;
    @Inject DateFormatter dateFormatter;

    private HomeActivity homeActivity;

    private ImageButton menuButton;
    private ImageButton shareButton;
    private TextView dateText;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private TimelineHeaderView headerView;
    private TimelineAdapter adapter;


    //region Lifecycle

    public static TimelineFragment2 newInstance(@NonNull DateTime date, @Nullable Timeline cachedTimeline) {
        TimelineFragment2 fragment = new TimelineFragment2();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date.withTimeAtStartOfDay());
        arguments.putSerializable(ARG_CACHED_TIMELINE, cachedTimeline);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.homeActivity = (HomeActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DateTime date = getDate();
        JSONObject properties = Analytics.createProperties(
            Analytics.Timeline.PROP_DATE, date.toString()
        );
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE, properties);

        presenter.setDateWithTimeline(date, getCachedTimeline());
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline2, container, false);


        this.dateText = (TextView) view.findViewById(R.id.fragment_timeline_date);
        dateText.setText(dateFormatter.formatAsTimelineDate(presenter.getDate()));
        Views.setSafeOnClickListener(dateText, this::showNavigator);

        this.menuButton = (ImageButton) view.findViewById(R.id.fragment_timeline_header_menu);
        Views.setSafeOnClickListener(menuButton, this::showUnderside);

        this.shareButton = (ImageButton) view.findViewById(R.id.fragment_timeline_header_share);
        shareButton.setVisibility(View.INVISIBLE);
        Views.setSafeOnClickListener(shareButton, this::share);


        this.recyclerView = (RecyclerView) view.findViewById(R.id.timeline_fragment_recycler);
        recyclerView.setHasFixedSize(true);

        this.layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        this.headerView = new TimelineHeaderView(getActivity());
        this.adapter = new TimelineAdapter(getActivity(), headerView);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.timeline,
                         this::bindTimeline,
                         this::timelineUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.menuButton = null;
        this.shareButton = null;
        this.dateText = null;

        this.headerView = null;
        this.recyclerView = null;
        this.layoutManager = null;
        this.adapter = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.homeActivity = null;
    }

    //endregion


    //region Actions

    public void showUnderside(@NonNull View sender) {
        homeActivity.getSlidingLayersView().toggle();
        scrollToTop();
    }

    public void share(@NonNull View sender) {

    }

    public void showNavigator(@NonNull View sender) {
        Timeline timeline = (Timeline) dateText.getTag();
        homeActivity.showTimelineNavigator(getDate(), timeline);
    }

    //endregion


    //region Hooks

    public @NonNull DateTime getDate() {
        return (DateTime) getArguments().getSerializable(ARG_DATE);
    }

    public @Nullable Timeline getCachedTimeline() {
        return (Timeline) getArguments().getSerializable(ARG_CACHED_TIMELINE);
    }

    public void setControlsAlarmShortcut(boolean flag) {

    }

    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }

    public void update() {
        presenter.update();
    }

    @Override
    public void onUserWillPullDownTopView() {
        menuButton.setImageResource(R.drawable.icon_menu_open);
        dateText.setTextColor(getResources().getColor(R.color.text_dim));
        shareButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onUserDidPushUpTopView() {
        menuButton.setImageResource(R.drawable.icon_menu_closed);
        dateText.setTextColor(getResources().getColor(R.color.text_dark));
        if (adapter.getItemCount() > 0) {
            shareButton.setVisibility(View.VISIBLE);
        }
    }

    //endregion


    //region Binding

    public void bindTimeline(@NonNull Timeline timeline) {
        adapter.bind(timeline);
        headerView.bindTimeline(timeline);
    }

    public void timelineUnavailable(Throwable e) {
        adapter.clear();
        headerView.timelineUnavailable(e);
    }

    //endregion


    static class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_SEGMENT = 1;
        private static final int VIEW_TYPE_EVENT = 2;

        private final Context context;
        private final View headerView;

        public TimelineAdapter(@NonNull Context context, @NonNull View headerView) {
            this.context = context;
            this.headerView = headerView;
        }


        //region Data

        @Override
        public int getItemCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            } else {
                return VIEW_TYPE_SEGMENT;
            }
        }

        public void bind(@NonNull Timeline timeline) {

        }

        public void clear() {

        }

        //endregion


        //region Vending Views

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_HEADER: {
                    return new ViewHolder(headerView);
                }

                case VIEW_TYPE_SEGMENT: {
                    return null;
                }

                case VIEW_TYPE_EVENT: {
                    return null;
                }

                default: {
                    throw new IllegalArgumentException();
                }
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View itemView) {
                super(itemView);
            }
        }

        //endregion
    }

    class TimelineHeaderView extends LinearLayout {
        private final View scoreContainer;
        private final SleepScoreDrawable scoreDrawable;
        private final TextView scoreLabelText;
        private final TextView scoreText;
        private final TextView messageText;

        public TimelineHeaderView(@NonNull Context context) {
            this(context, null);
        }

        public TimelineHeaderView(@NonNull Context context, @Nullable AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public TimelineHeaderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            LayoutInflater inflater = LayoutInflater.from(context);
            inflater.inflate(R.layout.view_timeline_header, this, true);

            this.scoreContainer = findViewById(R.id.view_timeline_header_chart);
            this.scoreDrawable = new SleepScoreDrawable(getResources());
            scoreContainer.setBackground(scoreDrawable);

            this.scoreLabelText = (TextView) findViewById(R.id.view_timeline_header_chart_label);
            this.scoreText = (TextView) findViewById(R.id.view_timeline_header_chart_score);
            this.messageText = (TextView) findViewById(R.id.view_timeline_header_chart_message);
            Views.makeTextViewLinksClickable(messageText);
        }

        public void bindTimeline(@NonNull Timeline timeline) {
            int sleepScore = timeline.getScore();
            scoreText.setText(Integer.toString(sleepScore));
        }

        public void timelineUnavailable(@NonNull Throwable e) {
            scoreText.setText(R.string.missing_data_placeholder);
            scoreText.setTextColor(getResources().getColor(R.color.text_dark));

            messageText.setText(getString(R.string.timeline_error_message, e.getMessage()));
        }
    }
}
