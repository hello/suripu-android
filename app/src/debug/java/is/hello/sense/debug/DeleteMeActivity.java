package is.hello.sense.debug;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import is.hello.sense.R;
import is.hello.sense.adapter.CustomAdapter;
import is.hello.sense.ui.activities.SenseActivity;

/**
 * Temporary activity to test the view.
 * <p>
 * todo Delete when alarms support the view
 */
public class DeleteMeActivity extends SenseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_me);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.activity_delete_me_recycler);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        final CustomAdapter adapter = new CustomAdapter(getLayoutInflater(), 0, 100);
        recyclerView.setAdapter(adapter);
        layoutManager.scrollToPosition(Integer.MAX_VALUE / 2);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                final int firstPosition = layoutManager.findFirstVisibleItemPosition();
                final int lastPosition = layoutManager.findLastVisibleItemPosition();
                final int center = firstPosition + (lastPosition - firstPosition) / 2;
                adapter.setCenterItemPosition(center);
            }
        });
    }
}
