package is.hello.sense.ui.fragments.support;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.zendesk.sdk.attachment.ImageUploadHelper;
import com.zendesk.sdk.model.CommentResponse;
import com.zendesk.sdk.model.network.CommentsResponse;
import com.zendesk.sdk.model.network.UploadResponse;
import com.zendesk.service.ErrorResponse;

import java.io.File;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.zendesk.TicketDetailPresenter;

public class TicketDetailFragment extends InjectionFragment implements ImageUploadHelper.ImageUploadProgressListener, TextWatcher {
    private static final String ARG_TICKET_ID = TicketDetailFragment.class.getName() + ".ARG_TICKET_ID";
    private static final String ARG_TICKET_SUBJECT = TicketDetailFragment.class.getName() + ".ARG_TICKET_SUBJECT";

    @Inject TicketDetailPresenter presenter;

    private ImageUploadHelper imageUploadHelper;

    private CommentAdapter adapter;
    private ImageButton attach;
    private EditText commentText;
    private ImageButton sendComment;

    //region Lifecycle

    public static TicketDetailFragment newInstance(@NonNull String ticketId, @NonNull String subject) {
        TicketDetailFragment fragment = new TicketDetailFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_TICKET_ID, ticketId);
        arguments.putString(ARG_TICKET_SUBJECT, subject);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.imageUploadHelper = new ImageUploadHelper(this);

        String ticketId = getArguments().getString(ARG_TICKET_ID);
        presenter.setTicketId(ticketId);
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket_detail, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        this.adapter = new CommentAdapter(getActivity());
        listView.setAdapter(adapter);

        this.attach = (ImageButton) view.findViewById(R.id.fragment_ticket_detail_comment_attach);
        this.sendComment = (ImageButton) view.findViewById(R.id.fragment_ticket_detail_comment_send);
        Views.setSafeOnClickListener(sendComment, this::submit);

        this.commentText = (EditText) view.findViewById(R.id.fragment_ticket_detail_comment_text);
        commentText.addTextChangedListener(this);

        updateButtons();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.comments,
                this::bindComments,
                this::presentError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(commentText.getWindowToken(), 0);

        commentText.removeTextChangedListener(this);

        this.adapter = null;
        this.attach = null;
        this.commentText = null;
        this.sendComment = null;
    }

    //endregion


    //region Commenting

    private void updateButtons() {
        boolean hasText = !TextUtils.isEmpty(commentText.getText());
        boolean uploadsInactive = imageUploadHelper.isImageUploadCompleted();
        attach.setEnabled(uploadsInactive);
        attach.getDrawable().setAlpha(uploadsInactive ? 0xFF : 0x77);

        sendComment.setEnabled(hasText && uploadsInactive);
        sendComment.getDrawable().setAlpha(hasText && uploadsInactive ? 0xFF : 0x77);
    }

    public void submit(@NonNull View sender) {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(presenter.submitComment(commentText.getText().toString(), Collections.emptyList()),
                ignored -> {
                    commentText.setText(null);
                    presenter.update();
                },
                this::presentError);
    }

    @Override
    public void allImagesUploaded(Map<File, UploadResponse> map) {
        updateButtons();
    }

    @Override
    public void imageUploaded(UploadResponse uploadResponse, File file) {

    }

    @Override
    public void imageUploadError(ErrorResponse errorResponse, File file) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        updateButtons();
    }

    //endregion


    //region Binding

    public void bindComments(@NonNull CommentsResponse comments) {
        LoadingDialogFragment.close(getFragmentManager());

        adapter.clear();
        adapter.addAll(comments.getComments());
    }

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }

    //endregion


    static class CommentAdapter extends ArrayAdapter<CommentResponse> {
        private final LayoutInflater inflater;

        public CommentAdapter(@NonNull Context context) {
            super(context, R.layout.item_support_ticket);

            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_support_ticket, parent, false);
                view.setTag(new ViewHolder(view));
            }

            ViewHolder holder = (ViewHolder) view.getTag();
            CommentResponse comment = getItem(position);
            holder.description.setText(comment.getBody());
            holder.date.setText(DateFormat.getDateTimeInstance().format(comment.getCreatedAt()));

            return view;
        }

        static class ViewHolder {
            final TextView description;
            final TextView date;

            ViewHolder(@NonNull View view) {
                this.description = (TextView) view.findViewById(R.id.item_support_ticket_description);
                this.date = (TextView) view.findViewById(R.id.item_support_ticket_date);
            }
        }
    }
}
