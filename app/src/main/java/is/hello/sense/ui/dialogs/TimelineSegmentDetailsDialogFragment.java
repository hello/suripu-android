package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.TimestampTextView;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static rx.android.observables.AndroidObservable.bindFragment;

public final class TimelineSegmentDetailsDialogFragment extends InjectionDialogFragment {
    public static final String TAG = TimelineSegmentDetailsDialogFragment.class.getSimpleName();

    private static final String ARG_SEGMENT = TimelineSegmentDetailsDialogFragment.class.getSimpleName() + ".ARG_SEGMENT";

    @Inject Markdown markdown;

    private TimelineSegment timelineSegment;
    private View contentView;

    public static TimelineSegmentDetailsDialogFragment newInstance(@NonNull TimelineSegment segment) {
        TimelineSegmentDetailsDialogFragment dialogFragment = new TimelineSegmentDetailsDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SEGMENT, segment);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.timelineSegment = (TimelineSegment) getArguments().getSerializable(ARG_SEGMENT);

        setCancelable(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Details);

        dialog.setContentView(R.layout.dialog_fragment_timeline_segment_details);
        dialog.setCanceledOnTouchOutside(true);

        this.contentView = dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_content);

        View background = dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_overlay);
        background.setOnClickListener(unused -> dismiss());

        ImageView icon = (ImageView) dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_icon);
        icon.setImageResource(timelineSegment.getEventType().iconRes);

        TextView eventType = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_event);
        eventType.setText(timelineSegment.getEventType().nameString);

        TextView message = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_message);
        message.setText(R.string.missing_data_placeholder);
        Observable<CharSequence> renderedMessage = bindFragment(this, markdown.render(timelineSegment.getMessage()));
        renderedMessage.subscribe(message::setText, error -> Logger.error(TimelineSegmentDetailsDialogFragment.class.getSimpleName(), "Could not render message markdown", error));

        TimestampTextView time = (TimestampTextView) dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_time);
        time.setDateTime(timelineSegment.getTimestamp());

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        contentView.setScaleX(0.5f);
        contentView.setScaleY(0.5f);
        animate(contentView)
                .setDuration(Animation.DURATION_DEFAULT / 2)
                .setInterpolator(new AccelerateInterpolator())
                .scaleX(1.01f)
                .scaleY(1.01f)
                .andThen()
                .setApplyChangesToView(true)
                .setDuration(Animation.DURATION_DEFAULT / 2)
                .scaleX(1f)
                .scaleY(1f)
                .start();
    }
}
