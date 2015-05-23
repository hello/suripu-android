package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Styles;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class TimelineInfoDialogFragment extends SenseDialogFragment {
    public static final String TAG = TimelineInfoDialogFragment.class.getSimpleName();

    private static final String ARG_SCORE = TimelineInfoDialogFragment.class.getName() + ".ARG_SCORE";
    private static final String ARG_ITEMS = TimelineInfoDialogFragment.class.getName() + ".ARG_ITEMS";

    private int score;
    private int scoreColor;
    private ArrayList<Item> items;

    private ImageButton closeButton;
    private RecyclerView recycler;
    private Adapter adapter;

    //region Lifecycle

    public static TimelineInfoDialogFragment newInstance(int score, @NonNull ArrayList<Item> items) {
        TimelineInfoDialogFragment detailsDialogFragment = new TimelineInfoDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_SCORE, score);
        arguments.putParcelableArrayList(ARG_ITEMS, items);
        detailsDialogFragment.setArguments(arguments);

        return detailsDialogFragment;
    }

    public static TimelineInfoDialogFragment newInstance(@NonNull Timeline timeline) {
        ArrayList<Item> items = new ArrayList<>();

        Timeline.Statistics statistics = timeline.getStatistics();

        if (statistics.getTotalSleep() != null) {
            int totalSleep = statistics.getTotalSleep();
            items.add(new Item(R.string.timeline_breakdown_label_total_sleep, Item.Type.DURATION, totalSleep));
        }

        if (statistics.getSoundSleep() != null) {
            int soundSleep = statistics.getSoundSleep();
            items.add(new Item(R.string.timeline_breakdown_label_sound_sleep, Item.Type.DURATION, soundSleep));
        }

        if (statistics.getTimeToSleep() != null) {
            int timeToSleep = statistics.getTimeToSleep();
            items.add(new Item(R.string.timeline_breakdown_label_total_sleep, Item.Type.DURATION, timeToSleep));
        }

        if (statistics.getTimesAwake() != null) {
            int timesAwake = statistics.getTimesAwake();
            items.add(new Item(R.string.timeline_breakdown_label_times_awake, Item.Type.COUNT, timesAwake));
        }

        return newInstance(timeline.getScore(), items);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.score = getArguments().getInt(ARG_SCORE);
        this.scoreColor = Styles.getSleepScoreColor(getActivity(), score);
        this.items = getArguments().getParcelableArrayList(ARG_ITEMS);

        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_TimelineDetails);
        dialog.setContentView(R.layout.fragment_dialog_timeline_info);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);

        ProgressBar scoreBar = (ProgressBar) dialog.findViewById(R.id.fragment_dialog_timeline_info_score);
        scoreBar.setProgress(score);
        setProgressTint(scoreBar, scoreColor);

        this.closeButton = (ImageButton) dialog.findViewById(R.id.fragment_dialog_timeline_info_close);
        closeButton.setOnClickListener(ignored -> dismissSafely());

        this.recycler = (RecyclerView) dialog.findViewById(R.id.fragment_dialog_timeline_info_recycler);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.adapter = new Adapter();
        recycler.setAdapter(adapter);

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.closeButton = null;
        this.recycler = null;
        this.adapter = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (closeButton.getVisibility() != View.VISIBLE) {
            animate(closeButton)
                    .setStartDelay(Animation.DURATION_SLOW)
                    .fadeIn()
                    .postStart();
        }
    }

    //endregion


    private static void setProgressTint(@NonNull ProgressBar tint, int tintColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tint.setProgressTintList(ColorStateList.valueOf(tintColor));
        } else {
            Drawable drawable = tint.getProgressDrawable();
            if (drawable instanceof LayerDrawable) {
                Drawable fillDrawable = ((LayerDrawable) drawable).findDrawableByLayerId(android.R.id.progress);
                if (fillDrawable != null) {
                    Drawables.setTintColor(fillDrawable, tintColor);
                }
            }
        }
    }


    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private final LayoutInflater inflater = LayoutInflater.from(getActivity());

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_timeline_info, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Item item = items.get(position);

            holder.titleText.setText(item.titleRes);
            holder.valueText.setText(item.getFormattedValue());
            holder.valueText.setTextColor(scoreColor);

            setProgressTint(holder.reading, scoreColor);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView valueText;
            final TextView averageText;
            final TextView titleText;
            final ProgressBar reading;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                this.valueText = (TextView) itemView.findViewById(R.id.item_timeline_info_value);
                this.averageText = (TextView) itemView.findViewById(R.id.item_timeline_info_average);
                this.titleText = (TextView) itemView.findViewById(R.id.item_timeline_info_title);
                this.reading = (ProgressBar) itemView.findViewById(R.id.item_timeline_info_reading);
            }
        }
    }

    public static class Item implements Parcelable {
        public final @StringRes int titleRes;
        public final Type type;
        public final int value;

        public Item(@StringRes int titleRes,
                    @NonNull Type type,
                    int value) {
            this.titleRes = titleRes;
            this.type = type;
            this.value = value;
        }

        public String getFormattedValue() {
            return type.format(value);
        }

        //region Serialization

        public Item(@NonNull Parcel in) {
            this(in.readInt(), Type.values()[in.readInt()], in.readInt());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(titleRes);
            out.writeInt(type.ordinal());
            out.writeInt(value);
        }

        public static final Parcelable.Creator<Item> CREATOR = new Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel source) {
                return new Item(source);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        //endregion

        public enum Type {
            DURATION {
                @Override
                public String format(int value) {
                    if (value < 60) {
                        return value + "m";
                    } else {
                        int hours = value / 60;
                        int minutes = value % 60;

                        String reading = Integer.toString(hours);
                        if (minutes >= 30) {
                            reading += "." + minutes;
                        }

                        reading += "h";

                        return reading;
                    }
                }
            },
            COUNT {
                @Override
                public String format(int value) {
                    return Integer.toString(value);
                }
            };

            public abstract String format(int value);
        }
    }
}
