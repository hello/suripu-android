package is.hello.sense.ui.fragments.support;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zendesk.sdk.attachment.AttachmentHelper;
import com.zendesk.sdk.attachment.ImageUploadHelper;
import com.zendesk.sdk.feedback.ui.AttachmentContainerHost;
import com.zendesk.sdk.model.Attachment;
import com.zendesk.sdk.model.CommentResponse;
import com.zendesk.sdk.model.network.CommentsResponse;
import com.zendesk.sdk.model.network.UploadResponse;
import com.zendesk.service.ErrorResponse;

import java.io.File;
import java.text.DateFormat;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.zendesk.AttachmentPicker;
import is.hello.sense.zendesk.TicketDetailPresenter;

public class TicketDetailFragment extends InjectionFragment implements ImageUploadHelper.ImageUploadProgressListener, TextWatcher, AdapterView.OnItemClickListener {
    private static final String ARG_TICKET_ID = TicketDetailFragment.class.getName() + ".ARG_TICKET_ID";
    private static final String ARG_TICKET_SUBJECT = TicketDetailFragment.class.getName() + ".ARG_TICKET_SUBJECT";

    @Inject TicketDetailPresenter presenter;

    private AttachmentPicker attachmentPicker;
    private ImageUploadHelper imageUploadHelper;

    private CommentAdapter adapter;
    private AttachmentContainerHost attachmentHost;
    private ImageButton attach;
    private EditText commentText;
    private ImageButton sendComment;
    private ProgressBar loadingIndicator;

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

        this.attachmentPicker = new AttachmentPicker(this, savedInstanceState);
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

        this.attachmentHost = (AttachmentContainerHost) view.findViewById(R.id.fragment_ticket_detail_comment_attachments);
        attachmentHost.setState(imageUploadHelper);
        // A retrolambda bug prevents a method reference from working here.
        //noinspection Convert2MethodRef
        attachmentHost.setAttachmentContainerListener(f -> imageUploadHelper.removeImage(f));

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        Styles.addCardSpacing(listView, Styles.CARD_SPACING_HEADER_AND_FOOTER | Styles.CARD_SPACING_USE_COMPACT);

        this.adapter = new CommentAdapter(getActivity());
        listView.setAdapter(adapter);

        this.attach = (ImageButton) view.findViewById(R.id.fragment_ticket_detail_comment_attach);
        Views.setSafeOnClickListener(attach, ignored -> attachmentPicker.showOptions());

        this.sendComment = (ImageButton) view.findViewById(R.id.fragment_ticket_detail_comment_send);
        Views.setSafeOnClickListener(sendComment, this::submit);

