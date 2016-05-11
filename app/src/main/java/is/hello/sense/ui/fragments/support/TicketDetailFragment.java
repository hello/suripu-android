package is.hello.sense.ui.fragments.support;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Lists;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.permissions.Permission;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.zendesk.AttachmentPicker;
import is.hello.sense.zendesk.TicketDetailPresenter;

public class TicketDetailFragment extends InjectionFragment
        implements ImageUploadHelper.ImageUploadProgressListener, TextWatcher, Permission.PermissionDialogResources,
        ArrayRecyclerAdapter.OnItemClickedListener<CommentResponse> {
    private static final String ARG_TICKET_ID = TicketDetailFragment.class.getName() + ".ARG_TICKET_ID";
    private static final String ARG_TICKET_SUBJECT = TicketDetailFragment.class.getName() + ".ARG_TICKET_SUBJECT";

    @Inject
    TicketDetailPresenter presenter;

    private AttachmentPicker attachmentPicker;
    private ImageUploadHelper imageUploadHelper;

    private CommentAdapter adapter;
    private AttachmentContainerHost attachmentHost;
    private ImageButton attach;
    private EditText commentText;
    private ImageButton sendComment;
    private ProgressBar loadingIndicator;
    private final ExternalStoragePermission externalStoragePermission = new ExternalStoragePermission(this);

    //region Lifecycle

    public static TicketDetailFragment newInstance(@NonNull String ticketId, @NonNull String subject) {
        final TicketDetailFragment fragment = new TicketDetailFragment();

        final Bundle arguments = new Bundle();
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

        final String ticketId = getArguments().getString(ARG_TICKET_ID);
        if (ticketId != null) {
            presenter.setTicketId(ticketId);
        }
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_ticket_detail, container, false);

        final Resources resources = getResources();

        this.attachmentHost = (AttachmentContainerHost) view.findViewById(R.id.fragment_ticket_detail_comment_attachments);
        attachmentHost.setState(imageUploadHelper);
        // A retrolambda bug prevents a method reference from working here.
        //noinspection Convert2MethodRef
        attachmentHost.setAttachmentContainerListener(f -> imageUploadHelper.removeImage(f));

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_ticket_detail_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(resources));
        recyclerView.setItemAnimator(null);

        this.adapter = new CommentAdapter(getActivity());
        adapter.setOnItemClickedListener(this);
        recyclerView.setAdapter(adapter);

        final @ColorInt int tintColor = ContextCompat.getColor(getActivity(), R.color.light_accent);
        this.attach = (ImageButton) view.findViewById(R.id.fragment_ticket_detail_comment_attach);
        final Drawable attachDrawable = attach.getDrawable().mutate();
        Drawables.setTintColor(attachDrawable, tintColor);
        attach.setImageDrawable(attachDrawable);
        Views.setSafeOnClickListener(attach, ignored -> showAttachOptions());

        this.sendComment = (ImageButton) view.findViewById(R.id.fragment_ticket_detail_comment_send);
        final Drawable sendDrawable = sendComment.getDrawable().mutate();
        Drawables.setTintColor(sendDrawable, tintColor);
        sendComment.setImageDrawable(sendDrawable);
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

        final InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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
        final boolean hasText = !TextUtils.isEmpty(commentText.getText());
        final boolean commentsLoaded = !adapter.isEmpty();
        final boolean uploadsInactive = imageUploadHelper.isImageUploadCompleted();

        final boolean attachEnabled = commentsLoaded && uploadsInactive;
        attach.setEnabled(attachEnabled);
        attach.getDrawable().setAlpha(attachEnabled ? 0xFF : 0x77);

        final boolean sendEnabled = commentsLoaded && hasText && uploadsInactive;
        sendComment.setEnabled(sendEnabled);
        sendComment.getDrawable().setAlpha(sendEnabled ? 0xFF : 0x77);
    }

    public void submit(@NonNull View sender) {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(presenter.submitComment(commentText.getText().toString(),
                                                 imageUploadHelper.getUploadTokens()),
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (externalStoragePermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            showAttachOptions();
        } else {
            externalStoragePermission.showEnableInstructionsDialog(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent response) {
        super.onActivityResult(requestCode, resultCode, response);

        final List<File> files = attachmentPicker.getFilesFromResult(requestCode, resultCode, response);
        AttachmentHelper.processAndUploadSelectedFiles(files,
                                                       imageUploadHelper, getActivity(), attachmentHost);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void allImagesUploaded(Map<File, UploadResponse> map) {
        stateSafeExecutor.execute(this::updateButtons);
    }

    @Override
    public void imageUploaded(UploadResponse uploadResponse, File file) {
        stateSafeExecutor.execute(() -> attachmentHost.setAttachmentUploaded(file));
    }

    @Override
    public void imageUploadError(ErrorResponse errorResponse, File file) {
        stateSafeExecutor.execute(() -> {
            Analytics.trackError(errorResponse.getReason(), ErrorResponse.class.getCanonicalName(),
                                 errorResponse.getResponseBody(), "Zendesk Attachment Upload", false);

            AttachmentHelper.showAttachmentTryAgainDialog(getActivity(), file, errorResponse,
                                                          imageUploadHelper, attachmentHost);
        });
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

        ErrorDialogFragment.presentError(getActivity(), e);

        updateButtons();
    }

    @Override
    public void onItemClicked(int position, CommentResponse comment) {
        if (comment != null && !Lists.isEmpty(comment.getAttachments())) {
            final SenseBottomSheet attachmentPicker = new SenseBottomSheet(getActivity());
            attachmentPicker.setTitle(R.string.action_view_attachment);

            final List<Attachment> attachments = comment.getAttachments();
            for (int i = 0, attachmentsSize = attachments.size(); i < attachmentsSize; i++) {
                final Attachment attachment = attachments.get(i);
                attachmentPicker.addOption(new SenseBottomSheet.Option(i)
                                                   .setTitle(attachment.getFileName()));
            }

            attachmentPicker.setOnOptionSelectedListener(option -> {
                final int index = option.getOptionId();
                final Attachment attachment = attachments.get(index);
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(attachment.getMappedContentUrl()));
                startActivity(intent);

                return true;
            });

            attachmentPicker.show();
        }
    }

    //endregion

    private void showAttachOptions() {
        if (externalStoragePermission.isGranted()) {
            attachmentPicker.showOptions();
        } else {
            externalStoragePermission.requestPermission();
        }
    }

    @Override
    public int dialogTitle() {
        return R.string.request_permission_write_external_storage_required_title;
    }

    @Override
    public int dialogMessage() {
        return R.string.request_permission_write_external_storage_required_message_generic;
    }

    @NonNull
    @Override
    public DialogInterface.OnClickListener clickListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UserSupport.showStoragePermissionMoreInfoPage(getActivity());
            }
        };
    }


    static class CommentAdapter extends ArrayRecyclerAdapter<CommentResponse, CommentAdapter.ViewHolder> {
        private final Resources resources;
        private final LayoutInflater inflater;

        public CommentAdapter(@NonNull Context context) {
            super(new ArrayList<>());

            this.resources = context.getResources();
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = inflater.inflate(R.layout.item_support_comment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final CommentResponse comment = getItem(position);
            holder.body.setText(comment.getBody());

            final String date = DateFormat.getDateInstance(DateFormat.MEDIUM)
                                          .format(comment.getCreatedAt());
            if (!Lists.isEmpty(comment.getAttachments())) {
                final int attachmentCount = comment.getAttachments().size();
                final String detailString = resources.getQuantityString(R.plurals.item_support_comment_detail_attachments,
                                                                        attachmentCount, date, attachmentCount);
                holder.detail.setText(detailString);
                holder.itemView.setBackgroundResource(R.drawable.background_timeline_header_card_selector);
            } else {
                holder.detail.setText(date);
                holder.itemView.setBackgroundResource(R.drawable.background_timeline_header_card);
            }
        }

        class ViewHolder extends ArrayRecyclerAdapter.ViewHolder {
            final TextView body;
            final TextView detail;

            ViewHolder(@NonNull View view) {
                super(view);

                this.body = (TextView) view.findViewById(R.id.item_support_comment_body);
                this.detail = (TextView) view.findViewById(R.id.item_support_comment_detail);

                view.setOnClickListener(this);
            }
        }
    }
}
