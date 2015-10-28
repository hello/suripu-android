package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.util.Views;

public class SenseListDialog<T> extends Dialog
        implements ArrayRecyclerAdapter.OnItemClickedListener<T> {
    private Listener<T> listener;

    private Button positiveButton;
    private Button negativeButton;
    private TextView messageText;
    private View messageDivider;
    private RecyclerView recyclerView;
    private ProgressBar activityIndicator;

    public SenseListDialog(Context context) {
        super(context, R.style.AppTheme_Dialog_List);
        initialize();
    }

    protected void initialize() {
        setContentView(R.layout.dialog_sense_list);

        final Resources resources = getContext().getResources();

        this.recyclerView = (RecyclerView) findViewById(R.id.dialog_sense_list_recycler);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        this.positiveButton = (Button) findViewById(R.id.dialog_sense_list_positive);
        Views.setSafeOnClickListener(positiveButton, this::onDone);

        this.negativeButton = (Button) findViewById(R.id.dialog_sense_list_negative);
        Views.setSafeOnClickListener(negativeButton, ignored -> cancel());

        this.messageText = (TextView) findViewById(R.id.dialog_sense_list_message);
        this.messageDivider = findViewById(R.id.dialog_sense_list_message_divider);
        this.activityIndicator = (ProgressBar) findViewById(R.id.dialog_sense_list_loading);

        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        setPositiveButtonEnabled(savedInstanceState.getBoolean("positiveButtonEnabled"));
        setActivityIndicatorVisible(savedInstanceState.getBoolean("activityIndicatorVisible"));

        Bundle parentSavedState = savedInstanceState.getParcelable("savedInstanceState");
        if (parentSavedState != null) {
            super.onRestoreInstanceState(parentSavedState);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle savedState = new Bundle();
        savedState.putParcelable("savedInstanceState", super.onSaveInstanceState());
        savedState.putBoolean("positiveButtonEnabled", positiveButton.isEnabled());
        savedState.putBoolean("activityIndicatorVisible", activityIndicator.getVisibility() == View.VISIBLE);
        return savedState;
    }


    public void setListener(@Nullable Listener<T> listener) {
        this.listener = listener;
    }

    public void setMessage(@Nullable CharSequence message) {
        if (TextUtils.isEmpty(message)) {
            messageText.setVisibility(View.GONE);
            messageDivider.setVisibility(View.GONE);
        } else {
            messageText.setVisibility(View.VISIBLE);
            messageDivider.setVisibility(View.VISIBLE);
        }
        messageText.setText(message);
    }

    public void setMessage(@StringRes int messageRes) {
        if (messageRes == 0) {
            messageText.setVisibility(View.GONE);
            messageDivider.setVisibility(View.GONE);
        } else {
            messageText.setVisibility(View.VISIBLE);
            messageDivider.setVisibility(View.VISIBLE);
        }
        messageText.setText(messageRes);
    }

    public void setAdapter(@NonNull ArrayRecyclerAdapter<T, ?> adapter) {
        adapter.setOnItemClickedListener(this);
        recyclerView.setAdapter(adapter);
    }

    public void setPositiveButtonEnabled(boolean enabled) {
        positiveButton.setEnabled(enabled);
    }

    @Override
    public void setCancelable(boolean flag) {
        super.setCancelable(flag);

        if (flag) {
            negativeButton.setVisibility(View.VISIBLE);
        } else {
            negativeButton.setVisibility(View.GONE);
        }
    }

    public void setActivityIndicatorVisible(boolean visible) {
        if (visible) {
            activityIndicator.setVisibility(View.VISIBLE);
        } else {
            activityIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * @see android.content.DialogInterface#BUTTON_POSITIVE
     * @see android.content.DialogInterface#BUTTON_NEGATIVE
     */
    public Button getButton(int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                return positiveButton;

            case DialogInterface.BUTTON_NEGATIVE:
                return negativeButton;

            default:
                return null;
        }
    }



    public void onDone(@NonNull View sender) {
        if (listener != null) {
            listener.onDoneClicked(this);
        }

        dismiss();
    }

    @Override
    public void onItemClicked(int position, T item) {
        if (listener != null) {
            //noinspection unchecked
            listener.onItemClicked(this, position, item);
        }
    }


    public interface Listener<T> {
        void onItemClicked(@NonNull SenseListDialog<T> dialog, int position, @NonNull T item);
        void onDoneClicked(@NonNull SenseListDialog<T> dialog);
    }
}
