package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ImageButton;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.InjectionDialogFragment;
import is.hello.sense.ui.widget.Views;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Markdown;

public final class TimelineEventDialogFragment extends InjectionDialogFragment {
    public static final String TAG = TimelineEventDialogFragment.class.getSimpleName();

    private static final String ARG_SEGMENT = TimelineEventDialogFragment.class.getSimpleName() + ".ARG_SEGMENT";

    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;
    @Inject Markdown markdown;
    private TimelineSegment timelineSegment;

    public static TimelineEventDialogFragment newInstance(@NonNull TimelineSegment segment) {
        TimelineEventDialogFragment dialogFragment = new TimelineEventDialogFragment();

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
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Simple);

        dialog.setContentView(R.layout.dialog_fragment_timeline_event);

        ImageButton closeButton = (ImageButton) dialog.findViewById(R.id.dialog_fragment_timeline_event_close);
        Views.setSafeOnClickListener(closeButton, ignored -> dismiss());

        TextView title = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_event_title);
        String eventName = getString(timelineSegment.getEventType().nameString);
        boolean use24Time = preferences.getBoolean(PreferencesPresenter.USE_24_TIME, false);
        String formattedTime = dateFormatter.formatAsTime(timelineSegment.getTimestamp(), use24Time);
        title.setText(getString(R.string.title_timeline_event_fmt, eventName, formattedTime));

        TextView message = (TextView) dialog.findViewById(R.id.dialog_fragment_timeline_event_message);
        markdown.render(timelineSegment.getMessage())
                .subscribe(message::setText, e -> message.setText(R.string.missing_data_placeholder));

        return dialog;
    }
}
