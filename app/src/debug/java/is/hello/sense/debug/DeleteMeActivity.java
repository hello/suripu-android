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

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.activity_delete_me_recycler);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        final CustomAdapter adapter = new CustomAdapter(getLayoutInflater(),
                                                        9,
                                                        34,
                                                        "%");
        recyclerView.setAdapter(adapter);
        //layoutManager.scrollToPosition(Integer.MAX_VALUE / 2);
        recyclerView.addItemDecoration(adapter.getDecorationWithInset());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(final RecyclerView recyclerView,
                                   final int dx,
                                   final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                final int firstPosition = layoutManager.findFirstVisibleItemPosition();
                final int lastPosition = layoutManager.findLastVisibleItemPosition();
                final int center = firstPosition + (lastPosition - firstPosition) / 2;
            //    adapter.setCenterItemPosition(center);
            }
        });
    }
}