        this.commentText = (EditText) view.findViewById(R.id.fragment_ticket_detail_comment_text);
        commentText.addTextChangedListener(this);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_ticket_detail_loading);

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

        this.attachmentHost = null;
        this.adapter = null;
        this.attach = null;
        this.commentText = null;
        this.sendComment = null;
        this.loadingIndicator = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        attachmentPicker.saveInstanceState(outState);
    }

    //endregion


    //region Commenting

    private void updateButtons() {
        boolean hasText = !TextUtils.isEmpty(commentText.getText());
        boolean commentsLoaded = !adapter.isEmpty();
        boolean uploadsInactive = imageUploadHelper.isImageUploadCompleted();

        boolean attachEnabled = commentsLoaded && uploadsInactive;
        attach.setEnabled(attachEnabled);
        attach.getDrawable().setAlpha(attachEnabled ? 0xFF : 0x77);

        boolean sendEnabled = commentsLoaded && hasText && uploadsInactive;
        sendComment.setEnabled(sendEnabled);
        sendComment.getDrawable().setAlpha(sendEnabled ? 0xFF : 0x77);
    }

    public void submit(@NonNull View sender) {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(presenter.submitComment(commentText.getText().toString(), imageUploadHelper.getUploadTokens()),
                ignored -> {
                    commentText.setText(null);
                    attachmentHost.reset();
                    presenter.update();
                },
                this::presentError);
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


    //region Attachments

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent response) {
        super.onActivityResult(requestCode, resultCode, response);

        List<File> files = attachmentPicker.getFilesFromResult(requestCode, resultCode, response);
        AttachmentHelper.processAndUploadSelectedFiles(files,
                imageUploadHelper, getActivity(), attachmentHost);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void allImagesUploaded(Map<File, UploadResponse> map) {
        updateButtons();
    }

    @Override
    public void imageUploaded(UploadResponse uploadResponse, File file) {
        attachmentHost.setAttachmentUploaded(file);
    }

    @Override
    public void imageUploadError(ErrorResponse errorResponse, File file) {
        Analytics.trackError(errorResponse.getReason(), ErrorResponse.class.getCanonicalName(),
                errorResponse.getResponseBody(), "Zendesk Attachment Upload");

        AttachmentHelper.showAttachmentTryAgainDialog(getActivity(), file,
                errorResponse, imageUploadHelper, attachmentHost);
    }

    //endregion


    //region Binding

    public void bindComments(@NonNull CommentsResponse comments) {
        LoadingDialogFragment.close(getFragmentManager());
        loadingIndicator.setVisibility(View.GONE);

        adapter.clear();
        adapter.addAll(comments.getComments());

        updateButtons();
    }

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        loadingIndicator.setVisibility(View.GONE);

        ErrorDialogFragment.presentError(getFragmentManager(), e);

        updateButtons();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        CommentResponse comment = (CommentResponse) parent.getItemAtPosition(position);
        if (comment != null && !Lists.isEmpty(comment.getAttachments())) {
            SenseBottomSheet attachmentPicker = new SenseBottomSheet(getActivity());
            attachmentPicker.setTitle(R.string.action_view_attachment);

            List<Attachment> attachments = comment.getAttachments();
            for (int i = 0, attachmentsSize = attachments.size(); i < attachmentsSize; i++) {
                Attachment attachment = attachments.get(i);
                attachmentPicker.addOption(new SenseBottomSheet.Option(i)
                        .setTitle(attachment.getFileName()));
            }

            attachmentPicker.setOnOptionSelectedListener(option -> {
                int index = option.getOptionId();
                Attachment attachment = attachments.get(index);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(attachment.getMappedContentUrl()));
                startActivity(intent);

                return true;
            });

            attachmentPicker.show();
        }
    }

    //endregion


    static class CommentAdapter extends ArrayAdapter<CommentResponse> {
        private final Resources resources;
        private final LayoutInflater inflater;

        public CommentAdapter(@NonNull Context context) {
            super(context, R.layout.item_support_comment);

            this.resources = context.getResources();
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_support_comment, parent, false);
                view.setTag(new ViewHolder(view));
            }

            ViewHolder holder = (ViewHolder) view.getTag();
            CommentResponse comment = getItem(position);
            holder.body.setText(comment.getBody());

            String date = DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(comment.getCreatedAt());
            if (!Lists.isEmpty(comment.getAttachments())) {
                int attachmentCount = comment.getAttachments().size();
                String detailString = resources.getQuantityString(R.plurals.item_support_comment_detail_attachments,
                        attachmentCount, date, attachmentCount);
                holder.detail.setText(detailString);
                view.setBackgroundResource(R.drawable.background_timeline_header_card_selector);
            } else {
                holder.detail.setText(date);
                view.setBackgroundResource(R.drawable.background_timeline_header_card);
            }

            return view;
        }

        static class ViewHolder {
            final TextView body;
            final TextView detail;

            ViewHolder(@NonNull View view) {
                this.body = (TextView) view.findViewById(R.id.item_support_comment_body);
                this.detail = (TextView) view.findViewById(R.id.item_support_comment_detail);
            }
        }
    }
}
