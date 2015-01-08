package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import is.hello.sense.R;

public class SenseSelectorDialog<T> extends Dialog {
    private ListAdapter adapter;
    private OnItemSelectedListener<T> onItemSelectedListener;

    private ListView listView;
    private Button cancelButton;

    public SenseSelectorDialog(Context context) {
        super(context, R.style.AppTheme_Dialog_Simple);
        initialize();
    }

    protected void initialize() {
        setContentView(R.layout.dialog_sense_selector);

        this.listView = (ListView) findViewById(android.R.id.list);
    }


    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener<T> onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public void setAdapter(@NonNull ListAdapter adapter) {
        this.adapter = adapter;
    }

    public interface OnItemSelectedListener<T> {
        void onItemSelected(int position, @NonNull T item);
    }
}
