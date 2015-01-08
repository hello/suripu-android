package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import is.hello.sense.R;

public class SenseSelectorDialog<T> extends Dialog implements AdapterView.OnItemClickListener {
    private OnSelectionListener<T> onSelectionListener;

    private Button doneButton;
    private ListView listView;
    private TintedProgressBar activityIndicator;

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

        this.activityIndicator = (TintedProgressBar) findViewById(R.id.dialog_sense_selector_loading);

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
