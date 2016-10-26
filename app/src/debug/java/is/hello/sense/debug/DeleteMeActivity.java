package is.hello.sense.debug;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import is.hello.sense.R;
import is.hello.sense.adapter.CustomAdapter;
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.ui.recycler.LastItemInsetDecoration;
import is.hello.sense.ui.widget.CustomView;

/**
 * Temporary activity to test the view.
 * <p>
 * todo Delete when alarms support the view
 */
public class DeleteMeActivity extends SenseActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_me);
        ((CustomView) findViewById(R.id.activity_delete_me_recycler)).initialize(9, 32, "%");

    }
}
