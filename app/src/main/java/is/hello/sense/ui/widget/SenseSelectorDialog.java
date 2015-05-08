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

public class SenseSelectorDialog<T> extends Dialog implements AdapterView.OnItemClickListener {
    private OnSelectionListener<T> onSelectionListener;

    private Button doneButton;
    private TextView messageText;
    private View messageDivider;
    private ListView listView;
    private ProgressBar activityIndicator;

    public SenseSelectorDialog(Context context) {
        super(context, R.style.AppTheme_Dialog_Selector);
        initialize();
    }

    protected void initialize() {
        setContentView(R.layout.dialog_sense_selector);

        this.listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.doneButton = (Button) findViewById(R.id.dialog_sense_selector_done);
        doneButton.setOnClickListener(this::onDone);

        this.messageText = (TextView) findViewById(R.id.dialog_sense_selector_message);
        this.messageDivider = findViewById(R.id.dialog_sense_selector_message_divider);
        this.activityIndicator = (ProgressBar) findViewById(R.id.dialog_sense_selector_loading);

        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        setDoneButtonEnabled(savedInstanceState.getBoolean("doneEnabled"));
        setActivityIndicatorVisible(savedInstanceState.getBoolean("activityIndicatorVisible"));

        super.onRestoreInstanceState(savedInstanceState.getParcelable("savedInstanceState"));
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle savedState = new Bundle();
        savedState.putParcelable("savedInstanceState", super.onSaveInstanceState());
        savedState.putBoolean("doneEnabled", doneButton.isEnabled());
        savedState.putBoolean("activityIndicatorVisible", activityIndicator.getVisibility() == View.VISIBLE);
        return savedState;
    }


    public void setOnSelectionListener(@Nullable OnSelectionListener<T> onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
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

    public void setActivityIndicatorVisible(boolean visible) {
        if (visible) {
            activityIndicator.setVisibility(View.VISIBLE);
        } else {
            activityIndicator.setVisibility(View.GONE);
        }
    }


    public void onDone(@NonNull View sender) {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectionCompleted(this);
        }

        dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (onSelectionListener != null) {
            //noinspection unchecked
            onSelectionListener.onItemSelected(this, position, (T) adapterView.getItemAtPosition(position));
        }
    }


    public interface OnSelectionListener<T> {
        void onItemSelected(@NonNull SenseSelectorDialog<T> dialog, int position, @NonNull T item);
        void onSelectionCompleted(@NonNull SenseSelectorDialog<T> dialog);
    }
}
