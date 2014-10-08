package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.util.DateFormatter;

public final class TimelineSegmentDetailsDialogFragment extends InjectionDialogFragment {
    public static final String TAG = TimelineSegmentDetailsDialogFragment.class.getSimpleName();

    private static final String ARG_SEGMENT = TimelineSegmentDetailsDialogFragment.class.getSimpleName() + ".ARG_SEGMENT";

    @Inject DateFormatter dateFormatter;

    private TimelineSegment timelineSegment;

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

        View background = dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_overlay);
        background.setOnClickListener(unused -> dismiss());

        TextView eventType = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_event);
        eventType.setText(timelineSegment.getEventType().nameString);

        TextView message = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_message);
        message.setText(timelineSegment.getMessage());

        TextView time = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_segment_details_time);
        time.setText(dateFormatter.formatAsTime(timelineSegment.getTimestamp()));

        return dialog;
    }
}
