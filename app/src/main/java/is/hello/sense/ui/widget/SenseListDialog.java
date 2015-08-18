package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class SenseListDialog<T> extends Dialog implements AdapterView.OnItemClickListener {
    private Listener<T> listener;

    private Button doneButton;
    private Button cancelButton;
    private TextView messageText;
    private View messageDivider;
    private ListView listView;
    private ProgressBar activityIndicator;

    public SenseListDialog(Context context) {
        super(context, R.style.AppTheme_Dialog_List);
        initialize();
    }

    protected void initialize() {
        setContentView(R.layout.dialog_sense_list);

        this.listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.doneButton = (Button) findViewById(R.id.dialog_sense_list_done);
        Views.setSafeOnClickListener(doneButton, this::onDone);

        this.cancelButton = (Button) findViewById(R.id.dialog_sense_list_cancel);
        Views.setSafeOnClickListener(cancelButton, ignored -> cancel());

        this.messageText = (TextView) findViewById(R.id.dialog_sense_list_message);
        this.messageDivider = findViewById(R.id.dialog_sense_list_message_divider);
        this.activityIndicator = (ProgressBar) findViewById(R.id.dialog_sense_list_loading);

        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        setDoneButtonEnabled(savedInstanceState.getBoolean("doneEnabled"));
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
        savedState.putBoolean("doneEnabled", doneButton.isEnabled());
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

    public void setAdapter(@NonNull ListAdapter adapter) {
        listView.setAdapter(adapter);
    }

    public void setDoneButtonEnabled(boolean enabled) {
        doneButton.setEnabled(enabled);
    }

    @Override
    public void setCancelable(boolean flag) {
        super.setCancelable(flag);

        if (flag) {
            cancelButton.setVisibility(View.VISIBLE);
        } else {
            cancelButton.setVisibility(View.GONE);
        }
    }

    public void setActivityIndicatorVisible(boolean visible) {
        if (visible) {
            activityIndicator.setVisibility(View.VISIBLE);
        } else {
            activityIndicator.setVisibility(View.GONE);
        }
    }


    public void onDone(@NonNull View sender) {
        if (listener != null) {
            listener.onDoneClicked(this);
        }

        dismiss();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (listener != null) {
            //noinspection unchecked
            listener.onItemClicked(this, position, (T) adapterView.getItemAtPosition(position));
        }
    }


    public interface Listener<T> {
        void onItemClicked(@NonNull SenseListDialog<T> dialog, int position, @NonNull T item);
        void onDoneClicked(@NonNull SenseListDialog<T> dialog);
    }
}
